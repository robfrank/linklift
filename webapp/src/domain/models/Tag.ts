export interface Tag {
  id: string;
  name: string;
  createdAt: string;
}

export interface CreateTagDTO {
  name: string;
}

export interface AddTagToLinkDTO {
  tagId: string;
}
