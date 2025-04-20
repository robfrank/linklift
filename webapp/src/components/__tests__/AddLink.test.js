import React from "react";
import { renderWithRouter, screen, waitFor } from "../../test-utils";
import userEvent from "@testing-library/user-event";
import AddLink from "../AddLink";
import api from "../../services/api";

// Mock the API module
jest.mock("../../services/api");

// Mock react-router-dom's useNavigate hook
const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => {
  const actual = jest.requireActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate
  };
});

describe("AddLink Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("renders form elements", () => {
    renderWithRouter(<AddLink />);

    expect(screen.getByText("Add New Link")).toBeInTheDocument();
    expect(screen.getByLabelText(/url/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/title/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /add link/i })).toBeInTheDocument();
  });

  test("validates form fields on submit", async () => {
    const user = userEvent.setup();
    renderWithRouter(<AddLink />);

    // Submit empty form
    const submitButton = screen.getByRole("button", { name: /add link/i });
    await user.click(submitButton);

    // Check validation errors appear
    expect(screen.getByText("URL is required")).toBeInTheDocument();
    expect(screen.getByText("Title is required")).toBeInTheDocument();
    expect(screen.getByText("Description is required")).toBeInTheDocument();

    // API should not be called
    expect(api.createLink).not.toHaveBeenCalled();
  });

  test("clears error when user types in a field", async () => {
    const user = userEvent.setup();
    renderWithRouter(<AddLink />);

    // Submit empty form to trigger validation errors
    const submitButton = screen.getByRole("button", { name: /add link/i });
    await user.click(submitButton);

    // Verify errors are displayed
    expect(screen.getByText("URL is required")).toBeInTheDocument();

    // Type in the URL field
    const urlInput = screen.getByLabelText(/url/i);
    await user.type(urlInput, "https://example.com");

    // Error should be cleared
    expect(screen.queryByText("URL is required")).not.toBeInTheDocument();
  });

  test("submits form successfully", async () => {
    const user = userEvent.setup();
    renderWithRouter(<AddLink />);

    // Fill out the form
    await user.type(screen.getByLabelText(/url/i), "https://example.com");
    await user.type(screen.getByLabelText(/title/i), "Example Website");
    await user.type(screen.getByLabelText(/description/i), "This is an example website");

    // Submit form
    const submitButton = screen.getByRole("button", { name: /add link/i });
    await user.click(submitButton);

    // API should be called with form data
    expect(api.createLink).toHaveBeenCalledWith({
      url: "https://example.com",
      title: "Example Website",
      description: "This is an example website"
    });

    // Success message should appear
    await waitFor(() => {
      expect(screen.getByText("Link added successfully!")).toBeInTheDocument();
    });

    // Form should be reset
    expect(screen.getByLabelText(/url/i)).toHaveValue("");
    expect(screen.getByLabelText(/title/i)).toHaveValue("");
    expect(screen.getByLabelText(/description/i)).toHaveValue("");

    // Should navigate to home after delay
    await waitFor(
      () => {
        expect(mockNavigate).toHaveBeenCalledWith("/");
      },
      { timeout: 3000 }
    );
  });

  test("handles API error - duplicate URL", async () => {
    const user = userEvent.setup();

    // Mock API to reject with conflict error
    api.createLink.mockRejectedValueOnce({
      response: { status: 409 }
    });

    renderWithRouter(<AddLink />);

    // Fill out the form
    await user.type(screen.getByLabelText(/url/i), "https://example.com");
    await user.type(screen.getByLabelText(/title/i), "Example Website");
    await user.type(screen.getByLabelText(/description/i), "This is an example website");

    // Submit form
    const submitButton = screen.getByRole("button", { name: /add link/i });
    await user.click(submitButton);

    // Error message should appear
    await waitFor(() => {
      expect(screen.getByText("This URL already exists")).toBeInTheDocument();
    });

    // Should not navigate away
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  test("handles API error - server error", async () => {
    const user = userEvent.setup();

    // Mock API to reject with server error and message
    api.createLink.mockRejectedValueOnce({
      response: {
        status: 500,
        data: { message: "Server error occurred" }
      }
    });

    renderWithRouter(<AddLink />);

    // Fill out the form
    await user.type(screen.getByLabelText(/url/i), "https://example.com");
    await user.type(screen.getByLabelText(/title/i), "Example Website");
    await user.type(screen.getByLabelText(/description/i), "This is an example website");

    // Submit form
    const submitButton = screen.getByRole("button", { name: /add link/i });
    await user.click(submitButton);

    // Error message should appear
    await waitFor(() => {
      expect(screen.getByText("Server error occurred")).toBeInTheDocument();
    });
  });

  test("handles generic API error", async () => {
    const user = userEvent.setup();

    // Mock API to reject with network error
    api.createLink.mockRejectedValueOnce(new Error("Network error"));

    renderWithRouter(<AddLink />);

    // Fill out the form
    await user.type(screen.getByLabelText(/url/i), "https://example.com");
    await user.type(screen.getByLabelText(/title/i), "Example Website");
    await user.type(screen.getByLabelText(/description/i), "This is an example website");

    // Submit form
    const submitButton = screen.getByRole("button", { name: /add link/i });
    await user.click(submitButton);

    // Generic error message should appear
    await waitFor(() => {
      expect(screen.getByText("Failed to add link")).toBeInTheDocument();
    });
  });

  test("navigates to home when cancel button is clicked", async () => {
    const user = userEvent.setup();
    renderWithRouter(<AddLink />);

    const cancelButton = screen.getByRole("button", { name: /cancel/i });
    await user.click(cancelButton);

    expect(mockNavigate).toHaveBeenCalledWith("/");
  });
});
