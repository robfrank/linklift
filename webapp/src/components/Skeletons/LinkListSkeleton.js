import React from "react";
import { Grid, Card, CardContent, Box, Skeleton } from "@mui/material";

const LinkListSkeleton = ({ count = 3 }) => {
  return (
    <Grid container spacing={2}>
      {Array.from(new Array(count)).map((_, index) => (
        <Grid item xs={12} key={index}>
          <Card elevation={1}>
            <CardContent>
              <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                <Skeleton variant="text" width="60%" height={32} />
                <Skeleton variant="rounded" width={100} height={24} />
              </Box>

              <Skeleton variant="text" width="100%" />
              <Skeleton variant="text" width="80%" />

              <Box display="flex" justifyContent="space-between" alignItems="center" mt={2}>
                <Skeleton variant="text" width="30%" />
                <Box display="flex" gap={1}>
                  <Skeleton variant="rounded" width={80} height={30} />
                  <Skeleton variant="rounded" width={80} height={30} />
                  <Skeleton variant="circular" width={30} height={30} />
                  <Skeleton variant="circular" width={30} height={30} />
                  <Skeleton variant="circular" width={30} height={30} />
                </Box>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};

export default LinkListSkeleton;
