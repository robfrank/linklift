import { Content, ContentResponse } from "../models/Content";

export interface IContentRepository {
  getContent(linkId: string): Promise<ContentResponse>;
  refreshContent(linkId: string): Promise<void>;
  deleteContent(linkId: string): Promise<void>;
}
