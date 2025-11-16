import React from "react";
import { renderWithRouter, screen } from "../../test-utils";
import NotFound from "../NotFound";
import userEvent from "@testing-library/user-event";

describe("NotFound Component", () => {
  test("renders 404 message", () => {
    renderWithRouter(<NotFound />);

    expect(screen.getByText("404")).toBeInTheDocument();
    expect(screen.getByText("Page Not Found")).toBeInTheDocument();
  });

  test("renders error description", () => {
    renderWithRouter(<NotFound />);

    expect(screen.getByText("The page you are looking for does not exist.")).toBeInTheDocument();
  });

  test("renders return to home button", () => {
    renderWithRouter(<NotFound />);

    const homeButton = screen.getByRole("link", { name: /return to home/i });
    expect(homeButton).toBeInTheDocument();
    expect(homeButton).toHaveAttribute("href", "/");
  });

  test("navigates to home page when button is clicked", async () => {
    const user = userEvent.setup();
    renderWithRouter(<NotFound />);

    const homeButton = screen.getByRole("link", { name: /return to home/i });
    await user.click(homeButton);

    // Since we're using MemoryRouter in tests, verify the href attribute
    expect(homeButton).toHaveAttribute("href", "/");
  });
});
