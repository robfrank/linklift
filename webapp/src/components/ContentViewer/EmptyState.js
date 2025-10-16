import React from "react";
import PropTypes from "prop-types";

/**
 * Empty state component
 * @param {{message: string}} props
 */
export const EmptyState = ({ message }) => (
    <div className="empty-state">
        <p>{message}</p>
    </div>
);

EmptyState.propTypes = {
    message: PropTypes.string.isRequired
};
