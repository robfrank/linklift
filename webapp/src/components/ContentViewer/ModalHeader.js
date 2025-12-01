import React from "react";
import PropTypes from "prop-types";

/**
 * Modal header component
 * @param {{title: string, onClose: Function, onRefresh: Function, isRefreshing: boolean}} props
 */
export const ModalHeader = ({ title, onClose, onRefresh, isRefreshing }) => (
  <div className="modal-header">
    <h2 id="modal-title">{title}</h2>
    <div className="header-actions">
      {onRefresh && (
        <button
          className={`refresh-button ${isRefreshing ? "refreshing" : ""}`}
          onClick={onRefresh}
          disabled={isRefreshing}
          aria-label="Refresh content"
          title="Refresh content"
        >
          🔄
        </button>
      )}
      <button className="close-button" onClick={onClose} aria-label="Close modal">
        ×
      </button>
    </div>
  </div>
);

ModalHeader.propTypes = {
  title: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired,
  onRefresh: PropTypes.func,
  isRefreshing: PropTypes.bool
};
