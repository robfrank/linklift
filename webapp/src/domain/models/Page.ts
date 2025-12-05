export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number; // current page number (0-based)
}

export interface PageRequest {
  page: number;
  size: number;
  sortBy?: string;
  sortDirection?: "ASC" | "DESC";
}
