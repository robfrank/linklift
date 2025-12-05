import React from "react";
import { Box, Typography, Button, Container, Paper } from "@mui/material";
import { ErrorOutline } from "@mui/icons-material";

class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error: error,
      errorInfo: errorInfo
    });
    console.error("Uncaught error:", error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
    window.location.href = "/";
  };

  render() {
    if (this.state.hasError) {
      return (
        <Container maxWidth="sm">
          <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
            <Paper elevation={3} sx={{ p: 4, textAlign: "center" }}>
              <ErrorOutline color="error" sx={{ fontSize: 60, mb: 2 }} />
              <Typography variant="h4" component="h1" gutterBottom>
                Something went wrong
              </Typography>
              <Typography variant="body1" color="text.secondary" paragraph>
                We're sorry, but an unexpected error occurred. Please try refreshing the page or go back to home.
              </Typography>
              {(process.env.NODE_ENV === "development" || process.env.NODE_ENV === "test") && this.state.error && (
                <Box sx={{ mt: 2, mb: 2, p: 2, bgcolor: "grey.100", borderRadius: 1, textAlign: "left", overflow: "auto" }}>
                  <Typography variant="caption" component="pre" sx={{ fontFamily: "monospace" }}>
                    {this.state.error.toString()}
                  </Typography>
                </Box>
              )}
              <Button variant="contained" color="primary" onClick={this.handleReset}>
                Go to Home
              </Button>
            </Paper>
          </Box>
        </Container>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
