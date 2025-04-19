import axios from "axios";

const API_BASE_URL = "/api/v1";

const api = {
  createLink: async (linkData) => {
    try {
      const response = await axios.post(`${API_BASE_URL}/link`, linkData);
      return response.data;
    } catch (error) {
      console.error("Error creating link:", error);
      throw error;
    }
  }
};

export default api;
