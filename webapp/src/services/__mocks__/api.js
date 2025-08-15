const api = {
  createLink: jest.fn().mockResolvedValue({
    link: {
      id: "mock-id-123",
      url: "https://example.com",
      title: "Example Website",
      description: "This is an example website",
      extractedAt: "2025-04-20T12:00:00Z",
      contentType: "text/html"
    },
    status: "Link received"
  }),

  listLinks: jest.fn().mockResolvedValue({
    content: [
      {
        id: "mock-id-123",
        url: "https://example.com",
        title: "Example Website",
        description: "This is an example website",
        extractedAt: "2025-04-20T12:00:00Z",
        contentType: "text/html"
      }
    ],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
    hasNext: false,
    hasPrevious: false
  })
};

export default api;
