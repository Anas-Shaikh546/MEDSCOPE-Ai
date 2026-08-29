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