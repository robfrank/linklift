import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import { useContent } from "../../hooks/useContent";
import { DownloadStatus } from "../../types/content";
import { LoadingSpinner } from "./LoadingSpinner";
import { ErrorMessage } from "./ErrorMessage";
import { DownloadingState } from "./DownloadingState";
import { EmptyState } from "./EmptyState";
import { ContentDisplay } from "./ContentDisplay";
import { ModalHeader } from "./ModalHeader";
import { ModalFooter } from "./ModalFooter";
import "./ContentViewerModal.css";

/**
 * Content viewer modal component
 * @param {{linkId: string|null, linkTitle: string, onClose: Function}} props
 */
export const ContentViewerModal = ({ linkId, linkTitle, onClose }) => {
  const [viewMode, setViewMode] = useState("text");
  const { data, isLoading, error } = useContent(linkId);

  // Handle escape key
  useEffect(() => {
    const handleEscape = (e) => {
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

  const renderContent = () => {
    if (isLoading) {
      return <LoadingSpinner />;
    }

    if (error) {
      return <ErrorMessage error={error} />;
    }

    if (!data) {
      return <EmptyState message="No content available" />;
    }

    const content = data.data;

    switch (content.status) {
      case DownloadStatus.PENDING:
      case DownloadStatus.IN_PROGRESS:
        return <DownloadingState status={content.status} />;

      case DownloadStatus.FAILED:
        return <ErrorMessage error={new Error("Content download failed")} />;

      case DownloadStatus.COMPLETED:
        return <ContentDisplay content={content} viewMode={viewMode} onViewModeChange={setViewMode} />;

      default:
        return <EmptyState message="Unknown status" />;
    }
  };

  const handleOverlayClick = (e) => {
    // Only close if clicking directly on overlay, not on modal content
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  return (
    <div className="modal-overlay" onClick={handleOverlayClick} role="dialog" aria-modal="true" aria-labelledby="modal-title">
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <ModalHeader title={linkTitle} onClose={onClose} />
        <div className="modal-body">{renderContent()}</div>
        <ModalFooter content={data?.data} />
      </div>
    </div>
  );
};

ContentViewerModal.propTypes = {
  linkId: PropTypes.string,
  linkTitle: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired
};
