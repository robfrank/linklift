import React from "react";
import PropTypes from "prop-types";
import { formatBytes, formatDate } from "../../utils/formatters";

/**
 * Modal footer with content metadata
 * @param {{content: Object}} props
 */
export const ModalFooter = ({ content }) => {
  if (!content) return null;

  return (
    <div className="modal-footer">
      <div className="metadata">
        <span>Size: {formatBytes(content.contentLength)}</span>
        <span>Type: {content.mimeType || "N/A"}</span>
        <span>Downloaded: {formatDate(content.downloadedAt)}</span>
      </div>
    </div>
  );
};

ModalFooter.propTypes = {
  content: PropTypes.shape({
    contentLength: PropTypes.number,
    mimeType: PropTypes.string,
    downloadedAt: PropTypes.string
  })
};
