import { api, ApiError } from "@/services/api";
import { authClient } from "@/lib/auth";
import type { AnalysisResponse, ReportSummary, ReportResultsResponse } from "@/types/report";

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

  async analyze(reportId: number): Promise<AnalysisResponse> {
    const token = authClient.getToken();
    return api.post<AnalysisResponse>(`/interpretations/analyze/${reportId}`, {}, token);
  },

  async getAnalysis(reportId: number): Promise<AnalysisResponse> {
    const token = authClient.getToken();
    return api.get<AnalysisResponse>(`/interpretations/by-report/${reportId}`, token);
  },

  /**
   * Fetches the PDF bytes and opens them in a new tab via a temporary
   * object URL.
   *
   * Bug fix: the window MUST be opened synchronously, before the
   * `await` below - opening it after an async fetch resolves is no
   * longer treated as a direct user-gesture by most browsers, so the
   * popup blocker silently kills it (this was the "can't view a PDF"
   * bug - it worked sometimes depending on browser/timing, which made
   * it look intermittent rather than a straightforward ordering bug).
   */
  async viewInNewTab(reportId: number): Promise<void> {
    const newWindow = window.open("", "_blank", "noopener,noreferrer");
    if (!newWindow) {
      throw new ApiError(0, "Your browser blocked the popup. Please allow popups for this site and try again.");
    }

    try {
      const token = authClient.getToken();
      const blob = await api.getBlob(`/reports/${reportId}/file`, token);
      const objectUrl = URL.createObjectURL(blob);
      newWindow.location.href = objectUrl;
      setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
    } catch (err) {
      newWindow.close();
      throw err;
    }
  },
};
