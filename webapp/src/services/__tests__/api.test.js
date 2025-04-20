import axios from "axios";
import api from "../api";

// Mock axios
jest.mock("axios");

describe("API Service", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("createLink", () => {
    test("calls POST with correct URL and data", async () => {
      const mockData = {
        url: "https://example.com",
        title: "Example Website",
        description: "This is an example website"
      };

      const mockResponse = {
        data: {
          link: {
            id: "link-123",
            url: "https://example.com",
            title: "Example Website",
            description: "This is an example website",
            createdAt: "2025-04-20T12:00:00Z"
          },
          status: "Link received"
        }
      };

      axios.post.mockResolvedValueOnce(mockResponse);

      const result = await api.createLink(mockData);

      expect(axios.post).toHaveBeenCalledWith("/api/v1/link", mockData);
      expect(result).toEqual(mockResponse.data);
    });

    test("handles error and throws it", async () => {
      const mockData = {
        url: "https://example.com",
        title: "Example Website",
        description: "This is an example website"
      };

      const mockError = new Error("Network error");
      axios.post.mockRejectedValueOnce(mockError);

      // Temporarily mock console.error to prevent test output noise
      const originalConsoleError = console.error;
      console.error = jest.fn();

      await expect(api.createLink(mockData)).rejects.toThrow("Network error");
      expect(console.error).toHaveBeenCalledWith("Error creating link:", mockError);

      // Restore console.error
      console.error = originalConsoleError;
    });
  });
});
