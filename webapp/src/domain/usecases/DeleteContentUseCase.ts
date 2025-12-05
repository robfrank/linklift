import { IContentRepository } from "../ports/IContentRepository";

export class DeleteContentUseCase {
  constructor(private contentRepository: IContentRepository) {}

  async execute(linkId: string): Promise<void> {
    return this.contentRepository.deleteContent(linkId);
  }
}
