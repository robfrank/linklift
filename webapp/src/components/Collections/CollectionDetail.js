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
  IconButton,
  Chip,
  Breadcrumbs,
  Link as MuiLink
} from "@mui/material";
import { ArrowBack, Delete, OpenInNew, Article, Folder } from "@mui/icons-material";
import { useParams, useNavigate, Link as RouterLink } from "react-router-dom";
import api from "../../infrastructure/api/axios-instance";
import { ContentViewerModal } from "../../infrastructure/ui/components/ContentViewer/ContentViewerModal";

const CollectionDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [collection, setCollection] = useState(null);
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedLink, setSelectedLink] = useState(null);

  const fetchCollection = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await api.get(`/collections/${id}`);
      setCollection(data.data.data.collection);
      setLinks(data.data.data.links || []);
    } catch (err) {
      console.error("Error fetching collection:", err);
      setError("Failed to load collection details.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCollection();
  }, [id]);

  const handleRemoveLink = async (linkId) => {
    if (window.confirm("Remove this link from the collection?")) {
      try {
        await api.delete(`/collections/${id}/links/${linkId}`);
        setLinks(links.filter((link) => link.id !== linkId));
      } catch (err) {
        console.error("Error removing link:", err);
        // Ideally show error notification
      }
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric"
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

  if (loading) {
    return (
      <Container maxWidth="lg">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
          <CircularProgress size={60} />
        </Box>
      </Container>
    );
  }

  if (error || !collection) {
    return (
      <Container maxWidth="lg">
        <Box my={4}>
          <Alert severity="error">{error || "Collection not found"}</Alert>
          <Button startIcon={<ArrowBack />} onClick={() => navigate("/collections")} sx={{ mt: 2 }}>
            Back to Collections
          </Button>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <Box my={4}>
        {/* Breadcrumbs */}
        <Breadcrumbs aria-label="breadcrumb" sx={{ mb: 2 }}>
          <MuiLink component={RouterLink} to="/" color="inherit">
            Home
          </MuiLink>
          <MuiLink component={RouterLink} to="/collections" color="inherit">
            Collections
          </MuiLink>
          <Typography color="text.primary">{collection.name}</Typography>
        </Breadcrumbs>

        <Paper elevation={3} sx={{ p: 3, mb: 4 }}>
          <Box display="flex" alignItems="center" mb={2}>
            <Folder color="primary" sx={{ fontSize: 40, mr: 2 }} />
            <Box>
              <Typography variant="h4" component="h1">
                {collection.name}
              </Typography>
              <Typography variant="body1" color="text.secondary">
                {collection.description}
              </Typography>
            </Box>
          </Box>
        </Paper>

        <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
          <Typography variant="h5" component="h2">
            Links ({links.length})
          </Typography>
          <Button variant="outlined" startIcon={<ArrowBack />} onClick={() => navigate("/collections")}>
            Back
          </Button>
        </Box>

        {links.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: "center", backgroundColor: "grey.50" }}>
            <Typography variant="h6" color="text.secondary">
              No links in this collection yet
            </Typography>
            <Button component={RouterLink} to="/" variant="contained" sx={{ mt: 2 }}>
              Browse Links to Add
            </Button>
          </Paper>
        ) : (
          <Grid container spacing={2}>
            {links.map((link) => (
              <Grid size={12} key={link.id}>
                <Card elevation={1} sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", p: 1 }}>
                  <CardContent sx={{ flexGrow: 1, py: 1, "&:last-child": { pb: 1 } }}>
                    <Box display="flex" alignItems="center" gap={2}>
                      <Typography variant="h6" component="div">
                        {link.title}
                      </Typography>
                      <Chip label={formatUrl(link.url)} size="small" variant="outlined" />
                    </Box>
                    <Typography variant="body2" color="text.secondary" noWrap sx={{ maxWidth: "80%" }}>
                      {link.description}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Added {formatDate(link.extractedAt)}
                    </Typography>
                  </CardContent>
                  <CardActions>
                    <Button size="small" onClick={() => setSelectedLink(link)} startIcon={<Article />}>
                      Content
                    </Button>
                    <Button size="small" href={link.url} target="_blank" rel="noopener noreferrer" startIcon={<OpenInNew />}>
                      Visit
                    </Button>
                    <IconButton color="error" onClick={() => handleRemoveLink(link.id)} title="Remove from collection">
                      <Delete />
                    </IconButton>
                  </CardActions>
                </Card>
              </Grid>
            ))}
          </Grid>
        )}
      </Box>

      {/* Content Viewer Modal */}
      {selectedLink && <ContentViewerModal linkId={selectedLink.id} linkTitle={selectedLink.title} onClose={() => setSelectedLink(null)} />}
    </Container>
  );
};

export default CollectionDetail;
