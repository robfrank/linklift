import React, { useEffect } from "react";
import { Container, Typography, Box, Button, Paper, CircularProgress } from "@mui/material";
import { Add as AddIcon } from "@mui/icons-material";
import { Link as RouterLink } from "react-router-dom";
import { useLinks } from "../hooks/useLinks";
import { LinkList } from "../components/LinkList";

import { AddToCollectionDialog } from "../components/AddToCollectionDialog";

const HomePage = () => {
  const {
    links,
    isLoadingLinks,
    listLinksError,
    page,
    size,
    totalPages,
    totalElements,
    sortBy,
    sortDirection,
    fetchLinks,
    setPage,
    setPageSize,
    setSort,
    deleteLink
  } = useLinks();

  const [addToCollectionDialogOpen, setAddToCollectionDialogOpen] = React.useState(false);
  const [linkToAddToCollection, setLinkToAddToCollection] = React.useState<string | null>(null);

  useEffect(() => {
    fetchLinks({ page, size, sortBy, sortDirection });
  }, [page, size, sortBy, sortDirection, fetchLinks]);

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  const handlePageSizeChange = (newSize: number) => {
    setPageSize(newSize);
  };

  const handleSortChange = (newSortBy: string) => {
    if (sortBy === newSortBy) return;
    setSort(newSortBy, sortDirection);
  };

  const handleSortDirectionToggle = () => {
    const newDirection = sortDirection === "ASC" ? "DESC" : "ASC";
    setSort(sortBy, newDirection);
  };

  const handleAddToCollection = (linkId: string) => {
    setLinkToAddToCollection(linkId);
    setAddToCollectionDialogOpen(true);
  };

  if (isLoadingLinks && links.length === 0) {
    return (
      <Container maxWidth="lg">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
          <CircularProgress size={60} />
        </Box>
      </Container>
    );
  }

  // Show Welcome standard empty state if no links and no error (and not filtering - wait, if we are filtering this might be wrong, but for now we only have list)
  // We should only show Welcome if we are on page 0 and totalElements is 0.
  if (totalElements === 0 && !listLinksError && !isLoadingLinks) {
    return (
      <Container maxWidth="md">
        <Box my={8} textAlign="center">
          <Paper elevation={3} sx={{ p: 6 }}>
            <Typography variant="h3" component="h1" gutterBottom color="primary">
              Welcome to LinkLift
            </Typography>
            <Typography variant="h5" paragraph color="text.secondary">
              A simple and efficient way to manage your favorite links
            </Typography>
            <Typography variant="body1" paragraph color="text.secondary">
              Get started by adding your first link to your collection.
            </Typography>
            <Box mt={4}>
              <Button component={RouterLink} to="/add" variant="contained" size="large" startIcon={<AddIcon />} sx={{ px: 4, py: 1.5 }}>
                Add New Link
              </Button>
            </Box>
          </Paper>
        </Box>
      </Container>
    );
  }

  return (
    <>
      <LinkList
        links={links}
        isLoading={isLoadingLinks}
        error={listLinksError}
        page={page}
        size={size}
        totalPages={totalPages}
        totalElements={totalElements}
        sortBy={sortBy}
        sortDirection={sortDirection}
        onPageChange={handlePageChange}
        onPageSizeChange={handlePageSizeChange}
        onSortChange={handleSortChange}
        onSortDirectionToggle={handleSortDirectionToggle}
        onDelete={deleteLink}
        onAddToCollection={handleAddToCollection}
      />
      <AddToCollectionDialog open={addToCollectionDialogOpen} onClose={() => setAddToCollectionDialogOpen(false)} linkId={linkToAddToCollection} />
    </>
  );
};

export default HomePage;
