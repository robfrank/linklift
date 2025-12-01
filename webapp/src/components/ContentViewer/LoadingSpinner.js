import React from "react";
import PropTypes from "prop-types";

/**
 * Loading spinner component
 * @param {{message: string}} props
 */
export const LoadingSpinner = ({ message = "Loading content..." }) => (
  <div className="loading-spinner">
    <div className="spinner" />
    <p>{message}</p>
  </div>
);

LoadingSpinner.propTypes = {
  message: PropTypes.string
};
