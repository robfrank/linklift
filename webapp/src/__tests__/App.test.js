import React from "react";
import { renderApp, screen, waitFor } from "../test-utils";
import App from "../App";
import api from "../infrastructure/api/axios-instance";
import * as AuthContext from "../contexts/AuthContext";

// Mock the API
jest.mock("../infrastructure/api/axios-instance");

// Mock the AuthContext
jest.mock("../contexts/AuthContext", () => ({
  ...jest.requireActual("../contexts/AuthContext"),
  useAuth: jest.fn()
}));

describe("App Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Differentiate responses by URL: links endpoint returns paginated data,
    // collections endpoint returns an array.
    api.get = jest.fn().mockImplementation((url) => {
      if (url.includes("/collections")) {
        return Promise.resolve({ data: { data: [] } });
      }
      return Promise.resolve({
        data: {
          data: {
            content: [],
            totalElements: 0,
            totalPages: 0
          }
        }
      });
    });

    api.post = jest.fn();
    api.delete = jest.fn();

    // Default to authenticated user
    AuthContext.useAuth.mockReturnValue({
      user: { userId: 1, username: "testuser", email: "test@example.com" },
      login: jest.fn(),
      register: jest.fn(),
      logout: jest.fn(),
      refreshToken: jest.fn(),
      isAuthenticated: true,
      loading: false
    });
  });

  test("renders header", () => {
    renderApp(<App />);
    expect(screen.getByText("LinkLift")).toBeInTheDocument();
  });

  test("renders home page by default", async () => {
    renderApp(<App />);

    await waitFor(() => {
      expect(screen.getByText("My Links")).toBeInTheDocument();
    });
  });

  test("renders add link page on /add route", () => {
    renderApp(<App />, { route: "/add" });
    expect(screen.getByText("Add New Link")).toBeInTheDocument();
    expect(screen.getByLabelText(/url/i)).toBeInTheDocument();
  });

  test("renders not found page for unknown routes", () => {
    renderApp(<App />, { route: "/unknown-route" });
    expect(screen.getByText("404")).toBeInTheDocument();
    expect(screen.getByText("Page Not Found")).toBeInTheDocument();
  });
});
