import React from "react";
import {
  Container,
  Typography,
  Paper,
  Box,
  Card,
  CardContent,
  CardActions,
  Button,
  Chip,
  CircularProgress,
  Alert,
  Pagination,
  FormControl,
  Select,
  MenuItem,
  InputLabel,
  Grid,
  Link as MuiLink,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Stack
} from "@mui/material";
import {
  OpenInNew,
  Sort,
  Article,
  PlaylistAdd,
  Edit,
  Delete,
  Bookmark,
  BookmarkBorder,
  Archive,
  Unarchive,
  CheckCircle,
  RadioButtonUnchecked
} from "@mui/icons-material";
import { Link, ReadStatus, UpdateLinkStatusDTO } from "../../../domain/models/Link";
import { ContentViewerModal } from "./ContentViewer/ContentViewerModal";

interface LinkListProps {
  links: Link[];
  isLoading: boolean;
  error: string | null;
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;

  onPageChange: (newPage: number) => void;
  onDelete: (id: string) => void;
  onAddToCollection: (linkId: string) => void;
  onUpdateStatus?: (id: string, status: UpdateLinkStatusDTO) => void;
}

export const LinkList: React.FC<LinkListProps> = ({
  links,
  isLoading,
  error,
  page,
  size,
  totalPages,
  totalElements,
  onPageChange,
  onDelete,
  onAddToCollection,
  onUpdateStatus
}) => {
  const [deleteLinkDialogOpen, setDeleteLinkDialogOpen] = React.useState(false);
  const [linkToDelete, setLinkToDelete] = React.useState<Link | null>(null);
  const [selectedLink, setSelectedLink] = React.useState<Link | null>(null);

  const handleDeleteClick = (link: Link) => {
    setLinkToDelete(link);
    setDeleteLinkDialogOpen(true);
  };

  const handleConfirmDelete = () => {
    if (linkToDelete) {
      onDelete(linkToDelete.id);
      setDeleteLinkDialogOpen(false);
      setLinkToDelete(null);
    }
  };

  const handleViewContent = (link: Link) => {
    setSelectedLink(link);
  };

  // ... helper functions ...
  const formatDate = (dateString: string | undefined) => {
    if (!dateString) return "";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  const formatUrl = (url: string) => {
    try {
      const urlObj = new URL(url);
      return urlObj.hostname;
    } catch {
      return url;
    }
  };

  if (isLoading && links.length === 0) {
    return (
      <Container maxWidth="lg">
        <Box my={4} display="flex" justifyContent="center">
          <CircularProgress />
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="lg">
      <Box my={4}>
        <Paper elevation={3} sx={{ p: 3 }}>
          {/* ... Header and controls ... */}
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Typography variant="h4" component="h1">
              My Links
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {totalElements} link{totalElements !== 1 ? "s" : ""} total
            </Typography>
          </Box>

          {/* Controls removed - moved to sidebar */}

          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}

          {/* Links Grid */}
          {links.length === 0 && !isLoading ? (
            <Paper sx={{ p: 4, textAlign: "center", backgroundColor: "grey.50" }}>
              <Typography variant="h6" color="text.secondary" gutterBottom>
                No links found
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Start by adding your first link!
              </Typography>
            </Paper>
          ) : (
            <>
              <Stack spacing={2}>
                {links.map((link) => (
                  <Box key={link.id}>
                    <Card elevation={1} sx={{ transition: "elevation 0.2s", "&:hover": { elevation: 3 } }}>
                      <CardContent>
                        <Box display="flex" justifyContent="space-between" alignItems="flex-start" mb={2}>
                          <Box display="flex" alignItems="center" gap={1}>
                            <Typography variant="h6" component="h3" sx={{ fontWeight: 500, opacity: link.archived ? 0.6 : 1 }}>
                              {link.title}
                            </Typography>
                            {link.readStatus === "UNREAD" && (
                              <Chip label="Unread" size="small" color="info" variant="filled" sx={{ height: 18, fontSize: "0.65rem" }} />
                            )}
                            {link.readStatus === "READING" && (
                              <Chip label="Reading" size="small" color="warning" variant="filled" sx={{ height: 18, fontSize: "0.65rem" }} />
                            )}
                            {link.archived && (
                              <Chip label="Archived" size="small" color="default" variant="outlined" sx={{ height: 18, fontSize: "0.65rem" }} />
                            )}
                          </Box>
                          <Chip label={formatUrl(link.url)} size="small" variant="outlined" color="primary" />
                        </Box>

                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          {link.description}
                        </Typography>

                        <Box display="flex" justifyContent="space-between" alignItems="center">
                          <Typography variant="caption" color="text.secondary">
                            Added {formatDate(link.extractedAt)}
                          </Typography>

                          <CardActions sx={{ p: 0, gap: 1 }}>
                            <Button size="small" startIcon={<Article />} variant="contained" onClick={() => handleViewContent(link)}>
                              View Content
                            </Button>
                            <Button
                              size="small"
                              component={MuiLink}
                              href={link.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              startIcon={<OpenInNew />}
                              variant="outlined"
                            >
                              Visit
                            </Button>
                            <Tooltip title="Add to Collection">
                              <span>
                                <IconButton size="small" color="primary" onClick={() => onAddToCollection(link.id)}>
                                  <PlaylistAdd />
                                </IconButton>
                              </span>
                            </Tooltip>
                            {onUpdateStatus && (
                              <>
                                <Tooltip title={link.favorited ? "Remove from favorites" : "Add to favorites"}>
                                  <IconButton
                                    size="small"
                                    color={link.favorited ? "warning" : "default"}
                                    onClick={() => onUpdateStatus(link.id, { favorited: !link.favorited })}
                                  >
                                    {link.favorited ? <Bookmark /> : <BookmarkBorder />}
                                  </IconButton>
                                </Tooltip>
                                <Tooltip title={link.archived ? "Unarchive" : "Archive"}>
                                  <IconButton size="small" color="default" onClick={() => onUpdateStatus(link.id, { archived: !link.archived })}>
                                    {link.archived ? <Unarchive /> : <Archive />}
                                  </IconButton>
                                </Tooltip>
                                <Tooltip title={link.readStatus === "READ" ? "Mark as unread" : "Mark as read"}>
                                  <IconButton
                                    size="small"
                                    color={link.readStatus === "READ" ? "success" : "default"}
                                    onClick={() => onUpdateStatus(link.id, { readStatus: link.readStatus === "READ" ? "UNREAD" : "READ" })}
                                  >
                                    {link.readStatus === "READ" ? <CheckCircle /> : <RadioButtonUnchecked />}
                                  </IconButton>
                                </Tooltip>
                              </>
                            )}
                            <Tooltip title="Edit">
                              <span>
                                <IconButton size="small" color="primary" disabled>
                                  <Edit />
                                </IconButton>
                              </span>
                            </Tooltip>
                            <Tooltip title="Delete">
                              <IconButton size="small" color="error" onClick={() => handleDeleteClick(link)}>
                                <Delete />
                              </IconButton>
                            </Tooltip>
                          </CardActions>
                        </Box>
                      </CardContent>
                    </Card>
                  </Box>
                ))}
              </Stack>

              {/* Pagination */}
              {totalPages > 1 && (
                <Box display="flex" justifyContent="center" mt={4}>
                  <Pagination
                    count={totalPages}
                    page={page + 1}
                    onChange={(_, p) => onPageChange(p - 1)}
                    color="primary"
                    size="large"
                    showFirstButton
                    showLastButton
                  />
                </Box>
              )}

              {isLoading && (
                <Box display="flex" justifyContent="center" mt={2}>
                  <CircularProgress size={30} />
                </Box>
              )}
            </>
          )}
        </Paper>
      </Box>

      {/* Delete Link Dialog */}
      <Dialog open={deleteLinkDialogOpen} onClose={() => setDeleteLinkDialogOpen(false)}>
        <DialogTitle>Delete Link</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete <strong>{linkToDelete?.title}</strong>? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteLinkDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleConfirmDelete} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Content Viewer Modal */}
      {selectedLink && <ContentViewerModal linkId={selectedLink.id} linkTitle={selectedLink.title} onClose={() => setSelectedLink(null)} />}
    </Container>
  );
};
