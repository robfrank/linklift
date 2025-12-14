import { useShallow } from "zustand/react/shallow";
import { useAppStore } from "../../../application/state/store";

export const useCollections = () => {
  return useAppStore(
    useShallow((state) => ({
      collections: state.collections,
      isLoadingCollections: state.isLoadingCollections,
      listCollectionsError: state.listCollectionsError,
      fetchCollections: state.fetchCollections,

      isAddingLinkToCollection: state.isAddingLinkToCollection,
      addLinkToCollectionError: state.addLinkToCollectionError,
      addLinkToCollection: state.addLinkToCollection,
      resetAddLinkToCollectionError: state.resetAddLinkToCollectionError
    }))
  );
};
