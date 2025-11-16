import React from "react";
import PropTypes from "prop-types";
import DOMPurify from "dompurify";

/**
 * Content display component with text/HTML toggle
 * @param {{content: Object, viewMode: string, onViewModeChange: Function}} props
 */
export const ContentDisplay = ({ content, viewMode, onViewModeChange }) => {
  // Sanitize HTML content for safe rendering
  const sanitizedHtml = content.htmlContent
    ? DOMPurify.sanitize(content.htmlContent, {
        ALLOWED_TAGS: [
          "html",
          "head",
          "body",
          "div",
          "span",
          "p",
          "a",
          "img",
          "h1",
          "h2",
          "h3",
          "h4",
          "h5",
          "h6",
          "ul",
          "ol",
          "li",
          "table",
          "tr",
          "td",
          "th",
          "strong",
          "em",
          "br",
          "hr"
        ],
        ALLOWED_ATTR: ["href", "src", "alt", "title", "class", "id"]
      })
    : "";

  return (
    <div className="content-display">
      <div className="view-mode-toggle">
        <button className={viewMode === "text" ? "active" : ""} onClick={() => onViewModeChange("text")} aria-pressed={viewMode === "text"}>
          Text View
        </button>
        <button className={viewMode === "html" ? "active" : ""} onClick={() => onViewModeChange("html")} aria-pressed={viewMode === "html"}>
          HTML Preview
        </button>
      </div>

      <div className="content-viewer-body">
        {viewMode === "text" ? (
          <div className="text-content">
            <pre>{content.textContent || "No text content available"}</pre>
          </div>
        ) : (
          <div className="html-content">
            <iframe srcDoc={sanitizedHtml} sandbox="allow-same-origin" title="HTML Preview" className="html-preview-iframe" />
          </div>
        )}
      </div>
    </div>
  );
};

ContentDisplay.propTypes = {
  content: PropTypes.shape({
    htmlContent: PropTypes.string,
    textContent: PropTypes.string
  }).isRequired,
  viewMode: PropTypes.oneOf(["text", "html"]).isRequired,
  onViewModeChange: PropTypes.func.isRequired
};
