import { StateCreator } from "zustand";
import { container } from "../di-container";
import { Collection } from "../../domain/models/Collection";

export interface CollectionSlice {
  // Collection State
  collections: Collection[];
  isLoadingCollections: boolean;
  listCollectionsError: string | null;
  fetchCollections: () => Promise<void>;

  // Add Link to Collection State
  isAddingLinkToCollection: boolean;
  addLinkToCollectionError: string | null;
  addLinkToCollection: (collectionId: string, linkId: string) => Promise<void>;
  resetAddLinkToCollectionError: () => void;
}

export const createCollectionSlice: StateCreator<CollectionSlice> = (set, get) => ({
  collections: [],
  isLoadingCollections: false,
  listCollectionsError: null,

  fetchCollections: async () => {
    set({ isLoadingCollections: true, listCollectionsError: null });
    try {
      const useCase = container.resolveGetCollectionsUseCase();
      const collections = await useCase.execute();
      set({ collections, isLoadingCollections: false });
    } catch (error: any) {
      console.error("Error fetching collections:", error);
      set({
        isLoadingCollections: false,
        listCollectionsError: "Failed to load collections. Please try again."
      });
    }
  },

  isAddingLinkToCollection: false,
  addLinkToCollectionError: null,
  addLinkToCollection: async (collectionId, linkId) => {
    set({ isAddingLinkToCollection: true, addLinkToCollectionError: null });
    try {
      const useCase = container.resolveAddLinkToCollectionUseCase();
      await useCase.execute(collectionId, linkId);
      set({ isAddingLinkToCollection: false });
    } catch (error: any) {
      let message = "Failed to add link to collection";
      if (error.response?.status === 409) {
        message = "Link already exists in this collection";
      }
      set({ isAddingLinkToCollection: false, addLinkToCollectionError: message });
      throw error;
    }
  },
  resetAddLinkToCollectionError: () => set({ addLinkToCollectionError: null })
});
