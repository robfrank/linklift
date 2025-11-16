import React from "react";
import PropTypes from "prop-types";

/**
 * Error message component
 * @param {{error: Error}} props
 */
export const ErrorMessage = ({ error }) => (
  <div className="error-message">
    <span className="error-icon">⚠️</span>
    <p>{error?.message || "An error occurred while loading content"}</p>
  </div>
);

ErrorMessage.propTypes = {
  error: PropTypes.instanceOf(Error)
};
