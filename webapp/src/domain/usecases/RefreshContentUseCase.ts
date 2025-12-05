import { IContentRepository } from "../ports/IContentRepository";

export class RefreshContentUseCase {
  constructor(private contentRepository: IContentRepository) {}

  async execute(linkId: string): Promise<void> {
    return this.contentRepository.refreshContent(linkId);
  }
}
