"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient } from "@/lib/auth";
import { reportService } from "@/services/reportService";
import { intelligenceService } from "@/services/intelligenceService";
import { ApiError } from "@/services/api";
import type { UserProfile } from "@/types/auth";
import type { AnalysisResponse, ReportSummary, ReportResult } from "@/types/report";
import type { InsightGenerationSummary } from "@/types/intelligence";
import IntelligencePanel from "@/components/IntelligencePanel";

const MAX_CLIENT_SIDE_SIZE_BYTES = 10 * 1024 * 1024;

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function resultStatusLabel(status: string): string {
  switch (status) {
    case "NORMAL": return "Normal";
    case "HIGH": return "High";
    case "LOW": return "Low";
    default: return "Unknown";
  }
}

function resultStatusColor(status: string): string {
  switch (status) {
    case "NORMAL": return "#10b981";
    case "HIGH": return "#ef4444";
    case "LOW": return "#f59e0b";
    default: return "#6b7280";
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
    case "NORMAL": return "#10b981";
    case "ATTENTION": return "#f59e0b";
    case "CONCERN": return "#f97316";
    case "URGENT": return "#ef4444";
    default: return "#6b7280";
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
  const [expandedIntelligenceId, setExpandedIntelligenceId] = useState<number | null>(null);
  const [resultsByReport, setResultsByReport] = useState<Record<number, ReportResult[]>>({});
  const [analysisByReport, setAnalysisByReport] = useState<Record<number, AnalysisResponse>>({});
  const [insightsByReport, setInsightsByReport] = useState<Record<number, InsightGenerationSummary>>({});
  const [generatingIntelligenceId, setGeneratingIntelligenceId] = useState<number | null>(null);
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
    e.target.value = "";
    if (!file) return;

    setError(null);

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

  async function handleGenerateIntelligence(reportId: number) {
    setError(null);
    setGeneratingIntelligenceId(reportId);
    try {
      const insights = await intelligenceService.generate(reportId);
      setInsightsByReport((prev) => ({ ...prev, [reportId]: insights }));
      setExpandedIntelligenceId(reportId);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Intelligence generation failed");
    } finally {
      setGeneratingIntelligenceId(null);
    }
  }

  async function handleToggleIntelligence(reportId: number) {
    setError(null);
    if (expandedIntelligenceId === reportId) {
      setExpandedIntelligenceId(null);
      return;
    }

    if (!insightsByReport[reportId]) {
      try {
        const insights = await intelligenceService.getForReport(reportId);
        setInsightsByReport((prev) => ({ ...prev, [reportId]: insights }));
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          setInsightsByReport((prev) => ({ ...prev, [reportId]: null }));
        } else {
          setError(err instanceof ApiError ? err.message : "Failed to load insights");
          return;
        }
      }
    }

    setExpandedIntelligenceId(reportId);
  }

  if (!user) {
    return (
      <main style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div className="glass-card" style={{ padding: "40px" }}>
          <div style={{ fontSize: "14px", color: "rgba(255,255,255,0.9)" }}>Loading...</div>
        </div>
      </main>
    );
  }

  return (
    <main style={{ minHeight: "100vh", padding: "40px 20px" }}>
      <div style={{ maxWidth: "900px", margin: "0 auto" }}>
        {/* Header */}
        <div className="glass-card fade-in" style={{ padding: "32px", marginBottom: "32px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: "20px" }}>
            <div>
              <h1 style={{ fontSize: "32px", fontWeight: "700", margin: "0 0 8px 0", textShadow: "0 2px 10px rgba(0,0,0,0.1)" }}>
                Welcome, {user.firstName}
              </h1>
              <p style={{ fontSize: "14px", color: "rgba(255,255,255,0.8)", margin: 0 }}>
                {user.email}
              </p>
            </div>
            <button onClick={handleLogout} className="btn-secondary">
              Logout
            </button>
          </div>
        </div>

        {/* Quick Actions */}
        <div style={{ display: "flex", gap: "16px", marginBottom: "32px", flexWrap: "wrap" }}>
          <button
            onClick={() => fileInputRef.current?.click()}
            disabled={uploading}
            className="btn-primary"
            style={{ flex: 1, minWidth: "200px" }}
          >
            {uploading ? "⏳ Uploading..." : "📄 Upload Report"}
          </button>
          <a
            href="/health"
            className="btn-secondary"
            style={{
              flex: 1,
              minWidth: "200px",
              textDecoration: "none",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: "12px 24px"
            }}
          >
            📈 View Health Trends
          </a>
        </div>

        <input
          ref={fileInputRef}
          type="file"
          accept="application/pdf"
          onChange={handleFileChosen}
          disabled={uploading}
          style={{ display: "none" }}
        />

        {error && (
          <div className="glass-card fade-in" style={{ padding: "16px", marginBottom: "24px", background: "rgba(239, 68, 68, 0.2)", border: "1px solid rgba(239, 68, 68, 0.4)" }}>
            <p style={{ color: "#fff", margin: 0, fontSize: "14px" }}>⚠️ {error}</p>
          </div>
        )}

        {/* Reports Section */}
        <div className="glass-card-light fade-in" style={{ padding: "32px" }}>
          <h2 style={{ fontSize: "24px", fontWeight: "700", color: "#2d3748", marginBottom: "24px" }}>
            Your Reports
          </h2>

          {loadingReports && (
            <div style={{ textAlign: "center", padding: "40px" }}>
              <div style={{ fontSize: "14px", color: "#718096" }}>Loading reports...</div>
            </div>
          )}

          {!loadingReports && reports.length === 0 && (
            <div style={{ textAlign: "center", padding: "60px 40px" }}>
              <div style={{ fontSize: "48px", marginBottom: "16px" }}>📋</div>
              <p style={{ fontSize: "15px", color: "#4a5568", fontWeight: "500" }}>No reports yet</p>
              <p style={{ fontSize: "13px", color: "#718096", marginTop: "8px" }}>Upload your first medical report to get started</p>
            </div>
          )}

          {!loadingReports && reports.length > 0 && (
            <div style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
              {reports.map((report) => (
                <div
                  key={report.id}
                  className="glass-card slide-in"
                  style={{ padding: "24px", transition: "all 0.3s ease" }}
                >
                  <div style={{ marginBottom: "16px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "8px" }}>
                      <h3 style={{ fontSize: "16px", fontWeight: "600", color: "#fff", margin: 0 }}>
                        📄 {report.originalFilename}
                      </h3>
                      <span style={{
                        background: report.status === "PROCESSED" ? "rgba(16, 185, 129, 0.2)" :
                                   report.status === "FAILED" ? "rgba(239, 68, 68, 0.2)" :
                                   "rgba(251, 191, 36, 0.2)",
                        color: "#fff",
                        padding: "4px 12px",
                        borderRadius: "12px",
                        fontSize: "12px",
                        fontWeight: "600"
                      }}>
                        {report.status}
                      </span>
                    </div>
                    <div style={{ fontSize: "13px", color: "rgba(255,255,255,0.7)" }}>
                      {new Date(report.createdAt).toLocaleString()} · {formatBytes(report.fileSize)}
                    </div>
                  </div>

                  {report.status === "UNSUPPORTED" && (
                    <div style={{ fontSize: "13px", color: "rgba(251, 191, 36, 0.9)", marginBottom: "16px", padding: "12px", background: "rgba(251, 191, 36, 0.1)", borderRadius: "8px" }}>
                      ⚠️ This file couldn't be processed automatically
                    </div>
                  )}
                  {report.status === "FAILED" && (
                    <div style={{ fontSize: "13px", color: "rgba(239, 68, 68, 0.9)", marginBottom: "16px", padding: "12px", background: "rgba(239, 68, 68, 0.1)", borderRadius: "8px" }}>
                      ❌ Processing failed. You can try again.
                    </div>
                  )}

                  <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
                    <button onClick={() => handleView(report.id)} className="btn-secondary btn-small">
                      👁️ View
                    </button>

                    {(report.status === "UPLOADED" || report.status === "FAILED") && (
                      <button
                        onClick={() => handleProcess(report.id)}
                        disabled={processingId === report.id}
                        className="btn-secondary btn-small"
                      >
                        {processingId === report.id ? "⏳ Processing..." : "⚙️ Process"}
                      </button>
                    )}

                    {report.status === "PROCESSED" && (
                      <>
                        <button onClick={() => handleToggleResults(report.id)} className="btn-secondary btn-small">
                          {expandedResultsId === report.id ? "📊 Hide Results" : "📊 View Results"}
                        </button>
                        <button
                          onClick={() => handleAnalyze(report.id)}
                          disabled={analyzingId === report.id}
                          className="btn-secondary btn-small"
                        >
                          {analyzingId === report.id ? "⏳ Analyzing..." : "🧪 Analyze"}
                        </button>
                        <button onClick={() => handleToggleAnalysis(report.id)} className="btn-secondary btn-small">
                          {expandedAnalysisId === report.id ? "📋 Hide Analysis" : "📋 View Analysis"}
                        </button>
                        <button onClick={() => handleToggleIntelligence(report.id)} className="btn-secondary btn-small">
                          {expandedIntelligenceId === report.id ? "🧠 Hide Insights" : "🧠 View Insights"}
                        </button>
                      </>
                    )}

                    <button onClick={() => handleDelete(report.id)} className="btn-secondary btn-small" style={{ marginLeft: "auto" }}>
                      🗑️ Delete
                    </button>
                  </div>

                  {/* Results Section */}
                  {expandedResultsId === report.id && resultsByReport[report.id] && (
                    <div style={{ marginTop: "24px", padding: "20px", background: "rgba(255,255,255,0.05)", borderRadius: "12px" }}>
                      {resultsByReport[report.id].length === 0 ? (
                        <p style={{ fontSize: "13px", color: "rgba(255,255,255,0.7)" }}>No test results were recognized</p>
                      ) : (
                        <div style={{ overflowX: "auto" }}>
                          <table style={{ width: "100%", fontSize: "13px", borderCollapse: "collapse" }}>
                            <thead>
                              <tr style={{ borderBottom: "1px solid rgba(255,255,255,0.1)" }}>
                                <th style={{ padding: "12px 8px 12px 0", textAlign: "left", color: "rgba(255,255,255,0.9)", fontWeight: "600" }}>Test</th>
                                <th style={{ padding: "12px 8px", textAlign: "left", color: "rgba(255,255,255,0.9)", fontWeight: "600" }}>Value</th>
                                <th style={{ padding: "12px 8px", textAlign: "left", color: "rgba(255,255,255,0.9)", fontWeight: "600" }}>Reference</th>
                                <th style={{ padding: "12px 0", textAlign: "left", color: "rgba(255,255,255,0.9)", fontWeight: "600" }}>Status</th>
                              </tr>
                            </thead>
                            <tbody>
                              {resultsByReport[report.id].map((result, idx) => (
                                <tr key={idx} style={{ borderTop: "1px solid rgba(255,255,255,0.05)" }}>
                                  <td style={{ padding: "12px 8px 12px 0", color: "rgba(255,255,255,0.9)" }}>{result.testName}</td>
                                  <td style={{ padding: "12px 8px", color: "rgba(255,255,255,0.9)" }}>
                                    {result.value !== null
                                      ? `${result.value}${result.unit ? " " + result.unit : ""}`
                                      : result.textValue}
                                  </td>
                                  <td style={{ padding: "12px 8px", color: "rgba(255,255,255,0.7)" }}>
                                    {result.referenceLow !== null && result.referenceHigh !== null
                                      ? `${result.referenceLow} - ${result.referenceHigh}`
                                      : "-"}
                                  </td>
                                  <td style={{ padding: "12px 0" }}>
                                    <span style={{
                                      color: resultStatusColor(result.status),
                                      fontWeight: "600"
                                    }}>
                                      {resultStatusLabel(result.status)}
                                    </span>
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                    </div>
                  )}

                  {/* Analysis Section */}
                  {expandedAnalysisId === report.id && analysisByReport[report.id] && (
                    <div style={{ marginTop: "24px", padding: "20px", background: "rgba(255,255,255,0.05)", borderRadius: "12px" }}>
                      <h4 style={{ fontSize: "16px", fontWeight: "600", color: "#fff", marginBottom: "16px" }}>🧪 AI Analysis</h4>

                      {analysisByReport[report.id].summary && (
                        <p style={{ fontSize: "14px", color: "rgba(255,255,255,0.9)", lineHeight: "1.7", marginBottom: "16px" }}>
                          {analysisByReport[report.id].summary}
                        </p>
                      )}

                      {analysisByReport[report.id].findings.length === 0 ? (
                        <p style={{ fontSize: "13px", color: "rgba(255,255,255,0.7)" }}>No individual findings</p>
                      ) : (
                        <div style={{ marginBottom: "16px" }}>
                          {analysisByReport[report.id].findings.map((finding) => (
                            <div key={finding.reportResultId} style={{ marginBottom: "12px", padding: "12px", background: "rgba(255,255,255,0.05)", borderRadius: "8px" }}>
                              <span style={{ color: severityColor(finding.severity), fontWeight: "600", fontSize: "13px" }}>
                                {severityLabel(finding.severity)}:
                              </span>{" "}
                              <span style={{ fontSize: "13px", color: "rgba(255,255,255,0.9)" }}>
                                {finding.interpretation}
                              </span>
                            </div>
                          ))}
                        </div>
                      )}

                      {analysisByReport[report.id].recommendations && (
                        <div style={{ marginTop: "16px", paddingTop: "16px", borderTop: "1px solid rgba(255,255,255,0.1)" }}>
                          <h5 style={{ fontSize: "14px", fontWeight: "600", color: "rgba(255,255,255,0.9)", marginBottom: "8px" }}>📋 Recommendations</h5>
                          <p style={{ fontSize: "13px", color: "rgba(255,255,255,0.8)", lineHeight: "1.6", whiteSpace: "pre-line" }}>
                            {analysisByReport[report.id].recommendations}
                          </p>
                        </div>
                      )}

                      <p style={{ fontSize: "11px", color: "rgba(255,255,255,0.5)", marginTop: "16px" }}>
                        ℹ️ AI-generated information is not a diagnosis. Discuss with a qualified clinician.
                      </p>
                    </div>
                  )}

                  {/* Intelligence Section */}
                  {expandedIntelligenceId === report.id && (
                    <IntelligencePanel
                      reportId={report.id}
                      insights={insightsByReport[report.id] || null}
                      loading={generatingIntelligenceId === report.id}
                      onGenerate={() => handleGenerateIntelligence(report.id)}
                    />
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
