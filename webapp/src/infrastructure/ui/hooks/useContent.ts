import { useEffect } from "react";
import { useAppStore } from "../../../application/state/store";

export const useContent = (linkId: string | null) => {
  const content = useAppStore((state) => state.content);
  const isLoading = useAppStore((state) => state.isLoadingContent);
  const error = useAppStore((state) => state.contentError);
  const getContent = useAppStore((state) => state.getContent);
  const clearContent = useAppStore((state) => state.clearContent);
  const refreshContentAction = useAppStore((state) => state.refreshContent);

  useEffect(() => {
    if (linkId) {
      getContent(linkId);
    } else {
      clearContent();
    }
  }, [linkId, getContent, clearContent]);

  return {
    data: content,
    isLoading,
    error,
    refetch: () => {
      if (linkId) getContent(linkId);
    },
    refreshContent: () => {
      if (linkId) refreshContentAction(linkId);
    }
  };
};
