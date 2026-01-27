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

  const expandNode = useCallback(async (linkId) => {
    try {
      const relatedLinks = await getRelatedLinks(linkId);

      setGraphData((prev) => {
        const newNodes = [...(prev.nodes || [])];
        const newEdges = [...(prev.links || [])];

        const existingNodeIds = new Set(newNodes.map((n) => n.id));
        const existingEdgeKeys = new Set(
          newEdges.map((e) => {
            const sourceId = typeof e.source === "object" ? e.source.id : e.source;
            const targetId = typeof e.target === "object" ? e.target.id : e.target;
            return `${sourceId}-${targetId}`;
          })
        );

        relatedLinks.forEach((link) => {
          if (!existingNodeIds.has(link.id)) {
            newNodes.push({
              id: link.id,
              label: link.title || link.url,
              url: link.url,
              val: 1 // default size
            });
            existingNodeIds.add(link.id);
          }

          const edgeKey = `${linkId}-${link.id}`;
          if (!existingEdgeKeys.has(edgeKey)) {
            newEdges.push({ source: linkId, target: link.id });
            existingEdgeKeys.add(edgeKey);
          }
        });

        return { nodes: newNodes, links: newEdges };
      });
    } catch (err) {
      console.error("Failed to expand node:", err);
      setError(err);
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
