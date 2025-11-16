import React from "react";
import PropTypes from "prop-types";
import { DownloadStatus } from "../../types/content";

/**
 * Component showing downloading/pending state
 * @param {{status: string}} props
 */
export const DownloadingState = ({ status }) => (
  <div className="downloading-state">
    <div className="progress-indicator" />
    <p>{status === DownloadStatus.PENDING ? "Content download pending..." : "Downloading content..."}</p>
  </div>
);

DownloadingState.propTypes = {
  status: PropTypes.oneOf(Object.values(DownloadStatus)).isRequired
};
