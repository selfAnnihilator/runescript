# Language Guide

## Types

RuneScript supports three primitive types:

### int
Integer numbers (positive, negative, zero)
```runescript
let age: int = 25;
let temperature: int = -5;
let count: int = 0;
```

### bool
Boolean values (`true` or `false`)
```runescript
let is_valid: bool = true;
let is_error: bool = false;
```

### string
Text values enclosed in double quotes
```runescript
let name: string = "Alice";
let message: string = "Hello, World!";
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

## Unique Feature: Pipe Operator

The pipe operator passes the value on the left as the first argument to the function on the right:

```runescript
// Instead of: print(5)
5 |> print();  // Same result, but more readable for chains

// Chaining operations
let value: int = 10;
value |> (x -> x + 5) |> (x -> x * 2) |> print();  // print((value + 5) * 2)
```

## Syntax Rules

### Comments
Use `//` for single-line comments:
```runescript
// This is a comment
let x: int = 5;  // Comment at end of line
```

### Semicolons
Statements must end with semicolons:
```runescript
print("Hello");  // Correct
let x: int = 5;  // Correct
```

### Whitespace
Whitespace is ignored (spaces, tabs, newlines):
```runescript
let x:int=5;     // Valid
let x : int = 5; // Also valid
```