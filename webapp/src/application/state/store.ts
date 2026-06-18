import { create } from "zustand";
import { createLinkSlice, LinkSlice } from "./linkSlice";
import { createContentSlice, ContentSlice } from "./contentSlice";
import { createCollectionSlice, CollectionSlice } from "./collectionSlice";
import { createSearchSlice, SearchSlice } from "./searchSlice";
import { createTagSlice, TagSlice } from "./tagSlice";
import { createQASlice, QASlice } from "./qaSlice";

interface AppState extends LinkSlice, ContentSlice, CollectionSlice, SearchSlice, TagSlice, QASlice {}

export const useAppStore = create<AppState>((...a) => ({
  ...createLinkSlice(...a),
  ...createContentSlice(...a),
  ...createCollectionSlice(...a),
  ...createSearchSlice(...a),
  ...createTagSlice(...a),
  ...createQASlice(...a)
}));
