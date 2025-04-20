import React from "react";
import { renderApp, screen } from "../test-utils";
import App from "../App";

describe("App Component", () => {
  test("renders header", () => {
    renderApp(<App />);
    expect(screen.getByText("LinkLift")).toBeInTheDocument();
  });

  test("renders home page by default", () => {
    renderApp(<App />);
    expect(screen.getByText("Welcome to LinkLift")).toBeInTheDocument();
    expect(screen.getByText(/simple and efficient/i)).toBeInTheDocument();
  });

  test("renders add link page on /add route", () => {
    renderApp(<App />, { route: "/add" });
    expect(screen.getByText("Add New Link")).toBeInTheDocument();
    expect(screen.getByLabelText(/url/i)).toBeInTheDocument();
  });

  test("renders not found page for unknown routes", () => {
    renderApp(<App />, { route: "/unknown-route" });
    expect(screen.getByText("404")).toBeInTheDocument();
    expect(screen.getByText("Page Not Found")).toBeInTheDocument();
  });
});
