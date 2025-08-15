import React, { useState, useEffect } from "react";
import { Container, Typography, Box, Button, Paper } from "@mui/material";
import { Add as AddIcon } from "@mui/icons-material";
import { Link as RouterLink } from "react-router-dom";
import LinkList from "./LinkList";
import api from "../services/api";

const Home = () => {
  const [hasLinks, setHasLinks] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkForLinks = async () => {
      try {
        const response = await api.listLinks({ page: 0, size: 1 });
        setHasLinks(response.totalElements > 0);
      } catch (error) {
        // If there's an error, assume no links and show welcome
        setHasLinks(false);
      } finally {
        setLoading(false);
      }
    };

    checkForLinks();
  }, []);

  if (loading) {
    return <LinkList />; // Let LinkList handle its own loading state
  }

  if (!hasLinks) {
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
