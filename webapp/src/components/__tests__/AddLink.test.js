import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import AddLink from "../AddLink";
import api from "../../services/api";

// Mock the API
jest.mock("../../services/api");

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
            <ThemeProvider theme={theme}>{component}</ThemeProvider>
        </BrowserRouter>
    );
};

describe("AddLink Component", () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("renders form elements correctly", () => {
        renderWithProviders(<AddLink />);

        expect(screen.getByRole("heading", { name: "Add New Link" })).toBeInTheDocument();
        expect(screen.getByLabelText("URL")).toBeInTheDocument();
        expect(screen.getByLabelText("Title")).toBeInTheDocument();
        expect(screen.getByLabelText("Description")).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Add Link" })).toBeInTheDocument();
        expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
    });

    test("validates required fields", async () => {
        const user = userEvent.setup();
        renderWithProviders(<AddLink />);

        const submitButton = screen.getByRole("button", { name: "Add Link" });
        await user.click(submitButton);

        await waitFor(() => {
            expect(screen.getByText("URL is required")).toBeInTheDocument();
            expect(screen.getByText("Title is required")).toBeInTheDocument();
            expect(screen.getByText("Description is required")).toBeInTheDocument();
        });
    });

    test("validates URL format", async () => {
        const user = userEvent.setup();
        renderWithProviders(<AddLink />);

        const urlInput = screen.getByLabelText("URL");
        const titleInput = screen.getByLabelText("Title");
        const descriptionInput = screen.getByLabelText("Description");

        await user.type(urlInput, "invalid-url");
        await user.type(titleInput, "Test Title");
        await user.type(descriptionInput, "Test Description");

        // Submit the form by pressing Enter or clicking submit
        const form = urlInput.closest("form");
        fireEvent.submit(form);

        // Wait for validation to complete and check for the error
        await waitFor(
            () => {
                expect(screen.getByText("Please enter a valid URL")).toBeInTheDocument();
            },
            { timeout: 3000 }
        );

        // Verify that the URL input is marked as invalid
        expect(urlInput).toHaveAttribute("aria-invalid", "true");

        // Make sure API was not called due to validation error
        expect(api.createLink).not.toHaveBeenCalled();
    });

    test("submits form successfully", async () => {
        const user = userEvent.setup();
        api.createLink.mockResolvedValue({ id: "1" });

        renderWithProviders(<AddLink />);

        const urlInput = screen.getByLabelText("URL");
        const titleInput = screen.getByLabelText("Title");
        const descriptionInput = screen.getByLabelText("Description");
        const submitButton = screen.getByRole("button", { name: "Add Link" });

        await user.type(urlInput, "https://example.com");
        await user.type(titleInput, "Test Title");
        await user.type(descriptionInput, "Test Description");
        await user.click(submitButton);

        await waitFor(() => {
            expect(api.createLink).toHaveBeenCalledWith({
                url: "https://example.com",
                title: "Test Title",
                description: "Test Description"
            });
        });

        await waitFor(() => {
            expect(screen.getByText("Link added successfully!")).toBeInTheDocument();
        });
    });

    test("handles API errors", async () => {
        const user = userEvent.setup();
        api.createLink.mockRejectedValue({
            response: { status: 409 }
        });

        renderWithProviders(<AddLink />);

        const urlInput = screen.getByLabelText("URL");
        const titleInput = screen.getByLabelText("Title");
        const descriptionInput = screen.getByLabelText("Description");
        const submitButton = screen.getByRole("button", { name: "Add Link" });

        await user.type(urlInput, "https://example.com");
        await user.type(titleInput, "Test Title");
        await user.type(descriptionInput, "Test Description");
        await user.click(submitButton);

        await waitFor(() => {
            expect(screen.getByText("This URL already exists")).toBeInTheDocument();
        });
    });
});
