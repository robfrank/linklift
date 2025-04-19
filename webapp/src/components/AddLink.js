import React, { useState } from "react";
import { Container, Typography, Paper, Box, TextField, Button, Snackbar, Alert } from "@mui/material";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

const AddLink = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    url: "",
    title: "",
    description: ""
  });
  const [errors, setErrors] = useState({});
  const [snackbar, setSnackbar] = useState({
    open: false,
    message: "",
    severity: "success"
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({
      ...formData,
      [name]: value
    });

    // Clear error when user types
    if (errors[name]) {
      setErrors({
        ...errors,
        [name]: ""
      });
    }
  };

  const validateForm = () => {
    const newErrors = {};
    if (!formData.url) newErrors.url = "URL is required";
    if (!formData.title) newErrors.title = "Title is required";
    if (!formData.description) newErrors.description = "Description is required";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    try {
      await api.createLink(formData);
      setSnackbar({
        open: true,
        message: "Link added successfully!",
        severity: "success"
      });

      // Reset form
      setFormData({
        url: "",
        title: "",
        description: ""
      });

      // Redirect to home after short delay
      setTimeout(() => {
        navigate("/");
      }, 2000);
    } catch (error) {
      let errorMessage = "Failed to add link";

      if (error.response) {
        // Handle specific error responses from API
        if (error.response.status === 409) {
          errorMessage = "This URL already exists";
        } else if (error.response.data && error.response.data.message) {
          errorMessage = error.response.data.message;
        }
      }

      setSnackbar({
        open: true,
        message: errorMessage,
        severity: "error"
      });
    }
  };

  const handleCloseSnackbar = () => {
    setSnackbar({
      ...snackbar,
      open: false
    });
  };

  return (
    <Container maxWidth="md">
      <Box my={4}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Add New Link
          </Typography>

          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              margin="normal"
              label="URL"
              name="url"
              value={formData.url}
              onChange={handleChange}
              error={!!errors.url}
              helperText={errors.url}
              placeholder="https://example.com"
            />

            <TextField
              fullWidth
              margin="normal"
              label="Title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              error={!!errors.title}
              helperText={errors.title}
              placeholder="Website Title"
            />

            <TextField
              fullWidth
              margin="normal"
              label="Description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              error={!!errors.description}
              helperText={errors.description}
              placeholder="A brief description of the website"
              multiline
              rows={3}
            />

            <Box mt={3} display="flex" justifyContent="flex-end">
              <Button type="button" variant="outlined" color="secondary" onClick={() => navigate("/")} sx={{ mr: 2 }}>
                Cancel
              </Button>
              <Button type="submit" variant="contained" color="primary">
                Add Link
              </Button>
            </Box>
          </form>
        </Paper>
      </Box>

      <Snackbar open={snackbar.open} autoHideDuration={6000} onClose={handleCloseSnackbar} anchorOrigin={{ vertical: "bottom", horizontal: "center" }}>
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} variant="filled">
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Container>
  );
};

export default AddLink;
