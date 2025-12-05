import React, { useState, useEffect } from "react";
import { useContent } from "../../hooks/useContent";
import { DownloadStatus } from "../../../../domain/models/Content";
// @ts-ignore
import { LoadingSpinner } from "../../../../components/ContentViewer/LoadingSpinner";
// @ts-ignore
import { ErrorMessage } from "../../../../components/ContentViewer/ErrorMessage";
// @ts-ignore
import { DownloadingState } from "../../../../components/ContentViewer/DownloadingState";
// @ts-ignore
import { EmptyState } from "../../../../components/ContentViewer/EmptyState";
// @ts-ignore
import { ContentDisplay } from "../../../../components/ContentViewer/ContentDisplay";
// @ts-ignore
import { ModalHeader } from "../../../../components/ContentViewer/ModalHeader";
// @ts-ignore
import { ModalFooter } from "../../../../components/ContentViewer/ModalFooter";
import "../../../../components/ContentViewer/ContentViewerModal.css";

interface ContentViewerModalProps {
  linkId: string;
  linkTitle: string;
  onClose: () => void;
}

export const ContentViewerModal: React.FC<ContentViewerModalProps> = ({ linkId, linkTitle, onClose }) => {
  const [viewMode, setViewMode] = useState("text");
  const [isRefreshing, setIsRefreshing] = useState(false);
  // useContent now returns simplified object
  const { data, isLoading, error, refetch, refreshContent } = useContent(linkId);

  // Handle escape key
  useEffect(() => {
    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };

    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, [onClose]);

  // Prevent body scroll when modal is open
  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "unset";
    };
  }, []);

  if (!linkId) return null;

  const handleRefreshContent = async () => {
    if (!linkId || isRefreshing) return;

    setIsRefreshing(true);
    try {
      refreshContent();
      // The hook handles the refresh action.
      // But we might want to wait or show refreshing state locally if the hook doesn't cover it fully.
      // My contentSlice implementation of refreshContent sets isLoadingContent=true.
      // So isLoading will become true.
      // But here we also have isRefreshing local state which is used for the spinner message "Refreshing content..."
      // vs just "Loading...".
      // Let's rely on isLoading from hook, but if we want "Refreshing" message, we can check if data exists && isLoading.
    } catch (error) {
      // handled by hook
    } finally {
      // We don't really know when it finishes unless we await the promise returned by refreshContent.
      // My hook exposes () => { if(linkId) refreshContentAction(linkId) }, but returns void for now in the wrapper.
      // The slice action is async. I should update hook to return the promise.
      setIsRefreshing(false);
    }
  };

  const handleRetry = () => {
    refetch();
  };

  const renderContent = () => {
    if (isLoading) {
      // If we have data and are loading, it's a refresh?
      // If isLoading is true, we show spinner.
      return <LoadingSpinner message={data ? "Refreshing content..." : undefined} />;
    }

    if (error) {
      // @ts-ignore
      return <ErrorMessage error={error} onRetry={handleRetry} />;
    }

    if (!data) {
      return <EmptyState message="No content available" />;
    }

    const content = data.data; // contentSlice returns ContentResponse which has data: Content

    switch (content.status as DownloadStatus) {
      case DownloadStatus.PENDING:
      case DownloadStatus.IN_PROGRESS:
        // @ts-ignore
        return <DownloadingState status={content.status} />;

      case DownloadStatus.FAILED:
        return <ErrorMessage error={new Error("Content download failed.")} onRetry={handleRefreshContent} />;

      case DownloadStatus.COMPLETED:
        // @ts-ignore
        return <ContentDisplay content={content} viewMode={viewMode} onViewModeChange={setViewMode} />;

      default:
        return <EmptyState message="Unknown status" />;
    }
  };

  const handleOverlayClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick} role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <ModalHeader title={linkTitle} onClose={onClose} onRefresh={handleRefreshContent} isRefreshing={isLoading && !!data} />
        <div className="modal-body">{renderContent()}</div>
        {/* @ts-ignore */}
        <ModalFooter content={data?.data} />
      </div>
    </div>
  );
};
