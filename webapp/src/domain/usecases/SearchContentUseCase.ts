import { IContentRepository } from "../ports/IContentRepository";
import { ContentResponse } from "../models/Content";

export class SearchContentUseCase {
  constructor(private contentRepository: IContentRepository) {}

  async execute(query: string, limit: number = 10): Promise<ContentResponse[]> {
    return this.contentRepository.search(query, limit);
  }
}
