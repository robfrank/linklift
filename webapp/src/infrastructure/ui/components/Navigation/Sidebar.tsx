import React, { useEffect } from "react";
import {
  Drawer,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Toolbar,
  Typography,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem
} from "@mui/material";
import { Home as HomeIcon, Folder as FolderIcon, Add as AddIcon, Label as LabelIcon, AutoAwesome as AskIcon } from "@mui/icons-material";
import { useNavigate, useLocation } from "react-router-dom";
import { useCollections } from "../../hooks/useCollections";
import { useLinks } from "../../hooks/useLinks";

const drawerWidth = 240;

export const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { collections, fetchCollections } = useCollections();
  const { sortBy, sortDirection, size, setSort, setPageSize } = useLinks();

  useEffect(() => {
    fetchCollections();
  }, [fetchCollections]);

  const handleSortChange = (event: any) => {
    setSort(event.target.value, sortDirection);
  };

  const handleSortDirectionChange = (event: any) => {
    setSort(sortBy, event.target.value);
  };

  const handleSizeChange = (event: any) => {
    setPageSize(Number(event.target.value));
  };

  const handleNavigation = (path: string) => {
    navigate(path);
  };

  return (
    <Drawer
      variant="permanent"
      sx={{
        width: drawerWidth,
        flexShrink: 0,
        [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: "border-box" }
      }}
    >
      <Toolbar />
      <Box sx={{ overflow: "auto" }}>
        <Box sx={{ p: 2 }}>
          <Typography variant="overline" color="text.secondary" display="block" gutterBottom>
            Filters
          </Typography>
          <FormControl fullWidth size="small" margin="dense">
            <InputLabel>Sort by</InputLabel>
            <Select value={sortBy} label="Sort by" onChange={handleSortChange}>
              <MenuItem value="extractedAt">Date Added</MenuItem>
              <MenuItem value="title">Title</MenuItem>
              <MenuItem value="url">URL</MenuItem>
            </Select>
          </FormControl>

          <FormControl fullWidth size="small" margin="dense">
            <InputLabel>Order</InputLabel>
            <Select value={sortDirection} label="Order" onChange={handleSortDirectionChange}>
              <MenuItem value="ASC">Ascending</MenuItem>
              <MenuItem value="DESC">Descending</MenuItem>
            </Select>
          </FormControl>

          <FormControl fullWidth size="small" margin="dense">
            <InputLabel>Per page</InputLabel>
            <Select value={size} label="Per page" onChange={handleSizeChange}>
              <MenuItem value={5}>5</MenuItem>
              <MenuItem value={10}>10</MenuItem>
              <MenuItem value={20}>20</MenuItem>
              <MenuItem value={50}>50</MenuItem>
            </Select>
          </FormControl>
        </Box>
        <Divider />
        <List>
          <ListItem disablePadding>
            <ListItemButton onClick={() => handleNavigation("/")} selected={location.pathname === "/" && !location.search.includes("collection")}>
              <ListItemIcon>
                <HomeIcon />
              </ListItemIcon>
              <ListItemText primary="All Links" />
            </ListItemButton>
          </ListItem>
          <ListItem disablePadding>
            <ListItemButton onClick={() => handleNavigation("/ask")} selected={location.pathname === "/ask"}>
              <ListItemIcon>
                <AskIcon />
              </ListItemIcon>
              <ListItemText primary="Ask AI" />
            </ListItemButton>
          </ListItem>
        </List>
        <Divider />
        <Typography variant="overline" sx={{ px: 2, mt: 2, display: "block", color: "text.secondary" }}>
          Collections
        </Typography>
        <List dense>
          {collections.map((collection: any) => (
            <ListItem key={collection.id} disablePadding>
              <ListItemButton
                onClick={() => handleNavigation(`/?collection=${collection.id}`)}
                selected={location.search.includes(`collection=${collection.id}`)}
              >
                <ListItemIcon sx={{ minWidth: 40 }}>
                  <FolderIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={collection.name} />
              </ListItemButton>
            </ListItem>
          ))}
          <ListItem disablePadding>
            <ListItemButton onClick={() => handleNavigation("/collections")}>
              <ListItemIcon sx={{ minWidth: 40 }}>
                <AddIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText primary="Manage Collections" />
            </ListItemButton>
          </ListItem>
        </List>
      </Box>
    </Drawer>
  );
};
