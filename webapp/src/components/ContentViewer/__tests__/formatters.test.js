import { formatBytes, formatDate } from "../../../utils/formatters";

describe("formatters", () => {
  describe("formatBytes", () => {
    it("formats zero bytes", () => {
      expect(formatBytes(0)).toBe("0 Bytes");
    });

    it("formats bytes correctly", () => {
      expect(formatBytes(500)).toBe("500 Bytes");
    });

    it("formats kilobytes correctly", () => {
      expect(formatBytes(1536)).toBe("1.5 KB");
    });

    it("formats megabytes correctly", () => {
      expect(formatBytes(1572864)).toBe("1.5 MB");
    });

    it("formats gigabytes correctly", () => {
      expect(formatBytes(1610612736)).toBe("1.5 GB");
    });
  });

  describe("formatDate", () => {
    beforeEach(() => {
      // Mock the current date to have consistent tests
      jest.useFakeTimers();
      jest.setSystemTime(new Date("2025-10-16T12:00:00Z"));
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('shows "Just now" for very recent dates', () => {
      const now = new Date("2025-10-16T12:00:00Z").toISOString();
      expect(formatDate(now)).toBe("Just now");
    });

    it("shows minutes ago for recent dates", () => {
      const fiveMinutesAgo = new Date("2025-10-16T11:55:00Z").toISOString();
      expect(formatDate(fiveMinutesAgo)).toBe("5 minutes ago");
    });

    it("shows hours ago for dates within 24 hours", () => {
      const twoHoursAgo = new Date("2025-10-16T10:00:00Z").toISOString();
      expect(formatDate(twoHoursAgo)).toBe("2 hours ago");
    });

    it("shows days ago for dates within a week", () => {
      const threeDaysAgo = new Date("2025-10-13T12:00:00Z").toISOString();
      expect(formatDate(threeDaysAgo)).toBe("3 days ago");
    });

    it("shows formatted date for older dates", () => {
      const oldDate = new Date("2025-09-01T12:00:00Z").toISOString();
      const result = formatDate(oldDate);
      expect(result).toContain("Sep");
      expect(result).toContain("2025");
    });
  });
});
