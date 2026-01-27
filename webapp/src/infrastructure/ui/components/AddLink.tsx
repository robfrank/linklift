import React, { useState, ChangeEvent, FormEvent } from "react";
import { Container, Typography, Paper, Box, TextField, Button, CircularProgress } from "@mui/material";

interface FormData {
  url: string;
  title: string;
  description: string;
}

interface AddLinkProps {
  onSubmit: (data: FormData) => void;
  onCancel: () => void;
  loading: boolean;
}

export const AddLink: React.FC<AddLinkProps> = ({ onSubmit, onCancel, loading }) => {
  const [formData, setFormData] = useState<FormData>({
    url: "",
    title: "",
    description: ""
  });
  const [errors, setErrors] = useState<Partial<FormData>>({});

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value
    }));

    // Clear error when user types
    if (errors[name as keyof FormData]) {
      setErrors((prev) => ({
        ...prev,
        [name]: ""
      }));
    }
  };

  const validateForm = () => {
    const newErrors: Partial<FormData> = {};

    if (!formData.url) {
      newErrors.url = "URL is required";
    } else {
      try {
        new URL(formData.url);
      } catch {
        newErrors.url = "Please enter a valid URL";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;
    onSubmit(formData);
  };

  return (
    <Container maxWidth="md">
      <Box my={4}>
        <Paper elevation={3} sx={{ p: 4 }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Add New Link
          </Typography>
          <Typography variant="body2" color="text.secondary" paragraph>
            Add a new link to your collection. Make sure to provide a descriptive title and description.
          </Typography>

          <form onSubmit={handleSubmit}>
            <TextField
              fullWidth
              margin="normal"
              label="URL"
              name="url"
              type="url"
              value={formData.url}
              onChange={handleChange}
              error={!!errors.url}
              helperText={errors.url || "Enter the full URL including https://"}
              placeholder="https://example.com"
              disabled={loading}
            />

            <TextField
              fullWidth
              margin="normal"
              label="Title"
              name="title"
              value={formData.title}
              onChange={handleChange}
              error={!!errors.title}
              helperText={errors.title || "A short, descriptive title for the link"}
              placeholder="Website Title"
              disabled={loading}
            />

            <TextField
              fullWidth
              margin="normal"
              label="Description"
              name="description"
              value={formData.description}
              onChange={handleChange}
              error={!!errors.description}
              helperText={errors.description || "Brief description of what this link is about"}
              placeholder="A brief description of the website content"
              multiline
              rows={3}
              disabled={loading}
            />

            <Box mt={3} display="flex" justifyContent="flex-end" gap={2}>
              <Button type="button" variant="outlined" color="secondary" onClick={onCancel} disabled={loading}>
                Cancel
              </Button>
              <Button type="submit" variant="contained" color="primary" disabled={loading} startIcon={loading ? <CircularProgress size={20} /> : null}>
                {loading ? "Adding..." : "Add Link"}
              </Button>
            </Box>
          </form>
        </Paper>
      </Box>
    </Container>
  );
};
