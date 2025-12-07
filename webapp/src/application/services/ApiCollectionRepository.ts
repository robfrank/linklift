import api from "../../infrastructure/api/axios-instance";
import { ICollectionRepository } from "../../domain/ports/ICollectionRepository";
import { Collection, CreateCollectionDTO } from "../../domain/models/Collection";

export class ApiCollectionRepository implements ICollectionRepository {
  async getAll(): Promise<Collection[]> {
    const response = await api.get("/collections");
    // Based on CollectionList.js: response.data.data
    return response.data.data || [];
  }

  async create(collection: CreateCollectionDTO): Promise<Collection> {
    const response = await api.post("/collections", collection);
    return response.data.data;
  }

  async delete(id: string): Promise<void> {
    await api.delete(`/collections/${id}`);
  }

  async addLink(collectionId: string, linkId: string): Promise<void> {
    await api.post(`/collections/${collectionId}/links`, { linkId });
  }

  async removeLink(collectionId: string, linkId: string): Promise<void> {
    await api.delete(`/collections/${collectionId}/links/${linkId}`);
  }
}
