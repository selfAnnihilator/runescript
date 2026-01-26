# RuneScript Language Guide

RuneScript is a statically-typed scripting language with a unique pipe operator and modern programming constructs.

## Table of Contents
1. [Basic Syntax](#basic-syntax)
2. [Data Types](#data-types)
3. [Variables](#variables)
4. [Operators](#operators)
5. [Control Flow](#control-flow)
6. [Functions](#functions)
7. [Unique Features](#unique-features)
8. [Examples](#examples)

## Basic Syntax

RuneScript follows C-style syntax with semicolon termination:

```runescript
print("Hello, World!");  // Comments start with //
let x: int = 42;         // Statements end with semicolon
```

## Data Types

RuneScript has three primitive types:

- **int**: Integer numbers (e.g., `42`, `-5`, `0`)
- **bool**: Boolean values (`true`, `false`)
- **string**: Text enclosed in double quotes (e.g., `"Hello"`)

```runescript
let age: int = 25;
let is_valid: bool = true;
let name: string = "Alice";
```

## Variables

### Declaration
Variables are declared using the `let` keyword with explicit type annotations:

```runescript
let name: type = expression;
```

Examples:
```runescript
let count: int = 10;
let message: string = "Welcome!";
let is_active: bool = false;
```

### Assignment
After declaration, variables can be reassigned:

```runescript
let x: int = 5;
x = x + 10;  // x is now 15
```

## Operators

### Arithmetic Operators
- `+` (addition)
- `-` (subtraction) 
- `*` (multiplication)
- `/` (division)

```runescript
let a: int = 10;
let b: int = 3;
print(a + b);  // 13
print(a - b);  // 7
print(a * b);  // 30
print(a / b);  // 3 (integer division)
```

### Comparison Operators
- `==` (equal to)
- `!=` (not equal to)
- `<` (less than)
- `<=` (less than or equal to)
- `>` (greater than)
- `>=` (greater than or equal to)

```runescript
let x: int = 5;
let y: int = 10;
print(x < y);    // true
print(x == y);   // false
print(x != y);   // true
```

### Logical Operators
- `!` (logical NOT)

```runescript
let is_sunny: bool = true;
print(!is_sunny);  // false
```

## Control Flow

### If-Else Statements
```runescript
if (condition) {
    // code when condition is true
} else {
    // code when condition is false
}
```

Example:
```runescript
let temperature: int = 25;
if (temperature > 30) {
    print("It's hot!");
} else {
    print("It's comfortable.");
}
```

### While Loops
```runescript
while (condition) {
    // code to repeat while condition is true
}
```

Example:
```runescript
let counter: int = 0;
while (counter < 3) {
    print(counter);
    counter = counter + 1;
}
// Output: 0, 1, 2
```

### Blocks
Use curly braces `{}` to create scoped blocks:

```runescript
{
    let x: int = 10;
    print(x);
}  // x goes out of scope here
```

## Functions

### Built-in Functions
- `print(value)` - Outputs the value followed by a newline

```runescript
print("Hello");
print(42);
print(true);
```

## Unique Features

### Pipe Operator (|>)
The pipe operator passes the value on the left as the first argument to the function on the right:

```runescript
// Instead of: print(5)
5 |> print();  // Same result, but more readable for chains

// Chaining operations
let value: int = 10;
value |> add(5) |> multiply(2) |> print();  // print(multiply(add(value, 5), 2))
```

## Examples

### Simple Calculator
```runescript
let a: int = 15;
let b: int = 3;

print("Addition: ", a + b);
print("Subtraction: ", a - b);
print("Multiplication: ", a * b);
print("Division: ", a / b);
```

### Conditional Logic
```runescript
let score: int = 85;
let grade: string;

if (score >= 90) {
    grade = "A";
} else if (score >= 80) {
    grade = "B";
} else if (score >= 70) {
    grade = "C";
} else {
    grade = "F";
}

print("Grade: ", grade);
```

### Loop with Condition
```runescript
let num: int = 1;
while (num <= 5) {
    if (num % 2 == 0) {
        print(num, " is even");
    } else {
        print(num, " is odd");
    }
    num = num + 1;
}
```

### Using Pipe Operator
```runescript
// Calculate and print: (10 + 5) * 2
let base: int = 10;
base |> add(5) |> multiply(2) |> print();  // Prints 30
```

## Common Patterns

### Input Validation
```runescript
let user_age: int = 25;
if (user_age >= 18) {
    print("Adult access granted");
} else {
    print("Access denied - minor");
}
```

### Counter Loops
```runescript
let i: int = 0;
while (i < 10) {
    print("Iteration: ", i);
    i = i + 1;
}
```

### Value Transformation
```runescript
let original: int = 5;
let doubled: int = original * 2;
let final_value: int = doubled + 10;
print(final_value);  // 20
```

## Error Handling

RuneScript provides detailed error messages with line and column information:

```
[line 5] Error at 'x': Undefined variable 'x'.
```

Common errors:
- Using undefined variables
- Type mismatches in operations
- Syntax errors (missing semicolons, unmatched brackets)

## Best Practices

1. **Always declare variable types explicitly**
2. **Use meaningful variable names**
3. **Comment complex logic**
4. **Use blocks to limit variable scope**
5. **Chain operations with the pipe operator for readability**

## File Extensions

Save RuneScript files with the `.rn` extension:
- `hello.rn`
- `calculator.rn`
- `game_logic.rn`