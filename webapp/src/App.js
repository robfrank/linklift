import React from "react";
import { Routes, Route, Outlet } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";

import { AuthProvider } from "./contexts/AuthContext";
import { SnackbarProvider } from "./contexts/SnackbarContext";
import { MainLayout } from "./infrastructure/ui/layouts/MainLayout";
import Home from "./infrastructure/ui/pages/HomePage";
import AddLink from "./infrastructure/ui/pages/AddLinkPage";
import Login from "./components/Login";
import Register from "./components/Register";
import ProtectedRoute from "./components/ProtectedRoute";
import NotFound from "./components/NotFound";
import CollectionList from "./components/Collections/CollectionList";
import CollectionDetail from "./components/Collections/CollectionDetail";
import SearchPage from "./infrastructure/ui/pages/SearchPage";
import AdminPage from "./infrastructure/ui/pages/AdminPage";
import GraphPage from "./infrastructure/ui/pages/GraphPage";
import { AskPage } from "./infrastructure/ui/pages/AskPage";

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
            <main>
              <Routes>
                {/* Public routes */}
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                {/* Protected routes wrapped in MainLayout */}
                <Route
                  element={
                    <ProtectedRoute>
                      <MainLayout>
                        <Outlet />
                      </MainLayout>
                    </ProtectedRoute>
                  }
                >
                  <Route path="/" element={<Home />} />
                  <Route path="/add" element={<AddLink />} />
                  <Route path="/collections" element={<CollectionList />} />
                  <Route path="/collections/:id" element={<CollectionDetail />} />
                  <Route path="/search" element={<SearchPage />} />
                  <Route path="/graph" element={<GraphPage />} />
                  <Route path="/ask" element={<AskPage />} />
                  <Route path="/admin" element={<AdminPage />} />
                </Route>

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
