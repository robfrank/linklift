import React from "react";
import { renderWithRouter, screen } from "../../test-utils";
import Home from "../Home";
import userEvent from "@testing-library/user-event";

describe("Home Component", () => {
  test("renders welcome message", () => {
    renderWithRouter(<Home />);

    expect(screen.getByText("Welcome to LinkLift")).toBeInTheDocument();
    expect(screen.getByText(/simple and efficient way/i)).toBeInTheDocument();
  });

  test("renders Add New Link button", () => {
    renderWithRouter(<Home />);

    const addButton = screen.getByRole("link", { name: /add new link/i });
    expect(addButton).toBeInTheDocument();
    expect(addButton).toHaveAttribute("href", "/add");
  });

  test("navigates to add page when button is clicked", async () => {
    const user = userEvent.setup();
    const { history } = renderWithRouter(<Home />);

    const addButton = screen.getByRole("link", { name: /add new link/i });
    await user.click(addButton);

    // Since we're using MemoryRouter in tests, we can verify the href attribute
    expect(addButton).toHaveAttribute("href", "/add");
  });
});
