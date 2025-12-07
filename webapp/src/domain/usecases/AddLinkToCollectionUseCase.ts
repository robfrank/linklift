import { ICollectionRepository } from "../ports/ICollectionRepository";

export class AddLinkToCollectionUseCase {
  constructor(private collectionRepository: ICollectionRepository) {}

  async execute(collectionId: string, linkId: string): Promise<void> {
    await this.collectionRepository.addLink(collectionId, linkId);
  }
}
