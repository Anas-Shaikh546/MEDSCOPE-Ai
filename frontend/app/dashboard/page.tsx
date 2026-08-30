"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient } from "@/lib/auth";
import { reportService } from "@/services/reportService";
import { ApiError } from "@/services/api";
import type { UserProfile } from "@/types/auth";
import type { AnalysisResponse, ReportSummary, ReportResult } from "@/types/report";

const MAX_CLIENT_SIDE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB - UX only, backend is the real boundary

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

// Plain status label only - no interpretation of what NORMAL/HIGH/LOW
// means for the patient (that's explicitly Step 5 territory, not this).
function resultStatusLabel(status: string): string {
  switch (status) {
    case "NORMAL": return "Normal";
    case "HIGH": return "High";
    case "LOW": return "Low";
    default: return "Unknown";
  }
}

function severityLabel(severity: string): string {
  switch (severity) {
    case "NORMAL": return "Normal";
    case "ATTENTION": return "Needs attention";
    case "CONCERN": return "Concerning";
    case "URGENT": return "Urgent";
    default: return severity;
  }
}

function severityColor(severity: string): string {
  switch (severity) {
    case "NORMAL": return "#176b3a";
    case "ATTENTION": return "#a15c00";
    case "CONCERN": return "#b23a00";
    case "URGENT": return "crimson";
    default: return "#555";
  }
}

export default function DashboardPage() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [user, setUser] = useState<UserProfile | null>(null);
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [loadingReports, setLoadingReports] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [analyzingId, setAnalyzingId] = useState<number | null>(null);
  const [expandedResultsId, setExpandedResultsId] = useState<number | null>(null);
  const [expandedAnalysisId, setExpandedAnalysisId] = useState<number | null>(null);
  const [resultsByReport, setResultsByReport] = useState<Record<number, ReportResult[]>>({});
  const [analysisByReport, setAnalysisByReport] = useState<Record<number, AnalysisResponse>>({});
  const [error, setError] = useState<string | null>(null);

  const loadReports = useCallback(async () => {
    setLoadingReports(true);
    try {
      const data = await reportService.list();
      setReports(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load reports");
    } finally {
      setLoadingReports(false);
    }
  }, []);

  useEffect(() => {
    if (!authClient.isAuthenticated()) {
      router.push("/login");
      return;
    }

    authClient
      .fetchCurrentUser()
      .then(setUser)
      .catch(() => {
        authClient.logout();
        router.push("/login");
      });

    loadReports();
  }, [router, loadReports]);

  function handleLogout() {
    authClient.logout();
    router.push("/login");
  }

  async function handleFileChosen(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file later
    if (!file) return;

    setError(null);

    // UX-only checks - the backend performs the real validation
    // (extension, MIME type, PDF signature, size) and is the actual
    // security boundary.
    if (!file.name.toLowerCase().endsWith(".pdf")) {
      setError("Only PDF files are allowed");
      return;
    }
    if (file.size > MAX_CLIENT_SIDE_SIZE_BYTES) {
      setError("File exceeds the maximum allowed size of 10 MB");
      return;
    }

    setUploading(true);
    try {
      await reportService.upload(file);
      await loadReports();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed");
    } finally {
      setUploading(false);
    }
  }

  async function handleView(reportId: number) {
    setError(null);
    try {
      await reportService.viewInNewTab(reportId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to open report");
    }
  }

  async function handleDelete(reportId: number) {
    setError(null);
    try {
      await reportService.remove(reportId);
      setReports((prev) => prev.filter((r) => r.id !== reportId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to delete report");
    }
  }

  async function handleProcess(reportId: number) {
    setError(null);
    setProcessingId(reportId);
    try {
      const updated = await reportService.process(reportId);
      setReports((prev) => prev.map((r) => (r.id === reportId ? updated : r)));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to process report");
    } finally {
      setProcessingId(null);
    }
  }

  async function handleToggleResults(reportId: number) {
    setError(null);

    if (expandedResultsId === reportId) {
      setExpandedResultsId(null);
      return;
    }

    if (!resultsByReport[reportId]) {
      try {
        const data = await reportService.getResults(reportId);
        setResultsByReport((prev) => ({ ...prev, [reportId]: data.results }));
      } catch (err) {
        setError(err instanceof ApiError ? err.message : "Failed to load results");
        return;
      }
    }

    setExpandedResultsId(reportId);
  }

  async function handleAnalyze(reportId: number) {
    setError(null);
    setAnalyzingId(reportId);
    try {
      const analysis = await reportService.analyze(reportId);
      setAnalysisByReport((prev) => ({ ...prev, [reportId]: analysis }));
      setExpandedAnalysisId(reportId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Analysis failed");
    } finally {
      setAnalyzingId(null);
    }
  }

  async function handleToggleAnalysis(reportId: number) {
    setError(null);
    if (expandedAnalysisId === reportId) {
      setExpandedAnalysisId(null);
      return;
    }

    if (!analysisByReport[reportId]) {
      try {
        const analysis = await reportService.getAnalysis(reportId);
        setAnalysisByReport((prev) => ({ ...prev, [reportId]: analysis }));
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          setError("No saved analysis yet. Select Analyze to create one.");
        } else {
          setError(err instanceof ApiError ? err.message : "Failed to load analysis");
        }
        return;
      }
    }

    setExpandedAnalysisId(reportId);
  }

  if (!user) {
    return <main style={{ maxWidth: 600, margin: "80px auto" }}>Loading...</main>;
  }

  return (
    <main style={{ maxWidth: 600, margin: "80px auto" }}>
      <h1>Welcome, {user.firstName}</h1>
      <p>Email: {user.email}</p>
      <p>Account created: {new Date(user.createdAt).toLocaleDateString()}</p>

      <hr style={{ margin: "24px 0" }} />

      <h2>Your Reports</h2>

      {loadingReports && <p>Loading reports...</p>}

      {!loadingReports && reports.length === 0 && (
        <p>No reports yet. Upload your first medical report.</p>
      )}

      {!loadingReports && reports.length > 0 && (
        <ul style={{ listStyle: "none", padding: 0 }}>
          {reports.map((report) => (
            <li
              key={report.id}
              style={{
                border: "1px solid #ddd",
                borderRadius: 6,
                padding: 12,
                marginBottom: 8,
              }}
            >
              <div style={{ fontWeight: 600 }}>{report.originalFilename}</div>
              <div style={{ fontSize: 13, color: "#666" }}>
                Uploaded: {new Date(report.createdAt).toLocaleString()} · {formatBytes(report.fileSize)} · Status: {report.status}
              </div>

              {report.status === "UNSUPPORTED" && (
                <div style={{ fontSize: 13, color: "#a15c00", marginTop: 4 }}>
                  This file couldn&apos;t be processed automatically (e.g. a scanned image with no selectable text).
                </div>
              )}
              {report.status === "FAILED" && (
                <div style={{ fontSize: 13, color: "crimson", marginTop: 4 }}>
                  Processing failed. You can try again.
                </div>
              )}

              <div style={{ marginTop: 8, display: "flex", gap: 8, flexWrap: "wrap" }}>
                <button onClick={() => handleView(report.id)}>View</button>

                {(report.status === "UPLOADED" || report.status === "FAILED") && (
                  <button onClick={() => handleProcess(report.id)} disabled={processingId === report.id}>
                    {processingId === report.id ? "Processing..." : "Process"}
                  </button>
                )}

                {report.status === "PROCESSED" && (
                  <>
                    <button onClick={() => handleToggleResults(report.id)}>
                      {expandedResultsId === report.id ? "Hide Results" : "View Results"}
                    </button>
                    <button onClick={() => handleAnalyze(report.id)} disabled={analyzingId === report.id}>
                      {analyzingId === report.id ? "Analyzing..." : "Analyze"}
                    </button>
                    <button onClick={() => handleToggleAnalysis(report.id)}>
                      {expandedAnalysisId === report.id ? "Hide Analysis" : "View Analysis"}
                    </button>
                  </>
                )}

                <button onClick={() => handleDelete(report.id)}>Delete</button>
              </div>

              {expandedResultsId === report.id && resultsByReport[report.id] && (
                <div style={{ marginTop: 12, borderTop: "1px solid #eee", paddingTop: 8 }}>
                  {resultsByReport[report.id].length === 0 ? (
                    <p style={{ fontSize: 13, color: "#666" }}>No test results were recognized in this report.</p>
                  ) : (
                    <table style={{ width: "100%", fontSize: 13, borderCollapse: "collapse" }}>
                      <thead>
                        <tr style={{ textAlign: "left", color: "#666" }}>
                          <th style={{ padding: "4px 8px 4px 0" }}>Test</th>
                          <th style={{ padding: "4px 8px" }}>Value</th>
                          <th style={{ padding: "4px 8px" }}>Reference Range</th>
                          <th style={{ padding: "4px 0" }}>Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {resultsByReport[report.id].map((result, idx) => (
                          <tr key={idx} style={{ borderTop: "1px solid #f0f0f0" }}>
                            <td style={{ padding: "6px 8px 6px 0" }}>{result.testName}</td>
                            <td style={{ padding: "6px 8px" }}>
                              {result.value !== null
                                ? `${result.value}${result.unit ? " " + result.unit : ""}`
                                : result.textValue}
                            </td>
                            <td style={{ padding: "6px 8px" }}>
                              {result.referenceLow !== null && result.referenceHigh !== null
                                ? `${result.referenceLow} - ${result.referenceHigh}`
                                : "-"}
                            </td>
                            <td style={{ padding: "6px 0" }}>{resultStatusLabel(result.status)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              )}

              {expandedAnalysisId === report.id && analysisByReport[report.id] && (
                <section style={{ marginTop: 12, borderTop: "1px solid #eee", paddingTop: 8 }}>
                  <h3 style={{ margin: "0 0 8px" }}>AI Analysis</h3>
                  {analysisByReport[report.id].summary && (
                    <p style={{ margin: "0 0 8px" }}>{analysisByReport[report.id].summary}</p>
                  )}

                  {analysisByReport[report.id].findings.length === 0 ? (
                    <p style={{ fontSize: 13, color: "#666" }}>No individual findings were returned.</p>
                  ) : (
                    <ul style={{ paddingLeft: 20, margin: "8px 0" }}>
                      {analysisByReport[report.id].findings.map((finding) => (
                        <li key={finding.reportResultId} style={{ marginBottom: 6 }}>
                          <span style={{ color: severityColor(finding.severity), fontWeight: 600 }}>
                            {severityLabel(finding.severity)}:
                          </span>{" "}
                          {finding.interpretation}
                        </li>
                      ))}
                    </ul>
                  )}

                  {analysisByReport[report.id].recommendations && (
                    <>
                      <h4 style={{ margin: "12px 0 4px" }}>Recommendations</h4>
                      <p style={{ whiteSpace: "pre-line", margin: 0 }}>
                        {analysisByReport[report.id].recommendations}
                      </p>
                    </>
                  )}

                  <p style={{ fontSize: 12, color: "#666", marginTop: 12, marginBottom: 0 }}>
                    AI-generated information is not a diagnosis. Discuss medical questions with a qualified clinician.
                  </p>
                  <p style={{ fontSize: 12, color: "#666", marginTop: 6, marginBottom: 0 }}>
                    Model: {analysisByReport[report.id].modelName ?? "Not recorded"}
                    {analysisByReport[report.id].modelVersion
                      ? ` (${analysisByReport[report.id].modelVersion})`
                      : ""}
                    {analysisByReport[report.id].promptVersion
                      ? ` · Prompt: ${analysisByReport[report.id].promptVersion}`
                      : ""}
                  </p>
                </section>
              )}
            </li>
          ))}
        </ul>
      )}

      <div style={{ marginTop: 16 }}>
        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf"
          onChange={handleFileChosen}
          disabled={uploading}
          style={{ display: "none" }}
        />
        <button onClick={() => fileInputRef.current?.click()} disabled={uploading}>
          {uploading ? "Uploading..." : "Upload Report"}
        </button>
      </div>

      {error && <p style={{ color: "crimson", marginTop: 12 }}>{error}</p>}

      <button onClick={handleLogout} style={{ marginTop: 24 }}>
        Log out
      </button>
    </main>
  );
}
