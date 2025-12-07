import { Collection, CreateCollectionDTO } from "../models/Collection";

export interface ICollectionRepository {
  getAll(): Promise<Collection[]>;
  create(collection: CreateCollectionDTO): Promise<Collection>;
  delete(id: string): Promise<void>;
  addLink(collectionId: string, linkId: string): Promise<void>;
  removeLink(collectionId: string, linkId: string): Promise<void>;
}
