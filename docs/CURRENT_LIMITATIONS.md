# RuneScript Language Guide (Current Implementation)

RuneScript is a statically-typed scripting language with a unique pipe operator and modern programming constructs.

⚠️ **Note**: This is a reference implementation with some current limitations that would be addressed in a production compiler.

## Current Capabilities

### Basic Syntax
- Semicolon-terminated statements
- C-style comments with `//`
- Curly braces for blocks

### Data Types
- **string**: Text values (works well)
- **bool**: Boolean values (works well)  
- **int**: Integer values (has conversion issues in current implementation)

### Variables
- Variable declarations: `let name: type = value;`
- Basic assignment: `name = new_value;`
- Note: Variable scoping is implemented but has some runtime issues

### Operators
- Basic arithmetic: `+`, `-`, `*`, `/` (work with proper values)
- Comparisons: `==`, `!=`, `<`, `<=`, `>`, `>=` (work with proper values)
- Logical: `!` (negation)

### Control Flow
- If-else statements: `if (condition) { ... } else { ... }`
- While loops: `while (condition) { ... }`
- Blocks: `{ ... }`

### Functions
- Built-in `print(value)` function

### Unique Feature: Pipe Operator
- `value |> function()` - passes value as first argument
- Supports chaining: `a |> f() |> g()`

## Known Limitations (Current Implementation)

1. **Numeric Conversion**: Numbers may not display correctly due to internal conversion issues
2. **Complex Expressions**: Some complex expressions may cause runtime errors
3. **Multi-argument Functions**: Print only supports single arguments

## Working Examples

### Hello World
```runescript
print("Hello, RuneScript!");
```

### Basic Values
```runescript
print("Text works fine");
print(true);
print(false);
// Numbers have conversion issues in current implementation
```

### Variables (Limited)
```runescript
let name: string = "Alice";
print(name);
```

### Simple Control Flow
```runescript
let isActive: bool = true;
if (isActive) {
    print("Active");
} else {
    print("Inactive");
}
```

## Running Programs

```bash
java -jar runescript.jar script.rn
```

## Development Notes

This implementation demonstrates:
- Complete compiler pipeline (Lexing → Parsing → Type Checking → Code Generation → Execution)
- Modern Java 21 features (sealed interfaces, records)
- Stack-based virtual machine
- Error handling with detailed diagnostics
- The unique pipe operator feature

For a production compiler, additional work would include:
- Fixing numeric type conversions
- Improving variable scoping and storage
- Adding garbage collection
- Optimizing performance
- Expanding standard library