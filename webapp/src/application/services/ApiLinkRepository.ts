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
    // The previous api implementation returned response.data.data
    // My axios instance is just returning the axial response.
    // Let's verify what the backend returns.
    // Based on src/services/api.js, it extracted response.data.data.
    // My axios-instance is standard axios.
    // Backend usually returns { status: "...", message: "...", data: { ... } }

    return response.data.data;
  }

  async delete(id: string): Promise<void> {
    await api.delete(`/links/${id}`);
  }
}
