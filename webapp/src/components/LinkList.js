import React, { useState, useEffect } from "react";
import {
  Container,
  Typography,
  Paper,
  Box,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
  Alert,
  Pagination,
  FormControl,
  Select,
  MenuItem,
  InputLabel,
  Grid,
  Link,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
  TextField
} from "@mui/material";
import { OpenInNew, Sort, Article, PlaylistAdd, Edit, Delete } from "@mui/icons-material";
import api from "../services/api";
import { ContentViewerModal } from "./ContentViewer/ContentViewerModal";
import LinkListSkeleton from "./Skeletons/LinkListSkeleton";
import { useSnackbar } from "../contexts/SnackbarContext";

const LinkList = () => {
  const { showSnackbar } = useSnackbar();
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [sortBy, setSortBy] = useState("extractedAt");
  const [sortDirection, setSortDirection] = useState("DESC");
  const [selectedLink, setSelectedLink] = useState(null);

  // Add to Collection State
  const [addToCollectionDialogOpen, setAddToCollectionDialogOpen] = useState(false);
  const [linkToAddToCollection, setLinkToAddToCollection] = useState(null);
  const [userCollections, setUserCollections] = useState([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState("");
  const [addToCollectionLoading, setAddToCollectionLoading] = useState(false);

  // Edit Link State
  const [editLinkDialogOpen, setEditLinkDialogOpen] = useState(false);
  const [linkToEdit, setLinkToEdit] = useState(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");

  // Delete Link State
  const [deleteLinkDialogOpen, setDeleteLinkDialogOpen] = useState(false);
  const [linkToDelete, setLinkToDelete] = useState(null);

  const fetchLinks = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await api.listLinks({
        page: page - 1, // Backend uses 0-based pagination
        size: pageSize,
        sortBy: sortBy,
        sortDirection: sortDirection
      });

      setLinks(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (err) {
      console.error("Error fetching links:", err);
      setError("Failed to load links. Please try again.");
      showSnackbar("Failed to load links", "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLinks();
  }, [page, pageSize, sortBy, sortDirection]);

  const handlePageChange = (event, newPage) => {
    setPage(newPage);
  };

  const handlePageSizeChange = (event) => {
    setPageSize(event.target.value);
    setPage(1); // Reset to first page when changing page size
  };

  const handleSortChange = (event) => {
    setSortBy(event.target.value);
    setPage(1); // Reset to first page when changing sort
  };

  const toggleSortDirection = () => {
    setSortDirection((prev) => (prev === "ASC" ? "DESC" : "ASC"));
    setPage(1);
  };

  const handleOpenAddToCollectionDialog = async (link) => {
    setLinkToAddToCollection(link);
    setAddToCollectionDialogOpen(true);
    setAddToCollectionLoading(true);
    try {
      const response = await api.listCollections();
      setUserCollections(response.data || []);
    } catch (err) {
      console.error("Error fetching collections:", err);
      showSnackbar("Failed to fetch collections", "error");
    } finally {
      setAddToCollectionLoading(false);
    }
  };

  const handleAddToCollection = async () => {
    if (!selectedCollectionId || !linkToAddToCollection) return;

    try {
      setAddToCollectionLoading(true);
      await api.addLinkToCollection(selectedCollectionId, linkToAddToCollection.id);
      setAddToCollectionDialogOpen(false);
      setSelectedCollectionId("");
      setLinkToAddToCollection(null);
      setLinkToAddToCollection(null);
      showSnackbar("Link added to collection", "success");
    } catch (err) {
      console.error("Error adding link to collection:", err);
      showSnackbar("Failed to add link to collection", "error");
    } finally {
      setAddToCollectionLoading(false);
    }
  };

  const handleEditClick = (link) => {
    setLinkToEdit(link);
    setEditTitle(link.title || "");
    setEditDescription(link.description || "");
    setEditLinkDialogOpen(true);
  };

  const handleUpdateLink = async () => {
    if (!linkToEdit) return;
    try {
      const response = await api.updateLink(linkToEdit.id, { title: editTitle, description: editDescription });
      const updatedLink = response.data;
      setLinks((prevLinks) => prevLinks.map((link) => (link.id === updatedLink.id ? updatedLink : link)));
      setEditLinkDialogOpen(false);
      setLinkToEdit(null);
      showSnackbar("Link updated successfully", "success");
    } catch (err) {
      console.error("Error updating link:", err);
      setError("Failed to update link.");
      showSnackbar("Failed to update link", "error");
    }
  };

  const handleDeleteClick = (link) => {
    setLinkToDelete(link);
    setDeleteLinkDialogOpen(true);
  };

  const handleConfirmDelete = async () => {
    if (!linkToDelete) return;
    try {
      await api.deleteLink(linkToDelete.id);
      setLinks((prevLinks) => prevLinks.filter((link) => link.id !== linkToDelete.id));
      setDeleteLinkDialogOpen(false);
      setLinkToDelete(null);
      showSnackbar("Link deleted successfully", "success");
    } catch (err) {
      console.error("Error deleting link:", err);
      setError("Failed to delete link.");
      showSnackbar("Failed to delete link", "error");
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  const formatUrl = (url) => {
    try {
      const urlObj = new URL(url);
      return urlObj.hostname;
    } catch {
      return url;
    }
  };

  if (loading && links.length === 0) {
    return (
      <Container maxWidth="lg">
        <Box my={4}>
          <LinkListSkeleton count={5} />
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <Box my={4}>
        <Paper elevation={3} sx={{ p: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Typography variant="h4" component="h1">
              My Links
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {totalElements} link{totalElements !== 1 ? "s" : ""} total
            </Typography>
          </Box>

          {/* Controls */}
          <Box display="flex" gap={2} mb={3} flexWrap="wrap">
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Sort by</InputLabel>
              <Select value={sortBy} label="Sort by" onChange={handleSortChange}>
                <MenuItem value="extractedAt">Date Added</MenuItem>
                <MenuItem value="title">Title</MenuItem>
                <MenuItem value="url">URL</MenuItem>
              </Select>
            </FormControl>

            <Tooltip title={`Sort ${sortDirection === "ASC" ? "Ascending" : "Descending"}`}>
              <IconButton onClick={toggleSortDirection} size="small">
                <Sort sx={{ transform: sortDirection === "ASC" ? "rotate(180deg)" : "none" }} />
              </IconButton>
            </Tooltip>

            <FormControl size="small" sx={{ minWidth: 100 }}>
              <InputLabel>Per page</InputLabel>
              <Select value={pageSize} label="Per page" onChange={handlePageSizeChange}>
                <MenuItem value={5}>5</MenuItem>
                <MenuItem value={10}>10</MenuItem>
                <MenuItem value={20}>20</MenuItem>
                <MenuItem value={50}>50</MenuItem>
              </Select>
            </FormControl>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          {/* Links Grid */}
          {links.length === 0 && !loading ? (
            <Paper sx={{ p: 4, textAlign: "center", backgroundColor: "grey.50" }}>
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No links found
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Start by adding your first link!
              </Typography>
            </Paper>
          ) : (
            <>
              <Grid container spacing={2}>
                {links.map((link) => (
                  <Grid item xs={12} key={link.id}>
                    <Card elevation={1} sx={{ transition: "elevation 0.2s", "&:hover": { elevation: 3 } }}>
                      <CardContent>
                        <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                          <Typography variant="h6" component="h3" sx={{ fontWeight: 500 }}>
                            {link.title}
                          </Typography>
                          <Chip label={formatUrl(link.url)} size="small" variant="outlined" color="primary" />
                        </Box>

                        <Typography variant="body2" color="text.secondary" paragraph>
                          {link.description}
                        </Typography>

                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Typography variant="caption" color="text.secondary">
                            Added {formatDate(link.extractedAt)}
                          </Typography>

                          <CardActions sx={{ p: 0, gap: 1 }}>
                            <Button size="small" onClick={() => setSelectedLink(link)} startIcon={<Article />} variant="contained">
                              View Content
                            </Button>
                            <Button
                              size="small"
                              component={Link}
                              href={link.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              startIcon={<OpenInNew />}
                              variant="outlined"
                            >
                              Visit
                            </Button>
                            <Tooltip title="Add to Collection">
                              <IconButton size="small" onClick={() => handleOpenAddToCollectionDialog(link)} color="primary">
                                <PlaylistAdd />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Edit">
                              <IconButton size="small" onClick={() => handleEditClick(link)} color="primary">
                                <Edit />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Delete">
                              <IconButton size="small" onClick={() => handleDeleteClick(link)} color="error">
                                <Delete />
                              </IconButton>
                            </Tooltip>
                          </CardActions>
                        </Box>
                      </CardContent>
                    </Card>
                  </Grid>
                ))}
              </Grid>

              {/* Pagination */}
              {totalPages > 1 && (
                <Box display="flex" justifyContent="center" mt={4}>
                  <Pagination count={totalPages} page={page} onChange={handlePageChange} color="primary" size="large" showFirstButton showLastButton />
                </Box>
              )}

              {loading && (
                <Box display="flex" justifyContent="center" mt={2}>
                  <CircularProgress size={30} />
                </Box>
              )}
            </>
          )}
        </Paper>
      </Box>

      {/* Content Viewer Modal */}
      {selectedLink && <ContentViewerModal linkId={selectedLink.id} linkTitle={selectedLink.title} onClose={() => setSelectedLink(null)} />}

      {/* Add to Collection Dialog */}
      <Dialog open={addToCollectionDialogOpen} onClose={() => setAddToCollectionDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Add to Collection</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Select a collection to add <strong>{linkToAddToCollection?.title}</strong> to:
          </DialogContentText>

          {addToCollectionLoading && userCollections.length === 0 ? (
            <Box display="flex" justifyContent="center" my={2}>
              <CircularProgress size={24} />
            </Box>
          ) : userCollections.length === 0 ? (
            <Alert severity="info">You don't have any collections yet. Create one in the Collections page.</Alert>
          ) : (
            <FormControl fullWidth>
              <InputLabel id="collection-select-label">Collection</InputLabel>
              <Select
                labelId="collection-select-label"
                value={selectedCollectionId}
                label="Collection"
                onChange={(e) => setSelectedCollectionId(e.target.value)}
              >
                {userCollections.map((collection) => (
                  <MenuItem key={collection.id} value={collection.id}>
                    {collection.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setAddToCollectionDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleAddToCollection} variant="contained" disabled={!selectedCollectionId || addToCollectionLoading}>
            Add
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Link Dialog */}
      <Dialog open={editLinkDialogOpen} onClose={() => setEditLinkDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Link</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Title"
            fullWidth
            variant="outlined"
            value={editTitle}
            onChange={(e) => setEditTitle(e.target.value)}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Description"
            fullWidth
            multiline
            rows={4}
            variant="outlined"
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditLinkDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleUpdateLink} variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Link Dialog */}
      <Dialog open={deleteLinkDialogOpen} onClose={() => setDeleteLinkDialogOpen(false)}>
        <DialogTitle>Delete Link</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete <strong>{linkToDelete?.title}</strong>? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteLinkDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleConfirmDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default LinkList;
