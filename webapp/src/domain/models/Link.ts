export interface Link {
  id: string;
  url: string;
  title: string;
  description: string;
}

export type CreateLinkDTO = Omit<Link, "id">;
