# Contributing to Wilddiary HTTP Relay Server

Thank you for your interest in contributing to the Wilddiary HTTP Relay Server project! We appreciate your contributions, whether they are bug reports, feature suggestions, or code contributions. Below are the guidelines for contributing to this project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
    - [Reporting Bugs](#reporting-bugs)
    - [Suggesting Enhancements](#suggesting-enhancements)
    - [Code Contributions](#code-contributions)
- [Development Process](#development-process)
    - [Setup the Development Environment](#setup-the-development-environment)
    - [Development Workflow](#development-workflow)
    - [Commit Messages](#commit-messages)
- [Pull Request Guidelines](#pull-request-guidelines)
- [License](#license)

## Code of Conduct

This project follows a [Code of Conduct](./CODE_OF_CONDUCT.md) to foster an inclusive, respectful, and open environment. Please make sure to review and follow these guidelines when interacting with the project.

## How Can I Contribute?

### Reporting Bugs

If you encounter a bug, please report it by creating a [GitHub issue](https://github.com/Wilddiary/wd-http-relay-server/issues). Be sure to include:

1. A clear and descriptive title.
2. Steps to reproduce the problem.
3. Expected vs. actual behavior.
4. Any relevant logs, screenshots, or error messages.
5. Your environment (OS, Java version, etc.).

### Suggesting Enhancements

If you have ideas to improve the proxy or add new features, we welcome your suggestions! Please create a new [GitHub issue](https://github.com/Wilddiary/wd-http-relay-server/issues) with the following details:

1. A detailed description of the enhancement or feature.
2. Why you believe it would be useful.
3. Any potential challenges or risks.
4. How it fits within the project scope.

### Code Contributions

We welcome code contributions! Before starting any work, it’s a good idea to discuss the changes you plan to make by opening a new issue or commenting on an existing one.

Please follow the steps outlined in the [Development Process](#development-process) to set up your development environment and begin coding.

## Development Process

### Setup the Development Environment

1. **Fork the repository**: Fork the project repository to your GitHub account.
2. **Clone your fork**:
   ```bash
   git clone https://github.com/Wilddiary/wd-http-relay-server.git
   cd wd-http-relay-server
   ```
3. **Set up the project**:
    - Ensure that you have Java 17+ and Maven 3.6+ installed.
    - Build the project:
      ```bash
      mvn clean install
      ```
4. **Create a new branch** for your work:
   ```bash
   git checkout -b feature/your-feature-name
   ```

### Development Workflow

- Make sure your code adheres to the existing coding standards and conventions.
- Write clear, modular, and well-documented code.
- Add unit tests for any new functionality.
- Ensure that the project builds successfully and passes all tests by running:
  ```bash
  mvn clean test
  ```

### Commit Messages

- Use meaningful and descriptive commit messages.
- Format commit messages as follows:
    - **Feat**: Adds a new feature.
    - **Fix**: Fixes a bug.
    - **Refactor**: Code improvement that doesn't add a feature or fix a bug.
    - **Docs**: Updates or adds documentation.
    - **Test**: Adds or modifies tests.

Example commit message:

```
feat: add gzip compression support for responses
```

## Pull Request Guidelines

1. **Create a pull request**: Once your feature or fix is ready, push your changes to your fork and create a pull request to the `main` branch of the original repository.

2. **Describe your changes**: In the pull request description, clearly describe what changes you made, why you made them, and how they solve the problem.

3. **Link to related issues**: If your pull request addresses an issue, link to it in the description (e.g., `Closes #123`).

4. **Ensure all tests pass**: Before submitting the pull request, ensure all tests pass, and your code is properly formatted.

5. **Code Review**: Be responsive to feedback during the code review process. Make the necessary changes and update your pull request accordingly.

## License

By contributing to the Wilddiary HTTP Relay Server project, you agree that your contributions will be licensed under the [Apache 2.0 License](./LICENSE).

---

This `CONTRIBUTING.md` file helps establish clear guidelines for contributing to the project, making it easier for new contributors to understand the process and start working on the project. You can adjust specific repository URLs and details as necessary for your project.