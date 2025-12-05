import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CollectionList from "../CollectionList";
import { SnackbarProvider } from "../../../contexts/SnackbarContext";
import api from "../../../infrastructure/api/axios-instance";

// Mock the API
jest.mock("../../../infrastructure/api/axios-instance");

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
    // Setup default mocks for axios instance
    api.get = jest.fn();
    api.post = jest.fn();
    api.delete = jest.fn();
  });

  test("renders loading state initially", () => {
    api.get.mockImplementation(() => new Promise(() => {}));
    renderWithProviders(<CollectionList />);
    expect(screen.getByRole("progressbar")).toBeInTheDocument();
  });

  test("renders collections when data is loaded", async () => {
    const mockCollections = [{ id: "1", name: "Test Collection", description: "Test Description" }];
    // The component expects data.data.data
    api.get.mockResolvedValue({ data: { data: mockCollections } });

    renderWithProviders(<CollectionList />);

    await waitFor(() => {
      expect(screen.getByText("Test Collection")).toBeInTheDocument();
      expect(screen.getByText("Test Description")).toBeInTheDocument();
    });
  });

  test("renders empty state when no collections", async () => {
    api.get.mockResolvedValue({ data: { data: [] } });

    renderWithProviders(<CollectionList />);

    await waitFor(() => {
      expect(screen.getByText("No collections found")).toBeInTheDocument();
    });
  });

  test("opens create dialog", async () => {
    const user = userEvent.setup();
    api.get.mockResolvedValue({ data: { data: [] } });

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
