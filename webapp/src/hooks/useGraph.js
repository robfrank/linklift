import { useState, useCallback } from "react";
import { getGraphData, getRelatedLinks } from "../services/GraphService";

const useGraph = () => {
  const [graphData, setGraphData] = useState({ nodes: [], edges: [] });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchGraph = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await getGraphData();
      setGraphData(data);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchRelated = useCallback(async (linkId) => {
    setLoading(true);
    setError(null);
    try {
      const data = await getRelatedLinks(linkId);
      // Handle merging related links into the graph or returning them
      return data;
    } catch (err) {
      setError(err);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    graphData,
    loading,
    error,
    fetchGraph,
    fetchRelated
  };
};

export default useGraph;
