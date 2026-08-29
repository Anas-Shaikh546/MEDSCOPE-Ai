import { api } from "@/services/api";
import { authClient } from "@/lib/auth";
import type { ReportSummary, ReportResultsResponse } from "@/types/report";

/**
 * Report-specific calls, built on top of api.ts - components should
 * call these, never fetch()/api.* directly.
 */
export const reportService = {
  async list(): Promise<ReportSummary[]> {
    const token = authClient.getToken();
    return api.get<ReportSummary[]>("/reports", token);
  },

  async upload(file: File): Promise<ReportSummary> {
    const token = authClient.getToken();
    const formData = new FormData();
    formData.append("file", file);
    return api.postForm<ReportSummary>("/reports", formData, token);
  },

  async remove(reportId: number): Promise<void> {
    const token = authClient.getToken();
    await api.delete<void>(`/reports/${reportId}`, token);
  },

  async process(reportId: number): Promise<ReportSummary> {
    const token = authClient.getToken();
    return api.post<ReportSummary>(`/reports/${reportId}/process`, {}, token);
  },

  async getResults(reportId: number): Promise<ReportResultsResponse> {
    const token = authClient.getToken();
    return api.get<ReportResultsResponse>(`/reports/${reportId}/results`, token);
  },

  /**
   * Fetches the PDF bytes and opens them in a new tab via a temporary
   * object URL. The URL is revoked shortly after so it doesn't leak
   * across the session.
   */
  async viewInNewTab(reportId: number): Promise<void> {
    const token = authClient.getToken();
    const blob = await api.getBlob(`/reports/${reportId}/file`, token);
    const objectUrl = URL.createObjectURL(blob);
    window.open(objectUrl, "_blank", "noopener,noreferrer");
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
  },
};