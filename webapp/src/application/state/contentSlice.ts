import { StateCreator } from "zustand";
import { container } from "../di-container";
import { ContentResponse } from "../../domain/models/Content";

export interface ContentSlice {
  content: ContentResponse | null;
  isLoadingContent: boolean;
  contentError: string | null;
  getContent: (linkId: string) => Promise<void>;
  refreshContent: (linkId: string) => Promise<void>;
  deleteContent: (linkId: string) => Promise<void>;
  clearContent: () => void;
}

export const createContentSlice: StateCreator<ContentSlice> = (set) => ({
  content: null,
  isLoadingContent: false,
  contentError: null,

  getContent: async (linkId) => {
    set({ isLoadingContent: true, contentError: null });
    try {
      const useCase = container.resolveGetContentUseCase();
      const content = await useCase.execute(linkId);
      set({ content, isLoadingContent: false });
    } catch (error: any) {
      console.error("Error fetching content:", error);
      set({
        isLoadingContent: false,
        contentError: "Failed to load content."
      });
    }
  },

  refreshContent: async (linkId) => {
    // Note: Refreshing might set a flag but usually we want to see the spinner
    set({ isLoadingContent: true, contentError: null });
    try {
      const useCase = container.resolveRefreshContentUseCase();
      await useCase.execute(linkId);
      // After initiating refresh, usually backend updates status to IN_PROGRESS.
      // We should probably fetch content again after a short delay or if the response implies new state.
      // But RefreshContentUseCase returns void.
      // So let's re-fetch.
      const getUseCase = container.resolveGetContentUseCase();
      const content = await getUseCase.execute(linkId);
      set({ content, isLoadingContent: false });
    } catch (error: any) {
      console.error("Error refreshing content:", error);
      set({
        isLoadingContent: false,
        contentError: "Failed to refresh content."
      });
    }
  },

  deleteContent: async (linkId) => {
    set({ isLoadingContent: true, contentError: null });
    try {
      const useCase = container.resolveDeleteContentUseCase();
      await useCase.execute(linkId);
      set({ content: null, isLoadingContent: false });
    } catch (error: any) {
      console.error("Error deleting content:", error);
      set({
        isLoadingContent: false,
        contentError: "Failed to delete content."
      });
    }
  },

  clearContent: () => set({ content: null, contentError: null })
});
