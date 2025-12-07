module.exports = {
  testEnvironment: "jsdom",
  moduleNameMapper: {
    "\\.(css|less|scss|sass)$": "identity-obj-proxy"
  },
  setupFilesAfterEnv: ["<rootDir>/src/setupTests.js"],
  testMatch: ["**/__tests__/**/*.{js,jsx,ts,tsx}", "**/?(*.)+(spec|test).{js,jsx,ts,tsx}"],
  collectCoverageFrom: ["src/**/*.{js,jsx,ts,tsx}", "!src/index.tsx", "!src/setupTests.js", "!**/node_modules/**"],
  transform: {
    "^.+\\.(js|jsx|ts|tsx)$": "babel-jest"
  }
};
