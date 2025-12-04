import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SnackbarProvider, useSnackbar } from "../SnackbarContext";
import { Button } from "@mui/material";

const TestComponent = () => {
  const { showSnackbar } = useSnackbar();
  return <Button onClick={() => showSnackbar("Test Message", "success")}>Show Snackbar</Button>;
};

describe("SnackbarContext", () => {
  it("shows snackbar when showSnackbar is called", async () => {
    const user = userEvent.setup();
    render(
      <SnackbarProvider>
        <TestComponent />
      </SnackbarProvider>
    );

    const button = screen.getByText("Show Snackbar");
    await user.click(button);

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Test Message");
    expect(alert).toHaveClass("MuiAlert-filledSuccess");
  });
});
