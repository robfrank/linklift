import React from "react";
import { Routes, Route } from "react-router-dom";
import { ThemeProvider, createTheme } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";

import Header from "./components/Header";
import Home from "./components/Home";
import AddLink from "./components/AddLink";
import LinkList from "./components/LinkList";
import NotFound from "./components/NotFound";

const theme = createTheme({
  palette: {
    primary: {
      main: "#1976d2"
    },
    secondary: {
      main: "#dc004e"
    }
  }
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Header />
      <main>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/add" element={<AddLink />} />
          <Route path="/links" element={<LinkList />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </ThemeProvider>
  );
}

export default App;
