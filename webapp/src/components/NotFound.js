import React from "react";
import { Container, Typography, Paper, Box, Button } from "@mui/material";
import { Link as RouterLink } from "react-router-dom";

const NotFound = () => {
  return (
    <Container maxWidth="md">
      <Box my={4}>
        <Paper elevation={3} sx={{ p: 4, textAlign: "center" }}>
          <Typography variant="h2" component="h1" gutterBottom>
            404
          </Typography>
          <Typography variant="h5" component="h2" gutterBottom>
            Page Not Found
          </Typography>
          <Typography variant="body1" paragraph>
            The page you are looking for does not exist.
          </Typography>
          <Button variant="contained" color="primary" component={RouterLink} to="/">
            Return to Home
          </Button>
        </Paper>
      </Box>
    </Container>
  );
};

export default NotFound;
