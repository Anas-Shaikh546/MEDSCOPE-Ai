import { api } from "@/services/api";
import { authClient } from "@/lib/auth";
import type { InsightGenerationSummary } from "@/types/intelligence";

export const intelligenceService = {
  async generate(reportId: number): Promise<InsightGenerationSummary> {
    const token = authClient.getToken();
    return api.post<InsightGenerationSummary>(`/insights/reports/${reportId}/generate`, {}, token);
  },

  async getAll(): Promise<InsightGenerationSummary[]> {
    const token = authClient.getToken();
    return api.get<InsightGenerationSummary[]>("/insights", token);
  },

  async getForReport(reportId: number): Promise<InsightGenerationSummary> {
    const token = authClient.getToken();
    return api.get<InsightGenerationSummary>(`/insights/reports/${reportId}`, token);
  },
};
