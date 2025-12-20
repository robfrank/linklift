export enum DownloadStatus {
  PENDING = "PENDING",
  IN_PROGRESS = "IN_PROGRESS",
  COMPLETED = "COMPLETED",
  FAILED = "FAILED"
}

export interface Content {
  id: string;
  linkId: string;
  textContent: string; // or extractedText
  htmlContent?: string;
  status: DownloadStatus;
  extractionDate: string;
  summary?: string;
  topImage?: string;
  extractedTitle?: string;
  extractedDescription?: string;
  extractedAuthor?: string;
  publishedDate?: string;
}

export interface ContentResponse {
  data: Content;
  message?: string;
}
