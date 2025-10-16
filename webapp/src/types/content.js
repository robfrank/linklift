/**
 * @typedef {'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED'} DownloadStatus
 */

/**
 * @typedef {Object} Content
 * @property {string} id - Content ID
 * @property {string} linkId - Associated link ID
 * @property {string|null} htmlContent - Raw HTML content
 * @property {string|null} textContent - Extracted text content
 * @property {number} contentLength - Content size in bytes
 * @property {string} downloadedAt - ISO 8601 timestamp
 * @property {string|null} mimeType - Content MIME type
 * @property {DownloadStatus} status - Download status
 */

/**
 * @typedef {Object} ContentResponse
 * @property {Content} data - Content data
 * @property {string} message - Response message
 */

/**
 * @typedef {Object} ContentError
 * @property {string} error - Error type
 * @property {string} message - Error message
 * @property {number} statusCode - HTTP status code
 */

export const DownloadStatus = {
    PENDING: "PENDING",
    IN_PROGRESS: "IN_PROGRESS",
    COMPLETED: "COMPLETED",
    FAILED: "FAILED"
};
