export interface Link {
  id: string;
  url: string;
  title: string;
  description: string;
  extractedAt: string;
}

export type CreateLinkDTO = Omit<Link, "id">;
