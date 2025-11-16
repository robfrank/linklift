import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter, MemoryRouter, Routes, Route } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";

const theme = createTheme({
  palette: {
    primary: { main: "#1976d2" },
    secondary: { main: "#dc004e" }
  }
});

// Wrapper with basic theme provider
export function renderWithTheme(ui, options) {
  return render(ui, {
    wrapper: ({ children }) => <ThemeProvider theme={theme}>{children}</ThemeProvider>,
    ...options
  });
}

// Wrapper with router and theme
export function renderWithRouter(ui, { route = "/", path = "/" } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider theme={theme}>
        <Routes>
          <Route path={path} element={ui} />
        </Routes>
      </ThemeProvider>
    </MemoryRouter>
  );
}

// Wrapper for testing the entire app
export function renderApp(ui, { route = "/" } = {}) {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <ThemeProvider theme={theme}>{ui}</ThemeProvider>
    </MemoryRouter>
  );
}

// Export specific functions rather than all to avoid conflicts
export { screen, waitFor };
