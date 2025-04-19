import React from "react";
import { Container, Typography, Paper, Box, Button } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";

const Home = () => {
  return (
    <Container maxWidth="md">
      <Box my={4}>
        <Paper elevation={3} sx={{ p: 4, textAlign: "center" }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Welcome to LinkLift
          </Typography>
          <Typography variant="body1" paragraph>
            A simple and efficient way to manage your links.
          </Typography>
          <Button variant="contained" color="primary" component={RouterLink} to="/add" size="large">
            Add New Link
          </Button>
        </Paper>
      </Box>
    </Container>
  );
};

export default Home;
