export enum DownloadStatus {
  PENDING = "PENDING",
  IN_PROGRESS = "IN_PROGRESS",
  COMPLETED = "COMPLETED",
  FAILED = "FAILED"
}

export interface Content {
  id: string; // usually same as linkId? no, content has its own ID usually, but here it says `data.data` which is `Content`.
  // Wait, let's verify content structure.
  linkId: string;
  textContent: string; // or extractedText
  htmlContent?: string;
  status: DownloadStatus;
  extractionDate: string;
  summary?: string;
  topImage?: string;
}

export interface ContentResponse {
  data: Content;
  message?: string;
}
