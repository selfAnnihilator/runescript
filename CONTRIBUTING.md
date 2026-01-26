# Contributing to RuneScript

Thank you for your interest in contributing to RuneScript! This document outlines the process for contributing to the project.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/runescript.git`
3. Create a branch: `git checkout -b feature-name`
4. Make your changes
5. Test your changes
6. Commit your changes: `git commit -m "Add feature"`
7. Push to your fork: `git push origin feature-name`
8. Submit a pull request

## Development Setup

1. Install Java 21 or higher
2. Clone the repository
3. The entire compiler is in `src/RuneScript.java`
4. Build with: `javac src/RuneScript.java`
5. Run with: `java RuneScript [options] [file]`

## Project Structure

- `src/` - Source code and executable JAR
- `examples/` - Example RuneScript programs
- `docs/` - Documentation
- `README.md` - Main project documentation
- `LICENSE` - MIT License
- `CHANGELOG.md` - Version history

## Code Style

- Follow Java conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Maintain the single-file structure when possible

## Testing

Before submitting changes:
1. Test with the example files in `examples/`
2. Verify all CLI options work (--emit-tokens, --emit-ast, etc.)
3. Test the REPL mode
4. Ensure error handling works properly

## Reporting Issues

When reporting issues, please include:
- Java version (`java -version`)
- Operating system
- Steps to reproduce
- Expected vs actual behavior
- Sample code that reproduces the issue

## Pull Request Guidelines

- Keep PRs focused on a single feature or bug fix
- Update documentation if needed
- Include tests for new functionality
- Follow existing code style
- Reference related issues in the PR description

## Questions?

Feel free to open an issue if you have questions about contributing.