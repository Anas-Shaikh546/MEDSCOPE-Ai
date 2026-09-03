export type InsightType =
  | "TREND_CONTEXT"
  | "PERSISTENT_ABNORMALITY"
  | "SIGNIFICANT_CHANGE"
  | "MULTI_RESULT_PATTERN"
  | "FOLLOW_UP"
  | "GENERAL_CONTEXT";

export type InsightPriority = "HIGH" | "MODERATE" | "LOW" | "INFORMATIONAL";

export type InsightStatus = "GENERATED" | "VALIDATED" | "FAILED" | "DISMISSED";

export type InsightGenerationStatus = "PENDING" | "IN_PROGRESS" | "COMPLETED" | "FAILED";

export interface Insight {
  id: number;
  generationId: number;
  type: InsightType;
  title: string;
  description: string;
  priority: InsightPriority;
  confidence: number;
  followUpQuestions: string | null;
  status: InsightStatus;
  createdAt: string;
}

export interface InsightGenerationSummary {
  generationId: number;
  reportId: number;
  generationNumber: number;
  status: InsightGenerationStatus;
  insightCount: number;
  createdAt: string;
  insights: Insight[];
}
