import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";

import { AuthProvider } from "./contexts/AuthContext";
import { SnackbarProvider } from "./contexts/SnackbarContext";
import Header from "./components/Header";
import Home from "./components/Home";
import AddLink from "./components/AddLink";
import Login from "./components/Login";
import Register from "./components/Register";
import ProtectedRoute from "./components/ProtectedRoute";
import NotFound from "./components/NotFound";
import CollectionList from "./components/Collections/CollectionList";
import CollectionDetail from "./components/Collections/CollectionDetail";

import ErrorBoundary from "./components/ErrorBoundary";

const theme = createTheme({
  palette: {
    primary: {
      main: "#1976d2"
    },
    secondary: {
      main: "#dc004e"
    }
  }
});

function App() {
  return (
    <ErrorBoundary>
      <AuthProvider>
        <ThemeProvider theme={theme}>
          <SnackbarProvider>
            <CssBaseline />
            <Header />
            <main>
              <Routes>
                {/* Public routes */}
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                {/* Protected routes */}
                <Route
                  path="/"
                  element={
                    <ProtectedRoute>
                      <Home />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/add"
                  element={
                    <ProtectedRoute>
                      <AddLink />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/collections"
                  element={
                    <ProtectedRoute>
                      <CollectionList />
                    </ProtectedRoute>
                  }
                />
                <Route
                  path="/collections/:id"
                  element={
                    <ProtectedRoute>
                      <CollectionDetail />
                    </ProtectedRoute>
                  }
                />

                {/* Redirect root to login if not authenticated, otherwise to home */}
                <Route path="*" element={<NotFound />} />
              </Routes>
            </main>
          </SnackbarProvider>
        </ThemeProvider>
      </AuthProvider>
    </ErrorBoundary>
  );
}

export default App;
