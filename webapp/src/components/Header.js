import React, { useState } from "react";
import { AppBar, Toolbar, Typography, Button, Box, Menu, MenuItem, Avatar, IconButton } from "@mui/material";
import { Link as RouterLink, useLocation, useNavigate } from "react-router-dom";
import {
  Home as HomeIcon,
  Add as AddIcon,
  AccountCircle,
  Login as LoginIcon,
  PersonAdd as PersonAddIcon,
  Folder,
  Search as SearchIcon,
  Settings
} from "@mui/icons-material";
import { useAuth } from "../contexts/AuthContext";
import { alpha, styled, InputBase } from "@mui/material";

const Search = styled("div")(({ theme }) => ({
  position: "relative",
  borderRadius: theme.shape.borderRadius,
  backgroundColor: alpha(theme.palette.common.white, 0.15),
  "&:hover": {
    backgroundColor: alpha(theme.palette.common.white, 0.25)
  },
  marginRight: theme.spacing(2),
  marginLeft: 0,
  width: "100%",
  [theme.breakpoints.up("sm")]: {
    marginLeft: theme.spacing(3),
    width: "auto"
  }
}));

const SearchIconWrapper = styled("div")(({ theme }) => ({
  padding: theme.spacing(0, 2),
  height: "100%",
  position: "absolute",
  pointerEvents: "none",
  display: "flex",
  alignItems: "center",
  justifyContent: "center"
}));

const StyledInputBase = styled(InputBase)(({ theme }) => ({
  color: "inherit",
  "& .MuiInputBase-input": {
    padding: theme.spacing(1, 1, 1, 0),
    paddingLeft: `calc(1em + ${theme.spacing(4)})`,
    transition: theme.transitions.create("width"),
    width: "100%",
    [theme.breakpoints.up("md")]: {
      width: "20ch"
    }
  }
}));

const Header = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout, isAuthenticated } = useAuth();
  const [anchorEl, setAnchorEl] = useState(null);

  const isActive = (path) => location.pathname === path;
  const [searchQuery, setSearchQuery] = useState("");

  const handleSearchSubmit = (e) => {
    if (e.key === "Enter" && searchQuery.trim()) {
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery("");
    }
  };

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
        <Typography variant="h6" component="div" sx={{ display: { xs: "none", sm: "block" }, mr: 2 }}>
          LinkLift
        </Typography>

        {isAuthenticated && (
          <Search>
            <SearchIconWrapper>
              <SearchIcon />
            </SearchIconWrapper>
            <StyledInputBase
              placeholder="Search links..."
              inputProps={{ "aria-label": "search" }}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyPress={handleSearchSubmit}
            />
          </Search>
        )}

        <Box sx={{ flexGrow: 1 }} />

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
                to="/collections"
                startIcon={<Folder />}
                sx={{
                  mr: 1,
                  backgroundColor: isActive("/collections") ? "rgba(255, 255, 255, 0.1)" : "transparent"
                }}
              >
                Collections
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
                <MenuItem divider />
                <MenuItem component={RouterLink} to="/admin" onClick={handleClose}>
                  <IconButton size="small" sx={{ mr: 1 }}>
                    <Settings fontSize="small" />
                  </IconButton>
                  Admin
                </MenuItem>
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
