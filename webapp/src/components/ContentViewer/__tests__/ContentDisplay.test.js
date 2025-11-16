import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { ContentDisplay } from "../ContentDisplay";

describe("ContentDisplay", () => {
  const mockContent = {
    htmlContent: "<html><body><h1>Test HTML</h1></body></html>",
    textContent: "Test text content"
  };

  const mockOnViewModeChange = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders text view by default", () => {
    render(<ContentDisplay content={mockContent} viewMode="text" onViewModeChange={mockOnViewModeChange} />);

    expect(screen.getByText("Test text content")).toBeInTheDocument();
  });

  it("switches to HTML view when button is clicked", () => {
    render(<ContentDisplay content={mockContent} viewMode="text" onViewModeChange={mockOnViewModeChange} />);

    const htmlButton = screen.getByRole("button", { name: /HTML Preview/i });
    fireEvent.click(htmlButton);

    expect(mockOnViewModeChange).toHaveBeenCalledWith("html");
  });

  it("renders iframe in HTML mode", () => {
    render(<ContentDisplay content={mockContent} viewMode="html" onViewModeChange={mockOnViewModeChange} />);

    const iframe = screen.getByTitle("HTML Preview");
    expect(iframe).toBeInTheDocument();
    expect(iframe).toHaveAttribute("sandbox", "allow-same-origin");
  });

  it("displays active state on selected view mode button", () => {
    render(<ContentDisplay content={mockContent} viewMode="text" onViewModeChange={mockOnViewModeChange} />);

    const textButton = screen.getByRole("button", { name: /Text View/i });
    expect(textButton).toHaveClass("active");
  });

  it("handles missing text content gracefully", () => {
    const contentWithoutText = {
      ...mockContent,
      textContent: null
    };

    render(<ContentDisplay content={contentWithoutText} viewMode="text" onViewModeChange={mockOnViewModeChange} />);

    expect(screen.getByText("No text content available")).toBeInTheDocument();
  });
});
