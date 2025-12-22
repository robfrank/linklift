export interface LinkNode {
  id: string;
  label: string;
  url: string;
  // properties required by react-force-graph-2d
  x?: number;
  y?: number;
  val?: number; // for node size
  color?: string;
}

export interface LinkEdge {
  source: string | LinkNode; // react-force-graph-2d replaces string id with node object
  target: string | LinkNode;
}

export interface GraphData {
  nodes: LinkNode[];
  edges: LinkEdge[];
}
