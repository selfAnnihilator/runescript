# Getting Started

This guide will help you set up and start using the RuneScript Compiler.

## Prerequisites

Before you begin, ensure you have Java 21 or higher installed on your system. You can verify your Java version by running:

```bash
java -version
```

If you don't have Java 21+, download and install it from the [Oracle JDK](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) or [OpenJDK](https://openjdk.org/projects/jdk/21/) distribution.

## Installation

1. Download the latest `runescript.jar` from the releases section of the repository
2. Place the JAR file in a convenient location on your system

## Running Scripts

To run a RuneScript file, use the following command:

```bash
java -jar runescript.jar <file.rn>
```

### Example: Hello World

Create a file named `hello.rn` with the following content:

```runescript
print("Hello, RuneScript!")
```

Run the script:

```bash
java -jar runescript.jar hello.rn
```

Expected output:
```
Hello, RuneScript!
```

### Example: Simple Calculation

Create a file named `math.rn`:

```runescript
result = 10 + 5 * 2
print("Result: " + result)
```

Run the script:

```bash
java -jar runescript.jar math.rn
```

Expected output:
```
Result: 20
```

### Example: Using the Pipe Operator

Create a file named `pipe.rn` to demonstrate the pipe operator:

```runescript
value = 5
result = value |> (x -> x * 2) |> (x -> x + 10)
print("Result: " + result)
```

Run the script:

```bash
java -jar runescript.jar pipe.rn
```

Expected output:
```
Result: 20
```

## Development Environment

For a better development experience, you can use any text editor or IDE that supports syntax highlighting for similar languages. The compiler handles all the compilation and execution steps for you.