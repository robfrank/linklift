import React, { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { Container, Typography, Box, Paper, Grid, Card, CardContent, Button, CircularProgress, Alert, Divider } from "@mui/material";
import { OpenInNew, Article } from "@mui/icons-material";
import { useSearch } from "../hooks/useSearch";
import { ContentViewerModal } from "../components/ContentViewer/ContentViewerModal";
import { Content, ContentResponse } from "../../../domain/models/Content";

const SearchPage = () => {
  const location = useLocation();
  const { searchResults, isSearching, searchError, performSearch } = useSearch();
  const [selectedContent, setSelectedContent] = React.useState<{ linkId: string; title: string } | null>(null);

  const queryParams = new URLSearchParams(location.search);
  const query = queryParams.get("q") || "";

  useEffect(() => {
    if (query) {
      performSearch(query);
    }
  }, [query, performSearch]);

  const handleViewContent = (linkId: string, title: string) => {
    setSelectedContent({ linkId, title });
  };

  return (
    <Container maxWidth="lg">
      <Box my={4}>
        <Typography variant="h4" component="h1" gutterBottom>
          Search Results
        </Typography>
        {query && (
          <Typography variant="body1" color="text.secondary" gutterBottom>
            Showing results for: <strong>{query}</strong> (Vector Search)
          </Typography>
        )}

        <Divider sx={{ my: 3 }} />

        {isSearching ? (
          <Box display="flex" justifyContent="center" my={8}>
            <CircularProgress />
          </Box>
        ) : searchError ? (
          <Alert severity="error">{searchError}</Alert>
        ) : searchResults.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: "center", bgcolor: "grey.50" }}>
            <Typography variant="h6" color="text.secondary">
              No results found for your query.
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Try different keywords or ensure the backfill process has completed.
            </Typography>
          </Paper>
        ) : (
          <Grid container spacing={3}>
            {searchResults.map((response: ContentResponse) => (
              <Grid size={12} key={response.data.id}>
                <Card elevation={2}>
                  <CardContent>
                    <Typography variant="h6" component="h2" gutterBottom>
                      {response.data.extractedTitle || "Untitled Content"}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" paragraph>
                      {response.data.summary ||
                        response.data.extractedDescription ||
                        (response.data.textContent ? response.data.textContent.substring(0, 200) + "..." : "No summary available.")}
                    </Typography>
                    <Box display="flex" justifyContent="space-between" alignItems="center">
                      <Typography variant="caption" color="text.secondary">
                        Link ID: {response.data.linkId}
                      </Typography>
                      <Box>
                        <Button
                          size="small"
                          startIcon={<Article />}
                          variant="contained"
                          sx={{ mr: 1 }}
                          onClick={() => handleViewContent(response.data.linkId, response.data.extractedTitle || "Content")}
                        >
                          View Full Content
                        </Button>
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Box>

      {selectedContent && <ContentViewerModal linkId={selectedContent.linkId} linkTitle={selectedContent.title} onClose={() => setSelectedContent(null)} />}
    </Container>
  );
};

export default SearchPage;
