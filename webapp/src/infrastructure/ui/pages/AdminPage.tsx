import React from "react";
import { Container, Typography, Box, Paper, Button, CircularProgress, Alert, Divider, Card, CardContent, CardHeader, Grid } from "@mui/material";
import { Storage, PlayArrow } from "@mui/icons-material";
import { useSearch } from "../hooks/useSearch";

const AdminPage = () => {
  const { triggerBackfill, isBackfilling, backfillMessage } = useSearch();

  const handleBackfill = async () => {
    await triggerBackfill();
  };

  return (
    <Container maxWidth="md">
      <Box my={4}>
        <Typography variant="h4" component="h1" gutterBottom>
          Admin Dashboard
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          System maintenance and backend operations.
        </Typography>

        <Divider sx={{ my: 3 }} />

        <Grid size={12}>
          <Card elevation={3}>
            <CardHeader title="Vector Search Maintenance" subheader="Manage embeddings and vector backfill" avatar={<Storage color="primary" />} />
            <CardContent>
              <Typography variant="body1" paragraph>
                The vector search functionality requires embeddings for all link content. If you have existing links without embeddings, or if you've changed
                the embedding model, you can trigger a backfill process.
              </Typography>

              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Note: This process runs in the background on the server and may take some time depending on the number of links.
              </Typography>

              {backfillMessage && (
                <Alert severity={backfillMessage.includes("Failed") ? "error" : "success"} sx={{ mb: 3 }}>
                  {backfillMessage}
                </Alert>
              )}

              <Button
                variant="contained"
                color="primary"
                onClick={handleBackfill}
                disabled={isBackfilling}
                startIcon={isBackfilling ? <CircularProgress size={20} color="inherit" /> : <PlayArrow />}
              >
                {isBackfilling ? "Backfilling..." : "Trigger Embedding Backfill"}
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </Box>
    </Container>
  );
};

export default AdminPage;
