export interface ReportSummary {
  id: number;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  status: string;
  createdAt: string;
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
