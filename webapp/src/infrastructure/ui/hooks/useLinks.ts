import { useShallow } from "zustand/react/shallow";
import { useAppStore } from "../../../application/state/store";

export const useLinks = () => {
  return useAppStore(
    useShallow((state) => ({
      isAddingLink: state.isAddingLink,
      addLinkError: state.addLinkError,
      addLink: state.addLink,
      resetAddLinkError: state.resetAddLinkError,

      links: state.links,
      isLoadingLinks: state.isLoadingLinks,
      listLinksError: state.listLinksError,
      page: state.page,
      size: state.size,
      totalPages: state.totalPages,
      totalElements: state.totalElements,
      sortBy: state.sortBy,
      sortDirection: state.sortDirection,
      fetchLinks: state.fetchLinks,
      setPage: state.setPage,
      setPageSize: state.setPageSize,
      setSort: state.setSort,

      isDeletingLink: state.isDeletingLink,
      deleteLinkError: state.deleteLinkError,
      deleteLink: state.deleteLink,
      resetDeleteLinkError: state.resetDeleteLinkError
    }))
  );
};
