"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient } from "@/lib/auth";
import { reportService } from "@/services/reportService";
import { ApiError } from "@/services/api";
import type { UserProfile } from "@/types/auth";
import type { ReportSummary } from "@/types/report";

const MAX_CLIENT_SIDE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB - UX only, backend is the real boundary

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export default function DashboardPage() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [user, setUser] = useState<UserProfile | null>(null);
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [loadingReports, setLoadingReports] = useState(true);
  const [uploading, setUploading] = useState(false);
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
              <div style={{ marginTop: 8, display: "flex", gap: 8 }}>
                <button onClick={() => handleView(report.id)}>View</button>
                <button onClick={() => handleDelete(report.id)}>Delete</button>
              </div>
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
