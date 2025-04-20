const api = {
  createLink: jest.fn().mockResolvedValue({
    link: {
      id: "mock-id-123",
      url: "https://example.com",
      title: "Example Website",
      description: "This is an example website",
      createdAt: "2025-04-20T12:00:00Z"
    },
    status: "Link received"
  })
};

export default api;
