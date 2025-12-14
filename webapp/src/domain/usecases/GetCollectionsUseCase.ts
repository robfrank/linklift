import { ICollectionRepository } from "../ports/ICollectionRepository";
import { Collection } from "../models/Collection";

export class GetCollectionsUseCase {
  constructor(private collectionRepository: ICollectionRepository) {}

  async execute(): Promise<Collection[]> {
    return await this.collectionRepository.getAll();
  }
}
