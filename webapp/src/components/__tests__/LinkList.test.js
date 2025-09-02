import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import LinkList from "../LinkList";
import api from "../../services/api";

// Mock the API
jest.mock("../../services/api");

const theme = createTheme();

const renderWithProviders = (component) => {
  return render(
    <BrowserRouter>
      <ThemeProvider theme={theme}>{component}</ThemeProvider>
    </BrowserRouter>
  );
};

describe("LinkList Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders loading state initially", () => {
    // Mock API to return a promise that doesn't resolve immediately
    api.listLinks.mockImplementation(() => new Promise(() => {}));

    renderWithProviders(<LinkList />);

    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  test("renders links when data is loaded", async () => {
    const mockResponse = {
      content: [
        {
          id: "1",
          title: "Test Link",
          url: "https://example.com",
          description: "A test link",
          extractedAt: "2023-01-01T00:00:00Z"
        }
      ],
      totalPages: 1,
      totalElements: 1
    };

    api.listLinks.mockResolvedValue(mockResponse);

    renderWithProviders(<LinkList />);

    await waitFor(() => {
      expect(screen.getByText("Test Link")).toBeInTheDocument();
      expect(screen.getByText("A test link")).toBeInTheDocument();
      expect(screen.getByText("1 link total")).toBeInTheDocument();
    });
  });

  test("renders empty state when no links", async () => {
    const mockResponse = {
      content: [],
      totalPages: 0,
      totalElements: 0
    };

    api.listLinks.mockResolvedValue(mockResponse);

    renderWithProviders(<LinkList />);

    await waitFor(() => {
      expect(screen.getByText("No links found")).toBeInTheDocument();
      expect(screen.getByText("Start by adding your first link!")).toBeInTheDocument();
    });
  });

  test("renders error state when API fails", async () => {
    api.listLinks.mockRejectedValue(new Error("API Error"));

    renderWithProviders(<LinkList />);

    await waitFor(() => {
      expect(screen.getByText("Failed to load links. Please try again.")).toBeInTheDocument();
    });
  });
});
