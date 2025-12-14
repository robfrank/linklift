import api from "../../infrastructure/api/axios-instance";
import { ILinkRepository } from "../../domain/ports/ILinkRepository";
import { CreateLinkDTO, Link } from "../../domain/models/Link";
import { Page, PageRequest } from "../../domain/models/Page";

export class ApiLinkRepository implements ILinkRepository {
  async create(linkData: CreateLinkDTO): Promise<Link> {
    const response = await api.put("/link", linkData);
    return response.data;
  }

  async getAll(request: PageRequest): Promise<Page<Link>> {
    const { page, size, sortBy, sortDirection } = request;
    const response = await api.get("/links", {
      params: {
        page,
        size,
        sortBy,
        sortDirection
      }
    });

    return response.data.data;
  }

  async delete(id: string): Promise<void> {
    await api.delete(`/links/${id}`);
  }
}
