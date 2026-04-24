# Current Limitations

This document outlines the current limitations of the RuneScript compiler implementation.

## Known Limitations

### 1. User-Defined Named Functions

`fun` / `def` style user-defined function declarations are not yet implemented. Only the built-in `print()` function is available.

### 3. Error Message Precision

Some error messages may not pinpoint exact column locations. Error recovery could be improved for better user feedback.

### 4. Single-Byte Constant and Local-Slot Indices

Constants and local variable slot indices are stored as single bytes, limiting programs to 255 unique constants and 255 local variables. This is acceptable for educational programs.

## Working Features

- ✅ All three primitive types: `int`, `bool`, `string`
- ✅ Variable declarations with type annotations: `let name: type = value;`
- ✅ Variable reassignment: `x = x + 1;`
- ✅ All arithmetic operators: `+`, `-`, `*`, `/` (integer division)
- ✅ All comparison operators: `==`, `!=`, `<`, `<=`, `>`, `>=`
- ✅ Logical NOT operator: `!`
- ✅ `if` / `else if` / `else` statements
- ✅ `while` loops
- ✅ Scoped blocks `{ }` with proper variable lifetime
- ✅ `print()` with single or multiple arguments: `print("Hello, ", name, "!")`
- ✅ Pipe operator: `5 |> print();`, `value |> f` 
- ✅ Lambda expressions: `(x -> x + 5)`, `(a, b -> a + b)`
- ✅ Pipe chaining with lambdas: `value |> (x -> x + 5) |> (x -> x * 2) |> print()`
- ✅ First-class lambdas: `let double = (x -> x * 2); print(double(7));`
- ✅ Closures over outer variables (captured by value at definition time)
- ✅ Multi-parameter lambdas: `let add = (a, b -> a + b);`
- ✅ Single-line comments: `// comment`
- ✅ Interactive REPL with persistent variable state across lines
- ✅ CLI flags: `--emit-tokens`, `--emit-ast`, `--emit-bytecode`
- ✅ Negative integer literals: `let temp: int = -5;`

## Educational Value

These limitations demonstrate:

1. **Incremental language design** — core features before advanced ones
2. **Parser extensibility** — how to add new syntax (e.g., lambda expressions)
3. **Trade-offs** between simplicity and functionality in educational compilers
