import React, { createContext, useContext, useState, useEffect } from "react";
import axios from "axios";

const AuthContext = createContext();

export function useAuth() {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error("useAuth must be used within an AuthProvider");
    }
    return context;
}

export function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Configure axios defaults
    useEffect(() => {
        const token = localStorage.getItem("accessToken");
        if (token) {
            axios.defaults.headers.common["Authorization"] = `Bearer ${token}`;
        }
    }, []);

    // Check if user is authenticated on mount
    useEffect(() => {
        const checkAuthStatus = async () => {
            const token = localStorage.getItem("accessToken");
            if (token) {
                try {
                    // Set the token in axios headers
                    axios.defaults.headers.common["Authorization"] = `Bearer ${token}`;

                    // Try to fetch user data or validate token
                    // For now, we'll just trust the stored user data
                    const userData = localStorage.getItem("user");
                    if (userData) {
                        setUser(JSON.parse(userData));
                    }
                } catch (error) {
                    console.error("Token validation failed:", error);
                    localStorage.removeItem("accessToken");
                    localStorage.removeItem("refreshToken");
                    localStorage.removeItem("user");
                    delete axios.defaults.headers.common["Authorization"];
                }
            }
            setLoading(false);
        };

        checkAuthStatus();
    }, []);

    const login = async ({ loginIdentifier, password, rememberMe = false }) => {
        try {
            const response = await axios.post("/api/v1/auth/login", {
                loginIdentifier,
                password,
                rememberMe
            });

            const { userId, username, email, firstName, lastName, accessToken, refreshToken } = response.data;

            // Store tokens
            localStorage.setItem("accessToken", accessToken);
            localStorage.setItem("refreshToken", refreshToken);

            // Store user data
            const userData = { userId, username, email, firstName, lastName };
            localStorage.setItem("user", JSON.stringify(userData));
            setUser(userData);

            // Set default authorization header
            axios.defaults.headers.common["Authorization"] = `Bearer ${accessToken}`;

            return userData;
        } catch (error) {
            const message = error.response?.data?.message || "Login failed";
            throw new Error(message);
        }
    };

    const register = async ({ username, email, password, firstName, lastName }) => {
        try {
            const response = await axios.post("/api/v1/auth/register", {
                username,
                email,
                password,
                firstName,
                lastName
            });

            return response.data;
        } catch (error) {
            const message = error.response?.data?.message || "Registration failed";
            throw new Error(message);
        }
    };

    const logout = async () => {
        try {
            // Call logout endpoint if available
            await axios.post("/api/v1/auth/logout");
        } catch (error) {
            console.error("Logout API call failed:", error);
        } finally {
            // Clear local storage and state regardless of API call result
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            localStorage.removeItem("user");
            delete axios.defaults.headers.common["Authorization"];
            setUser(null);
        }
    };

    const refreshToken = async () => {
        try {
            const refreshToken = localStorage.getItem("refreshToken");
            if (!refreshToken) {
                throw new Error("No refresh token available");
            }

            const response = await axios.post("/api/v1/auth/refresh", {
                refreshToken
            });

            const { userId, username, email, firstName, lastName, accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;

            // Update tokens
            localStorage.setItem("accessToken", newAccessToken);
            localStorage.setItem("refreshToken", newRefreshToken);

            // Update user data
            const userData = { userId, username, email, firstName, lastName };
            localStorage.setItem("user", JSON.stringify(userData));
            setUser(userData);

            // Update axios default header
            axios.defaults.headers.common["Authorization"] = `Bearer ${newAccessToken}`;

            return userData;
        } catch (error) {
            console.error("Token refresh failed:", error);
            logout();
            throw new Error("Session expired. Please login again.");
        }
    };

    // Setup axios interceptor for automatic token refresh
    useEffect(() => {
        const responseInterceptor = axios.interceptors.response.use(
            (response) => response,
            async (error) => {
                const originalRequest = error.config;

                if (error.response?.status === 401 && !originalRequest._retry) {
                    originalRequest._retry = true;

                    try {
                        await refreshToken();
                        // Retry the original request with new token
                        return axios(originalRequest);
                    } catch (refreshError) {
                        // Refresh failed, redirect to login
                        return Promise.reject(refreshError);
                    }
                }

                return Promise.reject(error);
            }
        );

        return () => {
            axios.interceptors.response.eject(responseInterceptor);
        };
    }, []);

    const value = {
        user,
        login,
        register,
        logout,
        refreshToken,
        isAuthenticated: !!user,
        loading
    };

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
