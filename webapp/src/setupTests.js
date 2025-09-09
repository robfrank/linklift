// Add Jest extended matchers
import "@testing-library/jest-dom";

// Polyfill for TextEncoder/TextDecoder (needed for React Router v7+)
if (typeof global.TextEncoder === "undefined") {
    const { TextEncoder, TextDecoder } = require("util");
    global.TextEncoder = TextEncoder;
    global.TextDecoder = TextDecoder;
}

// Suppress console errors in tests
const originalConsoleError = console.error;
console.error = (...args) => {
    // Filter out React Strict Mode warnings and other common testing warnings
    if (
        args[0]?.includes("Warning: ReactDOM") ||
        args[0]?.includes("Warning: React.createFactory") ||
        args[0]?.includes("createRoot") ||
        args[0]?.includes("Warning: The current testing environment") ||
        args[0]?.includes("React.createElement: type is invalid") ||
        args[0]?.includes("Warning: You seem to have overlapping act() calls") ||
        args[0]?.includes("Warning: An update to ForwardRef") ||
        args[0]?.includes("inside a test was not wrapped in act") ||
        args[0]?.includes("Warning: React Router Future Flag Warning")
    ) {
        return;
    }
    originalConsoleError(...args);
};

// Suppress console warnings for React Router future flags
const originalConsoleWarn = console.warn;
console.warn = (...args) => {
    if (
        args[0]?.includes("React Router Future Flag Warning") ||
        args[0]?.includes("MUI:") ||
        args[0]?.includes("Ringing") ||
        args[0]?.includes("deprecated") ||
        args[0]?.includes("No routes matched location") ||
        (typeof args[0] === "object" && args[0]?.type?.includes?.("router-warning"))
    ) {
        return;
    }
    originalConsoleWarn(...args);
};

// Mock matchMedia for Material UI
Object.defineProperty(window, "matchMedia", {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(), // deprecated
        removeListener: jest.fn(), // deprecated
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});
