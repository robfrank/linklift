module.exports = {
  testEnvironment: "jsdom",
  moduleNameMapper: {
    "\\.(css|less|scss|sass)$": "identity-obj-proxy"
  },
  setupFilesAfterEnv: ["<rootDir>/src/setupTests.js"],
  testMatch: ["**/__tests__/**/*.js", "**/?(*.)+(spec|test).js"],
  collectCoverageFrom: ["src/**/*.{js,jsx}", "!src/index.js", "!src/setupTests.js", "!**/node_modules/**"],
  transform: {
    "^.+\\.(js|jsx)$": "babel-jest"
  }
};
