import React from "react";
import { Grid, Card, CardContent, Typography, Box, Button } from "@mui/material";
import { Article } from "@mui/icons-material";
import { ContentResponse } from "../../../../domain/models/Content";

interface ContentListProps {
  results: ContentResponse[];
  onViewContent: (linkId: string, title: string) => void;
}

export const ContentList: React.FC<ContentListProps> = ({ results, onViewContent }) => {
  return (
    <Grid container spacing={3}>
      {results.map((response: ContentResponse) => (
        <Grid size={{ xs: 12 }} key={response.data.id || response.data.linkId}>
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
                    onClick={() => onViewContent(response.data.linkId, response.data.extractedTitle || "Content")}
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
  );
};
