import React from "react";
import PropTypes from "prop-types";

/**
 * Modal header component
 * @param {{title: string, onClose: Function}} props
 */
export const ModalHeader = ({ title, onClose }) => (
  <div className="modal-header">
    <h2 id="modal-title">{title}</h2>
    <button className="close-button" onClick={onClose} aria-label="Close modal">
      ×
    </button>
  </div>
);

ModalHeader.propTypes = {
  title: PropTypes.string.isRequired,
  onClose: PropTypes.func.isRequired
};
