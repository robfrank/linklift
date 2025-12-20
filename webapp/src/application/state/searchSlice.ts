import { StateCreator } from "zustand";
import { container } from "../di-container";
import { ContentResponse } from "../../domain/models/Content";

export interface SearchSlice {
  searchResults: ContentResponse[];
  isSearching: boolean;
  searchError: string | null;
  search: (query: string, limit?: number) => Promise<void>;
  backfillEmbeddings: () => Promise<void>;
  isBackfilling: boolean;
  backfillMessage: string | null;
}

export const createSearchSlice: StateCreator<SearchSlice> = (set) => ({
  searchResults: [],
  isSearching: false,
  searchError: null,
  isBackfilling: false,
  backfillMessage: null,

  search: async (query, limit = 10) => {
    set({ isSearching: true, searchError: null });
    try {
      const useCase = container.resolveSearchContentUseCase();
      const results = await useCase.execute(query, limit);
      set({ searchResults: results, isSearching: false });
    } catch (error: any) {
      console.error("Error searching content:", error);
      set({
        isSearching: false,
        searchError: "Search failed. Please try again."
      });
    }
  },

  backfillEmbeddings: async () => {
    set({ isBackfilling: true, backfillMessage: null });
    try {
      const useCase = container.resolveBackfillEmbeddingsUseCase();
      const message = await useCase.execute();
      set({ isBackfilling: false, backfillMessage: message });
    } catch (error: any) {
      console.error("Error triggering backfill:", error);
      set({
        isBackfilling: false,
        backfillMessage: "Failed to start backfill process."
      });
    }
  }
});
