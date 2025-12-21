import React, { useRef, useEffect } from "react";
import ForceGraph2D from "react-force-graph-2d";
import { Box, Paper, CircularProgress, Typography } from "@mui/material";

const GraphView = ({ graphData, loading, onNodeClick }) => {
  const fgRef = useRef();

  useEffect(() => {
    // Center graph on data change
    if (fgRef.current && graphData.nodes.length > 0) {
      fgRef.current.d3Force("charge").strength(-120);
      fgRef.current.zoomToFit(400, 20);
    }
  }, [graphData]);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="100%">
        <CircularProgress />
      </Box>
    );
  }

  if (!graphData.nodes || graphData.nodes.length === 0) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height="100%">
        <Typography variant="body1">No graph data available.</Typography>
      </Box>
    );
  }

  return (
    <Paper elevation={3} style={{ height: "100%", overflow: "hidden" }}>
      <ForceGraph2D
        ref={fgRef}
        graphData={graphData}
        nodeLabel="label"
        nodeColor={(node) => node.color || "#1976d2"} // Default color
        nodeRelSize={6}
        linkColor={() => "#ccc"}
        onNodeClick={onNodeClick}
        width={window.innerWidth - 300} // Approximate width adjustment
        height={window.innerHeight - 150} // Approximate height adjustment
      />
    </Paper>
  );
};

export default GraphView;
