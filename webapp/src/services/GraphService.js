import api from "../infrastructure/api/axios-instance";

export const getGraphData = async () => {
  try {
    const response = await api.get("/graph");
    // Unwrap response wrapper and map edges to links for react-force-graph
    const { nodes, edges } = response.data.data;
    return {
      nodes,
      links: edges
    };
  } catch (error) {
    throw error;
  }
};

export const getRelatedLinks = async (linkId) => {
  try {
    const response = await api.get(`/links/${linkId}/related`);
    return response.data.data;
  } catch (error) {
    throw error;
  }
};
