import { IContentRepository } from "../ports/IContentRepository";
import { ContentResponse } from "../models/Content";

export class GetContentUseCase {
  constructor(private contentRepository: IContentRepository) {}

  async execute(linkId: string): Promise<ContentResponse> {
    return this.contentRepository.getContent(linkId);
  }
}
