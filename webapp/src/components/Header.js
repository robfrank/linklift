import React from "react";
import { AppBar, Toolbar, Typography, Button, Box } from "@mui/material";
import { Link as RouterLink, useLocation } from "react-router-dom";
import { Home as HomeIcon, Add as AddIcon } from "@mui/icons-material";

const Header = () => {
  const location = useLocation();

  const isActive = (path) => location.pathname === path;

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          LinkLift
        </Typography>
        <Box>
          <Button
            color="inherit"
            component={RouterLink}
            to="/"
            startIcon={<HomeIcon />}
            sx={{
              mr: 1,
              backgroundColor: isActive("/") ? "rgba(255, 255, 255, 0.1)" : "transparent"
            }}
          >
            Links
          </Button>
          <Button
            color="inherit"
            component={RouterLink}
            to="/add"
            startIcon={<AddIcon />}
            sx={{
              backgroundColor: isActive("/add") ? "rgba(255, 255, 255, 0.1)" : "transparent"
            }}
          >
            Add Link
          </Button>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Header;
