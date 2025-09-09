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
    }
};

export default api;
