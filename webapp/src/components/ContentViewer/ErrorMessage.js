import React from "react";
import PropTypes from "prop-types";

/**
 * Error message component with enhanced error details
 * @param {{error: Error, onRetry: Function}} props
 */
export const ErrorMessage = ({ error, onRetry }) => {
  const getErrorDetails = () => {
    if (!error) return "An unknown error occurred";

    // Check for network errors
    if (error.message === "Network Error" || error.code === "ERR_NETWORK") {
      return {
        title: "Network Error",
        message: "Unable to connect to the server. Please check your internet connection.",
        canRetry: true
      };
    }

    // Check for HTTP errors
    if (error.response) {
      const status = error.response.status;
      switch (status) {
        case 404:
          return {
            title: "Content Not Found",
            message: "The content for this link could not be found.",
            canRetry: false
          };
        case 403:
          return {
            title: "Access Denied",
            message: "You don't have permission to view this content.",
            canRetry: false
          };
        case 500:
          return {
            title: "Server Error",
            message: "The server encountered an error. Please try again later.",
            canRetry: true
          };
        default:
          return {
            title: "Error",
            message: error.response.data?.message || error.message || "An error occurred",
            canRetry: true
          };
      }
    }

    return {
      title: "Error",
      message: error.message || "An error occurred while loading content",
      canRetry: true
    };
  };

  const errorDetails = getErrorDetails();

  return (
    <div className="error-message">
      <span className="error-icon">⚠️</span>
      <div className="error-content">
        <h3 className="error-title">{errorDetails.title}</h3>
        <p className="error-text">{errorDetails.message}</p>
        {errorDetails.canRetry && onRetry && (
          <button className="retry-button" onClick={onRetry}>
            Try Again
          </button>
        )}
      </div>
    </div>
  );
};

ErrorMessage.propTypes = {
  error: PropTypes.instanceOf(Error),
  onRetry: PropTypes.func
};
