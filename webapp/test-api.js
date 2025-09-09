#!/usr/bin/env node

/**
 * Simple script to test the API endpoints manually
 * Run with: node test-api.js
 * Make sure the backend is running on localhost:8080
 */

const axios = require("axios");

const API_BASE_URL = "http://localhost:8080/api/v1";

async function testListLinks() {
    console.log("Testing List Links API...");
    try {
        const response = await axios.get(`${API_BASE_URL}/links`, {
            params: {
                page: 0,
                size: 10,
                sortBy: "extractedAt",
                sortDirection: "DESC"
            }
        });
        console.log("✅ List Links API Response:", JSON.stringify(response.data, null, 2));
        return response.data;
    } catch (error) {
        console.log("❌ List Links API Error:", error.message);
        if (error.response) {
            console.log("Response Status:", error.response.status);
            console.log("Response Data:", error.response.data);
        }
        return null;
    }
}

async function testCreateLink() {
    console.log("\nTesting Create Link API...");
    const testLink = {
        url: "https://example.com",
        title: "Test Link",
        description: "This is a test link created by the test script"
    };

    try {
        const response = await axios.put(`${API_BASE_URL}/link`, testLink);
        console.log("✅ Create Link API Response:", JSON.stringify(response.data, null, 2));
        return response.data;
    } catch (error) {
        console.log("❌ Create Link API Error:", error.message);
        if (error.response) {
            console.log("Response Status:", error.response.status);
            console.log("Response Data:", error.response.data);
        }
        return null;
    }
}

async function main() {
    console.log("=== LinkLift API Test Script ===\n");
    console.log("Make sure the backend is running on localhost:8080\n");

    // Test the APIs
    await testListLinks();
    await testCreateLink();

    console.log("\n=== Test completed ===");
}

main();
