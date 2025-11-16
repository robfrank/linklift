import React, { useState } from "react";
import { AppBar, Toolbar, Typography, Button, Box, Menu, MenuItem, Avatar, IconButton } from "@mui/material";
import { Link as RouterLink, useLocation, useNavigate } from "react-router-dom";
import { Home as HomeIcon, Add as AddIcon, AccountCircle, Login as LoginIcon, PersonAdd as PersonAddIcon } from "@mui/icons-material";
import { useAuth } from "../contexts/AuthContext";

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout, isAuthenticated } = useAuth();
  const [anchorEl, setAnchorEl] = useState(null);

  const isActive = (path) => location.pathname === path;

  const handleMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = async () => {
    try {
      await logout();
      navigate("/login");
    } catch (error) {
      console.error("Logout failed:", error);
    }
    handleClose();
  };

  const getUserDisplayName = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user?.username || "User";
  };

  return (
    <AppBar position="static">
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          LinkLift
        </Typography>

        {isAuthenticated ? (
          <>
            {/* Authenticated navigation */}
            <Box sx={{ display: "flex", alignItems: "center" }}>
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
                  mr: 2,
                  backgroundColor: isActive("/add") ? "rgba(255, 255, 255, 0.1)" : "transparent"
                }}
              >
                Add Link
              </Button>

              {/* User menu */}
              <IconButton
                size="large"
                aria-label="account of current user"
                aria-controls="menu-appbar"
                aria-haspopup="true"
                onClick={handleMenu}
                color="inherit"
              >
                <Avatar sx={{ width: 32, height: 32 }}>{getUserDisplayName().charAt(0).toUpperCase()}</Avatar>
              </IconButton>
              <Menu
                id="menu-appbar"
                anchorEl={anchorEl}
                anchorOrigin={{
                  vertical: "bottom",
                  horizontal: "right"
                }}
                keepMounted
                transformOrigin={{
                  vertical: "top",
                  horizontal: "right"
                }}
                open={Boolean(anchorEl)}
                onClose={handleClose}
              >
                <MenuItem disabled>
                  <Typography variant="body2" color="text.secondary">
                    {getUserDisplayName()}
                  </Typography>
                </MenuItem>
                <MenuItem disabled>
                  <Typography variant="body2" color="text.secondary">
                    {user?.email}
                  </Typography>
                </MenuItem>
                <MenuItem onClick={handleLogout}>Logout</MenuItem>
              </Menu>
            </Box>
          </>
        ) : (
          <>
            {/* Unauthenticated navigation */}
            <Box>
              <Button
                color="inherit"
                component={RouterLink}
                to="/login"
                startIcon={<LoginIcon />}
                sx={{
                  mr: 1,
                  backgroundColor: isActive("/login") ? "rgba(255, 255, 255, 0.1)" : "transparent"
                }}
              >
                Login
              </Button>
              <Button
                color="inherit"
                component={RouterLink}
                to="/register"
                startIcon={<PersonAddIcon />}
                sx={{
                  backgroundColor: isActive("/register") ? "rgba(255, 255, 255, 0.1)" : "transparent"
                }}
              >
                Register
              </Button>
            </Box>
          </>
        )}
      </Toolbar>
    </AppBar>
  );
};

export default Header;
