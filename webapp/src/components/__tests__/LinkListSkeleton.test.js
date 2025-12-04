import React from "react";
import { render } from "@testing-library/react";
import LinkListSkeleton from "../Skeletons/LinkListSkeleton";

describe("LinkListSkeleton", () => {
  it("renders correct number of skeleton items", () => {
    const { container } = render(<LinkListSkeleton count={5} />);
    // Each item contains a Card
    const items = container.querySelectorAll(".MuiCard-root");
    expect(items.length).toBe(5);
  });

  it("renders default number of skeleton items", () => {
    const { container } = render(<LinkListSkeleton />);
    const items = container.querySelectorAll(".MuiCard-root");
    expect(items.length).toBe(3);
  });
});
