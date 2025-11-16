import React from "react";
import { renderWithRouter, screen, waitFor } from "../../test-utils";
import Home from "../Home";
import userEvent from "@testing-library/user-event";
import api from "../../services/api";

// Mock the API
jest.mock("../../services/api");

describe("Home Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders welcome message when no links exist", async () => {
    // Mock API to return empty result
    api.listLinks.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0
    });

    renderWithRouter(<Home />);

    await waitFor(() => {
      expect(screen.getByText("Welcome to LinkLift")).toBeInTheDocument();
    });

    expect(screen.getByText(/simple and efficient way/i)).toBeInTheDocument();
  });

  test("renders Add New Link button when no links exist", async () => {
    // Mock API to return empty result
    api.listLinks.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0
    });

    renderWithRouter(<Home />);

    await waitFor(() => {
      expect(screen.getByText("Welcome to LinkLift")).toBeInTheDocument();
    });

    const addButton = screen.getByRole("link", { name: /add new link/i });
    expect(addButton).toBeInTheDocument();
    expect(addButton).toHaveAttribute("href", "/add");
  });

  test("renders LinkList when links exist", async () => {
    // Mock API to return links
    api.listLinks.mockResolvedValue({
      content: [
        {
          id: "1",
          title: "Test Link",
          url: "https://example.com",
          description: "Test description",
          extractedAt: "2023-01-01T00:00:00Z"
        }
      ],
      totalElements: 1,
      totalPages: 1
    });

    renderWithRouter(<Home />);

    await waitFor(() => {
      expect(screen.getByText("My Links")).toBeInTheDocument();
    });

    // Should not show welcome message when links exist
    expect(screen.queryByText("Welcome to LinkLift")).not.toBeInTheDocument();
  });

  test("navigates to add page when button is clicked", async () => {
    const user = userEvent.setup();

    // Mock API to return empty result
    api.listLinks.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0
    });

    renderWithRouter(<Home />);

    await waitFor(() => {
      expect(screen.getByText("Welcome to LinkLift")).toBeInTheDocument();
    });

    const addButton = screen.getByRole("link", { name: /add new link/i });
    await user.click(addButton);

    // Since we're using MemoryRouter in tests, we can verify the href attribute
    expect(addButton).toHaveAttribute("href", "/add");
  });
});
