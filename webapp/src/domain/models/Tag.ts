export interface Tag {
  id: string;
  name: string;
  userId: string;
  createdAt: string;
}

export interface CreateTagDTO {
  name: string;
}

export interface AddTagToLinkDTO {
  tagId: string;
}
