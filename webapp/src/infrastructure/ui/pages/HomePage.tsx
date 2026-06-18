import React, { useEffect, useState } from "react";
import { Container, Box, Tab, Tabs, Typography, CircularProgress, Paper, Fab } from "@mui/material";
import { Add as AddIcon, ViewList, Hub } from "@mui/icons-material";
import { Link as RouterLink, useLocation, useNavigate } from "react-router-dom";
import { useLinks } from "../hooks/useLinks";
import { useSearch } from "../hooks/useSearch";
import { LinkList } from "../components/LinkList";
import { ContentList } from "../components/Content/ContentList";
import GraphView from "../../../components/Graph/GraphView";
import useGraph from "../../../hooks/useGraph";
import { ContentViewerModal } from "../components/ContentViewer/ContentViewerModal"; // Ensure import
import { AddToCollectionDialog } from "../components/AddToCollectionDialog";

const HomePage = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const queryParams = new URLSearchParams(location.search);
  const viewParam = queryParams.get("view") || "list";
  const collectionParam = queryParams.get("collection");

  const [tabValue, setTabValue] = useState(viewParam === "graph" ? 1 : 0);

  const {
    links,
    isLoadingLinks,
    listLinksError,
    fetchLinks,
    deleteLink,
    updateLinkStatus,
    page,
    size,
    totalPages,
    totalElements,
    setPage,
    setPageSize,
    setSort,
    sortBy,
    sortDirection
  } = useLinks();
  const { searchResults, isSearching, performSearch } = useSearch();
  const { graphData, fetchGraph, loading: graphLoading } = useGraph();

  const [selectedContent, setSelectedContent] = useState<{ linkId: string; title: string } | null>(null);

  // Sync tab with URL
  useEffect(() => {
    setTabValue(viewParam === "graph" ? 1 : 0);
  }, [viewParam]);

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
    const newView = newValue === 1 ? "graph" : "list";
    const newSearch = new URLSearchParams(location.search);
    newSearch.set("view", newView);
    navigate({ search: newSearch.toString() });
  };

  // Auto-fetch data
  useEffect(() => {
    // Fetch only the active tab's data; switching tabs re-runs this via viewParam.
    if (viewParam === "graph") {
      fetchGraph();
    } else {
      fetchLinks({ page, size });
    }
  }, [viewParam, fetchLinks, fetchGraph, page, size]);

  // Handle search being active
  const isSearchActive = searchResults.length > 0 || isSearching;

  // Highlight IDs for graph
  const highlightIds = isSearchActive ? searchResults.map((r) => r.data.linkId) : [];

  const [addToCollectionDialogOpen, setAddToCollectionDialogOpen] = useState(false);
  const [linkToAddToCollection, setLinkToAddToCollection] = useState<string | null>(null);

  const handleViewContent = (linkId: string, title: string) => {
    setSelectedContent({ linkId, title });
  };

  const handleAddToCollection = (linkId: string) => {
    setLinkToAddToCollection(linkId);
    setAddToCollectionDialogOpen(true);
  };

  return (
    <Container maxWidth="xl">
      <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 2, display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="view tabs">
          <Tab icon={<ViewList />} iconPosition="start" label="List" />
          <Tab icon={<Hub />} iconPosition="start" label="Graph" />
        </Tabs>

        <Fab color="primary" aria-label="add" size="small" component={RouterLink} to="/add">
          <AddIcon />
        </Fab>
      </Box>

      <Box role="tabpanel" hidden={tabValue !== 0}>
        {tabValue === 0 &&
          (isSearchActive ? (
            <ContentList results={searchResults} onViewContent={handleViewContent} />
          ) : (
            <LinkList
              links={links}
              isLoading={isLoadingLinks}
              error={listLinksError}
              page={page}
              size={size}
              totalPages={totalPages}
              totalElements={totalElements || 0}
              onPageChange={setPage}
              onDelete={deleteLink}
              onAddToCollection={handleAddToCollection}
              onUpdateStatus={updateLinkStatus}
            />
          ))}
      </Box>

      <Box role="tabpanel" hidden={tabValue !== 1} sx={{ height: "calc(100vh - 200px)" }}>
        {tabValue === 1 && (
          <GraphView
            graphData={graphData}
            loading={graphLoading}
            highlightIds={highlightIds}
            onNodeClick={(node: any) => {
              // Expand or view details?
              console.log("Clicked node", node);
            }}
          />
        )}
      </Box>

      {selectedContent && <ContentViewerModal linkId={selectedContent.linkId} linkTitle={selectedContent.title} onClose={() => setSelectedContent(null)} />}

      <AddToCollectionDialog open={addToCollectionDialogOpen} onClose={() => setAddToCollectionDialogOpen(false)} linkId={linkToAddToCollection} />
    </Container>
  );
};

export default HomePage;
