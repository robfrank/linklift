import api from "../../infrastructure/api/axios-instance";
import { IContentRepository } from "../../domain/ports/IContentRepository";
import { ContentResponse } from "../../domain/models/Content";

export class ApiContentRepository implements IContentRepository {
  async getContent(linkId: string): Promise<ContentResponse> {
    const response = await api.get(`/links/${linkId}/content`);
    return response.data;
  }

  async refreshContent(linkId: string): Promise<void> {
    await api.post(`/links/${linkId}/content/refresh`);
  }

  async deleteContent(linkId: string): Promise<void> {
    await api.delete(`/links/${linkId}/content`);
  }
}
