import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CollectionList from "../CollectionList";
import { SnackbarProvider } from "../../../contexts/SnackbarContext";
import api from "../../../services/api";

// Mock the API
jest.mock("../../../services/api");

// Mock useNavigate
const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate
}));

const theme = createTheme();

const renderWithProviders = (component) => {
  return render(
    <BrowserRouter>
      <ThemeProvider theme={theme}>
        <SnackbarProvider>{component}</SnackbarProvider>
      </ThemeProvider>
    </BrowserRouter>
  );
};

describe("CollectionList Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Setup default mocks
    api.listCollections = jest.fn();
    api.createCollection = jest.fn();
    api.deleteCollection = jest.fn();
  });

  test("renders loading state initially", () => {
    api.listCollections.mockImplementation(() => new Promise(() => {}));
    renderWithProviders(<CollectionList />);
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  test("renders collections when data is loaded", async () => {
    const mockCollections = [{ id: "1", name: "Test Collection", description: "Test Description" }];
    api.listCollections.mockResolvedValue({ data: mockCollections });

    renderWithProviders(<CollectionList />);

    await waitFor(() => {
      expect(screen.getByText("Test Collection")).toBeInTheDocument();
      expect(screen.getByText("Test Description")).toBeInTheDocument();
    });
  });

  test("renders empty state when no collections", async () => {
    api.listCollections.mockResolvedValue({ data: [] });

    renderWithProviders(<CollectionList />);

    await waitFor(() => {
      expect(screen.getByText("No collections found")).toBeInTheDocument();
    });
  });

  test("opens create dialog", async () => {
    const user = userEvent.setup();
    api.listCollections.mockResolvedValue({ data: [] });

    renderWithProviders(<CollectionList />);

    await waitFor(() => {
      expect(screen.getByText("My Collections")).toBeInTheDocument();
    });

    const createButton = screen.getByRole("button", { name: "New Collection" });
    await user.click(createButton);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Create New Collection")).toBeInTheDocument();
  });
});
