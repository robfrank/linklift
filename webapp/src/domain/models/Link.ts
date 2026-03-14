export type ReadStatus = "UNREAD" | "READING" | "READ";

export interface Link {
  id: string;
  url: string;
  title: string;
  description: string;
  extractedAt: string;
  readStatus: ReadStatus;
  archived: boolean;
  favorited: boolean;
}

export type CreateLinkDTO = Omit<Link, "id" | "readStatus" | "archived" | "favorited">;

export interface UpdateLinkStatusDTO {
  readStatus?: ReadStatus;
  archived?: boolean;
  favorited?: boolean;
}
