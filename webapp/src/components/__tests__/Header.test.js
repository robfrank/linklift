import React from "react";
import { renderWithRouter, screen } from "../../test-utils";
import Header from "../Header";
import * as AuthContext from "../../contexts/AuthContext";

// Mock the AuthContext
jest.mock("../../contexts/AuthContext", () => ({
  ...jest.requireActual("../../contexts/AuthContext"),
  useAuth: jest.fn()
}));

describe("Header Component", () => {
  beforeEach(() => {
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

  test("renders the app title", () => {
    renderWithRouter(<Header />);
    expect(screen.getByText("LinkLift")).toBeInTheDocument();
  });

  test("renders navigation links", () => {
    renderWithRouter(<Header />);

    const homeLink = screen.getByRole("link", { name: /links/i });
    const addLink = screen.getByRole("link", { name: /add link/i });

    expect(homeLink).toBeInTheDocument();
    expect(addLink).toBeInTheDocument();
  });

  test("home link navigates to root path", () => {
    renderWithRouter(<Header />);
    const homeLink = screen.getByRole("link", { name: /links/i });
    expect(homeLink).toHaveAttribute("href", "/");
  });

  test("add link button navigates to add path", () => {
    renderWithRouter(<Header />);
    const addLink = screen.getByRole("link", { name: /add link/i });
    expect(addLink).toHaveAttribute("href", "/add");
  });
});
