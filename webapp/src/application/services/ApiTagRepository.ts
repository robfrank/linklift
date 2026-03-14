import api from "../../infrastructure/api/axios-instance";
import { ITagRepository } from "../../domain/ports/ITagRepository";
import { Tag, CreateTagDTO, AddTagToLinkDTO } from "../../domain/models/Tag";

export class ApiTagRepository implements ITagRepository {
  async listTags(): Promise<Tag[]> {
    const response = await api.get("/tags");
    return response.data;
  }

  async createTag(data: CreateTagDTO): Promise<Tag> {
    const response = await api.post("/tags", data);
    return response.data;
  }

  async deleteTag(tagId: string): Promise<void> {
    await api.delete(`/tags/${tagId}`);
  }

  async getTagsForLink(linkId: string): Promise<Tag[]> {
    const response = await api.get(`/links/${linkId}/tags`);
    return response.data;
  }

  async addTagToLink(linkId: string, data: AddTagToLinkDTO): Promise<void> {
    await api.post(`/links/${linkId}/tags`, data);
  }

  async removeTagFromLink(linkId: string, tagId: string): Promise<void> {
    await api.delete(`/links/${linkId}/tags/${tagId}`);
  }

  async suggestTags(linkId: string): Promise<Tag[]> {
    const response = await api.get(`/links/${linkId}/tags/suggest`);
    return response.data;
  }
}
