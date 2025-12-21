import api from "../infrastructure/api/axios-instance";

export const getGraphData = async () => {
  try {
    const response = await api.get("/graph");
    return response.data;
  } catch (error) {
    throw error;
  }
};

export const getRelatedLinks = async (linkId) => {
  try {
    const response = await api.get(`/links/${linkId}/related`);
    return response.data;
  } catch (error) {
    throw error;
  }
};
