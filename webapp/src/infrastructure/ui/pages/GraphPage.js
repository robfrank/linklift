import React, { useEffect } from "react";
import { Container, Typography, Box } from "@mui/material";
import useGraph from "../../../hooks/useGraph";
import GraphView from "../../../components/Graph/GraphView";

const GraphPage = () => {
  const { graphData, loading, fetchGraph, expandNode } = useGraph();

  useEffect(() => {
    fetchGraph();
  }, [fetchGraph]);

  const handleNodeClick = (node) => {
    // Navigate neighbours on click
    expandNode(node.id);
  };

  return (
    <Container maxWidth="xl" sx={{ mt: 4, mb: 4, height: "80vh" }}>
      <Box mb={2}>
        <Typography variant="h4" component="h1" gutterBottom>
          Link Graph
        </Typography>
      </Box>
      <Box flex={1} height="100%">
        <GraphView graphData={graphData} loading={loading} onNodeClick={handleNodeClick} />
      </Box>
    </Container>
  );
};

export default GraphPage;
