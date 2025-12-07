import { create } from "zustand";
import { createLinkSlice, LinkSlice } from "./linkSlice";
import { createContentSlice, ContentSlice } from "./contentSlice";
import { createCollectionSlice, CollectionSlice } from "./collectionSlice";

interface AppState extends LinkSlice, ContentSlice, CollectionSlice {}

export const useAppStore = create<AppState>((...a) => ({
  ...createLinkSlice(...a),
  ...createContentSlice(...a),
  ...createCollectionSlice(...a)
}));
