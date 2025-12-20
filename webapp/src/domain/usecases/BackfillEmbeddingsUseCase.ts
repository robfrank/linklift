import { IContentRepository } from "../ports/IContentRepository";

export class BackfillEmbeddingsUseCase {
  constructor(private contentRepository: IContentRepository) {}

  async execute(): Promise<string> {
    return this.contentRepository.backfillEmbeddings();
  }
}
