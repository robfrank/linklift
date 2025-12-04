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
  Grid,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  IconButton,
  Tooltip
} from "@mui/material";
import { Add, Delete, Folder, ArrowForward } from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import api from "../../services/api";
import { useSnackbar } from "../../contexts/SnackbarContext";

const CollectionList = () => {
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  const [collections, setCollections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
  const [newCollection, setNewCollection] = useState({ name: "", description: "" });
  const [createLoading, setCreateLoading] = useState(false);

  const fetchCollections = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.listCollections();
      setCollections(data.data || []);
    } catch (err) {
      console.error("Error fetching collections:", err);
      setError("Failed to load collections. Please try again.");
      showSnackbar("Failed to load collections", "error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCollections();
  }, []);

  const handleCreateCollection = async () => {
    if (!newCollection.name.trim()) return;

    try {
      setCreateLoading(true);
      await api.createCollection(newCollection);
      setOpenCreateDialog(false);
      setNewCollection({ name: "", description: "" });
      fetchCollections();
      showSnackbar("Collection created successfully", "success");
    } catch (err) {
      console.error("Error creating collection:", err);
      showSnackbar("Failed to create collection", "error");
    } finally {
      setCreateLoading(false);
    }
  };

  const handleDeleteCollection = async (id, e) => {
    e.stopPropagation();
    if (window.confirm("Are you sure you want to delete this collection?")) {
      try {
        await api.deleteCollection(id);
        fetchCollections();
        showSnackbar("Collection deleted successfully", "success");
      } catch (err) {
        console.error("Error deleting collection:", err);
        showSnackbar("Failed to delete collection", "error");
      }
    }
  };

  if (loading && collections.length === 0) {
    return (
      <Container maxWidth="lg">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
          <CircularProgress size={60} />
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
              My Collections
            </Typography>
            <Button variant="contained" startIcon={<Add />} onClick={() => setOpenCreateDialog(true)}>
              New Collection
            </Button>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          {collections.length === 0 && !loading ? (
            <Paper sx={{ p: 4, textAlign: "center", backgroundColor: "grey.50" }}>
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No collections found
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Create your first collection to organize your links!
              </Typography>
            </Paper>
          ) : (
            <Grid container spacing={3}>
              {collections.map((collection) => (
                <Grid item xs={12} sm={6} md={4} key={collection.id}>
                  <Card
                    elevation={2}
                    sx={{
                      height: "100%",
                      display: "flex",
                      flexDirection: "column",
                      cursor: "pointer",
                      transition: "transform 0.2s",
                      "&:hover": { transform: "translateY(-4px)", elevation: 4 }
                    }}
                    onClick={() => navigate(`/collections/${collection.id}`)}
                  >
                    <CardContent sx={{ flexGrow: 1 }}>
                      <Box display="flex" alignItems="center" mb={2}>
                        <Folder color="primary" sx={{ mr: 1 }} />
                        <Typography variant="h6" component="h2" noWrap>
                          {collection.name}
                        </Typography>
                      </Box>
                      <Typography
                        variant="body2"
                        color="text.secondary"
                        sx={{
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          display: "-webkit-box",
                          WebkitLineClamp: 3,
                          WebkitBoxOrient: "vertical"
                        }}
                      >
                        {collection.description || "No description"}
                      </Typography>
                    </CardContent>
                    <CardActions sx={{ justifyContent: "space-between", px: 2, pb: 2 }}>
                      <Button size="small" endIcon={<ArrowForward />}>
                        View
                      </Button>
                      <IconButton size="small" color="error" onClick={(e) => handleDeleteCollection(collection.id, e)}>
                        <Delete fontSize="small" />
                      </IconButton>
                    </CardActions>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </Paper>
      </Box>

      {/* Create Collection Dialog */}
      <Dialog open={openCreateDialog} onClose={() => setOpenCreateDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Collection</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Collection Name"
            fullWidth
            variant="outlined"
            value={newCollection.name}
            onChange={(e) => setNewCollection({ ...newCollection, name: e.target.value })}
            required
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Description"
            fullWidth
            variant="outlined"
            multiline
            rows={3}
            value={newCollection.description}
            onChange={(e) => setNewCollection({ ...newCollection, description: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenCreateDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateCollection} variant="contained" disabled={!newCollection.name.trim() || createLoading}>
            {createLoading ? <CircularProgress size={24} /> : "Create"}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default CollectionList;
