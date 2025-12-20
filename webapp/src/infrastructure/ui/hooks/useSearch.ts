import { useCallback } from "react";
import { useAppStore } from "../../../application/state/store";
import { SearchSlice } from "../../../application/state/searchSlice";
import { LinkSlice } from "../../../application/state/linkSlice";
import { ContentSlice } from "../../../application/state/contentSlice";
import { CollectionSlice } from "../../../application/state/collectionSlice";

export interface AppState extends LinkSlice, ContentSlice, CollectionSlice, SearchSlice {}

export const useSearch = () => {
  const searchResults = useAppStore((state: AppState) => state.searchResults);
  const isSearching = useAppStore((state: AppState) => state.isSearching);
  const searchError = useAppStore((state: AppState) => state.searchError);
  const search = useAppStore((state: AppState) => state.search);

  const backfillEmbeddings = useAppStore((state: AppState) => state.backfillEmbeddings);
  const isBackfilling = useAppStore((state: AppState) => state.isBackfilling);
  const backfillMessage = useAppStore((state: AppState) => state.backfillMessage);

  const performSearch = useCallback(
    async (query: string, limit?: number) => {
      await search(query, limit);
    },
    [search]
  );

  const triggerBackfill = useCallback(async () => {
    await backfillEmbeddings();
  }, [backfillEmbeddings]);

  return {
    searchResults,
    isSearching,
    searchError,
    performSearch,
    triggerBackfill,
    isBackfilling,
    backfillMessage
  };
};
