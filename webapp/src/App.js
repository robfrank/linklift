import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";

import { AuthProvider } from "./contexts/AuthContext";
import Header from "./components/Header";
import Home from "./components/Home";
import AddLink from "./components/AddLink";
import Login from "./components/Login";
import Register from "./components/Register";
import ProtectedRoute from "./components/ProtectedRoute";
import NotFound from "./components/NotFound";

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
        <AuthProvider>
            <ThemeProvider theme={theme}>
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

                        {/* Redirect root to login if not authenticated, otherwise to home */}
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </main>
            </ThemeProvider>
        </AuthProvider>
    );
}

export default App;
