import React, { useState, useEffect } from "react";
import { Container, Typography, Box, Button, Paper, CircularProgress } from "@mui/material";
import { Add as AddIcon } from "@mui/icons-material";
import { Link as RouterLink } from "react-router-dom";
import LinkList from "./LinkList";
import api from "../services/api";

const Home = () => {
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchLinks = async () => {
      try {
        setLoading(true);
        setError(null);
        const response = await api.listLinks({ page: 0, size: 20 });
        setLinks(response.content || []);
      } catch (err) {
        console.error("Error fetching links:", err);
        setError(err);
        setLinks([]);
      } finally {
        setLoading(false);
      }
    };

    fetchLinks();
  }, []);

  if (loading) {
    return (
      <Container maxWidth="lg">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="50vh">
          <CircularProgress size={60} />
        </Box>
      </Container>
    );
  }

  if (links.length === 0 && !error) {
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

  return <LinkList />;
};

export default Home;
