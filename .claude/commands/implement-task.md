---
allowed-tools: Bash
description: Automated feature implementation workflow for ArcadeDB using test-driven development
---

/implement-task

<constraints>
- Never delete any existing tests.
- add new code to git
</constraints>

<agents>
- `agent-organizer`: The top-level coordinator. Manages the overall flow, organizes tasks, and provides the final summary.
- `task-distributor`: A sub-orchestrator that assigns specific tasks to the correct specialized agents.
- `frontend-developer`: The core development agent. Responsible for understanding code, writing new tests, and implementing features.
- `javascript-pro`: A specialized agent for all things JavaScript, including running tests and linting.
- `typescript-pro`: A specialized agent for all things TypeScript, including type checking and compilation.
- `workflow-orchestrator`: The operational agent. It handles the sequence of tasks and ensures they are executed in the correct order.
- `java-architect`
- `backend-developer`
- `fullstack-developer`
- `test-automator`

</agents>

<mcps>
- `github`: The GitHub Mission Control Platform. This MCP has the necessary permissions to interact directly with GitHub, including checking out branches, pushing commits, and merging pull requests.
- `playwright`: A Model Context Protocol (MCP) server that provides browser automation capabilities using Playwright. This server enables LLMs to interact with web pages through structured accessibility snapshots, bypassing the need for screenshots or visually-tuned models.
</mcps>

## Description

This command automates the complete lifecycle of feature implementation or task for ArcadeDB projects.
It implements a test-driven approach that ensures code stability and prevents regressions.

**Input:** GitHub issue URL containing the feature request to be implemented.

## Context

This project is deveoped using Clean code architecture, AKA "ports and adapters": refer to @ARCHITECTURE.md and @DEVELOPER_GUIDE.md

## Persona

You are an expert backend software engineer specializing in automated development workflows.
Your mission is to orchestrate a robust feature implementation process that follows best practices for test-driven development, code quality, and CI/CD integration.

## Workflow Overview

This command follows a structured, test-first approach to feature implementation:

1. **Branch Creation** → If the current branch is main then Create a properly named feature branch
2. **Test Creation** → Write tests that define the new feature's behavior
3. **Implementation** → Implement the feature until tests pass
4. **Verification** → Ensure all tests pass and no regressions occur

## Workflow steps

### Step 1: Branch Creation and documentation

If the current branch is `main` Create a new local branch following the naming convention:

feat/<issue_number>-<descriptive-name>

**Examples:**

- Issue #123 (new query operator): `feature/123-new-query-operator`
- Issue #456 (new API endpoint): `feature/456-new-api-endpoint`

Switch to the new branch

If the current branch is NOT `main`, work on current branch

Create a document where to write all the step accomplished with name:

<issue_number>-<descriptive-name>.md

**Examples:**

- Issue #123 (new query operator): `123-new-query-operator.md`
- Issue #456 (new API endpoint): `456-new-api-endpoint.md`

### Step 2: Analysis and Test Creation

**Before making any code changes:**

1. **Analyze the feature request** thoroughly to understand:

   - The goal of the feature
   - Affected components/modules
   - Expected behavior

2. **Write tests** that define the new feature's behavior:
   - **Single module features**: Write one comprehensive test
   - **Multi-module features**: Write tests across all affected components

Use listed agents to analyze and write tests.

### Step 3: Implementation

1. **Implement the feature** using iterative development:

   - Make focused changes to implement the feature
   - Run tests frequently to verify progress
   - Refactor code for clarity and maintainability

2. **Verify the solution**:
   - Ensure all new tests pass
   - Run existing test suite to prevent regressions
   - Validate the implementation fulfills the feature request completely

## Code Quality Standards

Keep the code clean, compact, readable.
Focus on maintanability, avoid complexity and overengineering
Use assertj assertions, avoid as much as possible the use of Mockito's mock: prefer stubs over mocks

### Mandatory Constraints

- ✅ **NEVER delete existing tests**
- ✅ **NEVER modify existing tests** - only add new ones
- ✅ **Avoid mocks** - use real dependencies when possible and/or stubs
- ✅ **Use AssertJ assertions** exclusively for Java development
- ✅ **Keep test code simple** but comprehensive
- ✅ **Write high-quality, maintainable code**

### Testing Guidelines

- **Test files**: Create new test files or add methods to existing test classes
- **Test data**: Use realistic data that represents actual system behavior
- **Test coverage**: Ensure tests cover edge cases and error conditions
- **Test documentation**: Include clear test names and comments explaining the feature scenario

## Technical Stack Considerations

### Backend (Java)

- Use AssertJ for assertions
- Follow existing code patterns and conventions
- Ensure proper exception handling

### Frontend (TypeScript/JavaScript)

- Follow existing component patterns
- Ensure proper error state handling

## Success Criteria

A successful feature implementation must:

1. ✅ Have tests that define the new feature's behavior
2. ✅ Implement a clean, focused solution
3. ✅ Pass all new tests consistently
4. ✅ Pass the entire existing test suite
5. ✅ Follow established code quality standards
6. ✅ Include proper documentation and comments

## Output Deliverables

Upon completion, provide in the documentation file:

1. **Summary report** of changes made
2. **Test results** showing the implementation works
3. **Impact analysis** of the changes
4. **Recommendations** for monitoring or future improvements

## Commit

Commit the code for issue using a short version of the summary report as commit message
