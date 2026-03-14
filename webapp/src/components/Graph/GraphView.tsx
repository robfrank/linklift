import React, { useRef, useEffect, useState } from "react";
import ForceGraph2D from "react-force-graph-2d"; // Might lack types
import { Box, Paper, CircularProgress, Typography } from "@mui/material";

interface GraphData {
  nodes: { id: string; label: string; color?: string; [key: string]: any }[];
  links: { source: string; target: string; [key: string]: any }[];
}

interface GraphViewProps {
  graphData: GraphData;
  loading: boolean;
  onNodeClick?: (node: any) => void;
  highlightIds?: string[];
}

const GraphView: React.FC<GraphViewProps> = ({ graphData, loading, onNodeClick, highlightIds = [] }) => {
  const fgRef = useRef<any>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 800, height: 600 });

  useEffect(() => {
    // Center graph on data change
    if (fgRef.current && graphData.nodes && graphData.nodes.length > 0) {
      fgRef.current.d3Force("charge").strength(-120);
      fgRef.current.zoomToFit(400, 20);
    }
  }, [graphData]);

  useEffect(() => {
    const observer = new ResizeObserver((entries) => {
      for (let entry of entries) {
        setDimensions({
          width: entry.contentRect.width,
          height: entry.contentRect.height
        });
      }
    });

    if (containerRef.current) {
      observer.observe(containerRef.current);
    }

    return () => observer.disconnect();
  }, []);

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

  const getNodeColor = (node: any) => {
    if (!highlightIds || highlightIds.length === 0) return node.color || "#1976d2";
    return highlightIds.includes(node.id) ? "#ff4081" : "#e0e0e0"; // Highlight vs Dimmed
  };

  return (
    <Paper elevation={3} style={{ height: "100%", overflow: "hidden" }} ref={containerRef}>
      <ForceGraph2D
        ref={fgRef}
        graphData={graphData}
        nodeLabel="label"
        nodeColor={getNodeColor}
        nodeRelSize={6}
        linkColor={() => "#ccc"}
        onNodeClick={onNodeClick}
        width={dimensions.width}
        height={dimensions.height}
      />
    </Paper>
  );
};

export default GraphView;
