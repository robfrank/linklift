import React from "react";
import { renderWithRouter, screen } from "../../test-utils";
import Header from "../Header";

describe("Header Component", () => {
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
