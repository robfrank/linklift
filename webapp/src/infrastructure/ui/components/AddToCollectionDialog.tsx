import React, { useEffect, useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  List,
  ListItem,
  ListItemText,
  CircularProgress,
  Alert,
  Typography,
  Box,
  ListItemButton
} from "@mui/material";
import { Folder } from "@mui/icons-material";
import { useCollections } from "../hooks/useCollections";

interface AddToCollectionDialogProps {
  open: boolean;
  onClose: () => void;
  linkId: string | null;
}

export const AddToCollectionDialog: React.FC<AddToCollectionDialogProps> = ({ open, onClose, linkId }) => {
  const {
    collections,
    isLoadingCollections,
    fetchCollections,
    addLinkToCollection,
    isAddingLinkToCollection,
    addLinkToCollectionError,
    resetAddLinkToCollectionError
  } = useCollections();

  useEffect(() => {
    if (open) {
      fetchCollections();
      resetAddLinkToCollectionError();
    }
  }, [open, fetchCollections, resetAddLinkToCollectionError]);

  const handleCollectionClick = async (collectionId: string) => {
    if (!linkId) return;
    try {
      await addLinkToCollection(collectionId, linkId);
      onClose();
      // Optional: Show success snackbar (managed by parent or global)
    } catch (error) {
      // Error managed by local state
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add to Collection</DialogTitle>
      <DialogContent>
        {addLinkToCollectionError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {addLinkToCollectionError}
          </Alert>
        )}

        {isLoadingCollections ? (
          <Box display="flex" justifyContent="center" p={3}>
            <CircularProgress />
          </Box>
        ) : collections.length === 0 ? (
          <Box textAlign="center" py={3}>
            <Typography color="text.secondary">No collections found. Create a collection first.</Typography>
          </Box>
        ) : (
          <List>
            {collections.map((collection) => (
              <ListItem key={collection.id} disablePadding>
                <ListItemButton onClick={() => handleCollectionClick(collection.id)} disabled={isAddingLinkToCollection}>
                  <Folder color="primary" sx={{ mr: 2 }} />
                  <ListItemText primary={collection.name} secondary={collection.description} />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isAddingLinkToCollection}>
          Cancel
        </Button>
      </DialogActions>
    </Dialog>
  );
};
