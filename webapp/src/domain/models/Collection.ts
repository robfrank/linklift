export interface Collection {
  id: string;
  name: string;
  description: string;
}

export interface CreateCollectionDTO {
  name: string;
  description?: string;
}
