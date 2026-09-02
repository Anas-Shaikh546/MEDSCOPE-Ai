export interface ReportSummary {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  status: string;
  createdAt: string;
  testDate: string | null;
}

export interface ReportResult {
  testName: string;
  value: number | null;
  textValue: string | null;
  unit: string | null;
  referenceLow: number | null;
  referenceHigh: number | null;
  status: string;
}

export interface ReportResultsResponse {
  reportId: number;
  status: string;
  results: ReportResult[];
}

export interface AnalysisFinding {
  reportResultId: number;
  interpretation: string;
  severity: "NORMAL" | "ATTENTION" | "CONCERN" | "URGENT";
}

export interface AnalysisResponse {
  id: number;
  reportId: number;
  status: string;
  summary: string | null;
  recommendations: string | null;
  modelName: string | null;
  modelVersion: string | null;
  promptVersion: string | null;
  createdAt: string;
  findings: AnalysisFinding[];
}

// Step 6 — timeline types
export type TrendDirection =
  | "INCREASING"
  | "DECREASING"
  | "STABLE"
  | "FLUCTUATING"
  | "INSUFFICIENT_DATA"
  | "UNSUPPORTED";

export interface TimelineObservation {
  date: string;
  dateIsConfirmed: boolean;
  reportId: number;
  reportResultId: number;
  value: number;
  unit: string;
  referenceLow: number | null;
  referenceHigh: number | null;
  status: string;
}

export interface TestTrend {
  canonicalName: string;
  displayName: string;
  category: string;
  defaultUnit: string | null;
  trend: TrendDirection;
  observations: TimelineObservation[];
}

export interface TrendsResponse {
  trends: TestTrend[];
}