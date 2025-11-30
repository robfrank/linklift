import axios from "axios";

const API_BASE_URL = "/api/v1";

const api = {
  createLink: async (linkData) => {
    try {
      const response = await axios.put(`${API_BASE_URL}/link`, linkData);
      return response.data;
    } catch (error) {
      console.error("Error creating link:", error);
      throw error;
    }
  },

  listLinks: async (params = {}) => {
    try {
      const { page = 0, size = 20, sortBy = "extractedAt", sortDirection = "DESC" } = params;
      const response = await axios.get(`${API_BASE_URL}/links`, {
        params: {
          page,
          size,
          sortBy,
          sortDirection
        }
      });
      // Backend returns data in { data: { content: [], totalPages: ..., ... }, message: ... }
      return response.data.data;
    } catch (error) {
      console.error("Error fetching links:", error);
      throw error;
    }
  },

  /**
   * Get content for a specific link
   * @param {string} linkId - The link ID
   * @returns {Promise<import('../types/content').ContentResponse>}
   */
  getContent: async (linkId) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/links/${linkId}/content`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching content for link ${linkId}:`, error);
      throw error;
    }
  },

  updateLink: async (id, data) => {
    try {
      const response = await axios.patch(`${API_BASE_URL}/links/${id}`, data);
      return response.data;
    } catch (error) {
      console.error(`Error updating link ${id}:`, error);
      throw error;
    }
  },

  deleteLink: async (id) => {
    try {
      await axios.delete(`${API_BASE_URL}/links/${id}`);
    } catch (error) {
      console.error(`Error deleting link ${id}:`, error);
      throw error;
    }
  },

  // Collections
  createCollection: async (collectionData) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/collections`, collectionData);
      return response.data;
    } catch (error) {
      console.error("Error creating collection:", error);
      throw error;
    }
  },

  listCollections: async () => {
    try {
      const response = await axios.get(`${API_BASE_URL}/collections`);
      return response.data;
    } catch (error) {
      console.error("Error fetching collections:", error);
      throw error;
    }
  },

  getCollection: async (id) => {
    try {
      const response = await axios.get(`${API_BASE_URL}/collections/${id}`);
      return response.data;
    } catch (error) {
      console.error(`Error fetching collection ${id}:`, error);
      throw error;
    }
  },

  deleteCollection: async (id) => {
    try {
      await axios.delete(`${API_BASE_URL}/collections/${id}`);
    } catch (error) {
      console.error(`Error deleting collection ${id}:`, error);
      throw error;
    }
  },

  addLinkToCollection: async (collectionId, linkId) => {
    try {
      await axios.post(`${API_BASE_URL}/collections/${collectionId}/links`, { linkId });
    } catch (error) {
      console.error(`Error adding link ${linkId} to collection ${collectionId}:`, error);
      throw error;
    }
  },

  removeLinkFromCollection: async (collectionId, linkId) => {
    try {
      await axios.delete(`${API_BASE_URL}/collections/${collectionId}/links/${linkId}`);
    } catch (error) {
      console.error(`Error removing link ${linkId} from collection ${collectionId}:`, error);
      throw error;
    }
  }
};

export default api;
