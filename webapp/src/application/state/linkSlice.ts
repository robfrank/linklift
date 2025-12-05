import { StateCreator } from "zustand";
import { container } from "../di-container";
import { CreateLinkDTO, Link } from "../../domain/models/Link";
import { PageRequest } from "../../domain/models/Page";

export interface LinkSlice {
  // Add Link State
  isAddingLink: boolean;
  addLinkError: string | null;
  addLink: (link: CreateLinkDTO) => Promise<void>;
  resetAddLinkError: () => void;

  // List Links State
  links: Link[];
  isLoadingLinks: boolean;
  listLinksError: string | null;
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  sortBy: string;
  sortDirection: "ASC" | "DESC";
  fetchLinks: (request: PageRequest) => Promise<void>;
  setPage: (page: number) => void;
  setPageSize: (size: number) => void;
  setSort: (sortBy: string, sortDirection: "ASC" | "DESC") => void;

  // Delete Link State
  isDeletingLink: boolean;
  deleteLinkError: string | null;
  deleteLink: (id: string) => Promise<void>;
  resetDeleteLinkError: () => void;
}

export const createLinkSlice: StateCreator<LinkSlice> = (set, get) => ({
  // Add Link Implementation
  isAddingLink: false,
  addLinkError: null,
  addLink: async (linkData) => {
    set({ isAddingLink: true, addLinkError: null });
    try {
      const useCase = container.resolveAddLinkUseCase();
      await useCase.execute(linkData);
      set({ isAddingLink: false });
      // Optionally refresh list if successful
      // get().fetchLinks({ page: get().page, size: get().size, sortBy: get().sortBy, sortDirection: get().sortDirection });
    } catch (error: any) {
      let message = "Failed to add link";
      if (error.response?.status === 409) {
        message = "This URL already exists";
      } else if (error.response?.data?.message) {
        message = error.response.data.message;
      } else if (error.response?.status === 400) {
        message = "Invalid link data. Please check your input.";
      } else if (error.response?.status && error.response.status >= 500) {
        message = "Server error. Please try again later.";
      } else if (error.request) {
        message = "Unable to connect to server. Please check your connection.";
      }

      set({ isAddingLink: false, addLinkError: message });
      throw error;
    }
  },
  resetAddLinkError: () => set({ addLinkError: null }),

  // List Links Implementation
  links: [],
  isLoadingLinks: false,
  listLinksError: null,
  page: 0,
  size: 20,
  totalPages: 0,
  totalElements: 0,
  sortBy: "extractedAt",
  sortDirection: "DESC",

  fetchLinks: async (request) => {
    set({ isLoadingLinks: true, listLinksError: null });
    try {
      const useCase = container.resolveGetLinksUseCase();
      const pageResult = await useCase.execute(request);
      set({
        links: pageResult.content,
        totalPages: pageResult.totalPages,
        totalElements: pageResult.totalElements,
        isLoadingLinks: false
      });
    } catch (error: any) {
      console.error("Error fetching links:", error);
      set({
        isLoadingLinks: false,
        listLinksError: "Failed to load links. Please try again."
      });
    }
  },

  setPage: (page) => set({ page }),
  setPageSize: (size) => set({ size, page: 0 }), // Reset to first page on size change
  setSort: (sortBy, sortDirection) => set({ sortBy, sortDirection, page: 0 }),

  // Delete Link Implementation
  isDeletingLink: false,
  deleteLinkError: null,
  deleteLink: async (id) => {
    set({ isDeletingLink: true, deleteLinkError: null });
    try {
      const useCase = container.resolveDeleteLinkUseCase();
      await useCase.execute(id);
      set({ isDeletingLink: false });
      // Refresh list
      get().fetchLinks({
        page: get().page,
        size: get().size,
        sortBy: get().sortBy,
        sortDirection: get().sortDirection
      });
    } catch (error: any) {
      let message = "Failed to delete link";
      if (error.response?.status && error.response.status >= 500) {
        message = "Server error. Please try again later.";
      } else if (error.request) {
        message = "Unable to connect to server. Please check your connection.";
      }
      set({ isDeletingLink: false, deleteLinkError: message });
      throw error;
    }
  },
  resetDeleteLinkError: () => set({ deleteLinkError: null })
});
