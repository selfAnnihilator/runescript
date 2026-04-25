# RuneScript — Project Notes

A complete walkthrough of what this project is, why it exists, how it works, and what it can do.

---

## What is RuneScript?

RuneScript is a programming language built from scratch. It has its own syntax, its own compiler, and its own runtime. You write code in `.rn` files, and RuneScript compiles that code into bytecode instructions which are then executed by a virtual machine (VM) — all implemented in Java.

It is a **statically-typed, interpreted language** in the style of modern scripting languages, with one distinctive feature: the **pipe operator** (`|>`) which lets you chain operations in a readable left-to-right style.

The entire implementation lives in one file: `src/RuneScript.java`.

---

## Why Was It Made?

This is an **educational compiler project**. The goal is to demonstrate — from scratch, with no libraries — how a complete programming language works end to end:

- How source code text becomes structured data (lexing and parsing)
- How that structure is checked for correctness (type checking)
- How it becomes executable instructions (bytecode compilation)
- How those instructions actually run on a machine (virtual machine execution)

Most programmers use languages without ever thinking about how they work internally. This project makes all of that visible and concrete.

---

## What Can It Do?

A working RuneScript program can:

- Declare and use typed variables
- Do arithmetic and comparisons
- Make decisions with `if/else`
- Repeat actions with `while` loops
- Print output to the terminal
- Define and call anonymous functions (lambdas)
- Chain operations with the pipe operator
- Run interactively line by line in a REPL

### Example program

```runescript
let name: string = "Alice";
let score: int = 92;

if (score >= 90) {
    print("Well done, ", name, "! Grade: A");
} else if (score >= 80) {
    print("Good job, ", name, "! Grade: B");
} else {
    print("Keep going, ", name, ".");
}

// Pipe operator: read left to right instead of nesting calls
let result: int = 5;
result |> (x -> x + 10) |> (x -> x * 2) |> print();
// Same as: print(((5 + 10) * 2)) → prints 30
```

---

## How to Run It

You need Java 21 or higher. Only one file is needed: `src/runescript.jar`.

```bash
# Run a script file
java -jar runescript.jar program.rn

# Start the interactive REPL
java -jar runescript.jar

# Debugging: see internal stages
java -jar runescript.jar --emit-tokens program.rn    # tokenization output
java -jar runescript.jar --emit-ast program.rn       # parse tree output
java -jar runescript.jar --emit-bytecode program.rn  # bytecode output
```

---

## The Language

### Types

RuneScript has three primitive types:

```runescript
let age: int = 25;          // whole numbers
let active: bool = true;    // true or false
let name: string = "Alice"; // text
```

### Variables

Declared with `let`, type annotation after a colon. Type is optional — if omitted, it is inferred from the initial value.

```runescript
let x: int = 10;
let greeting = "Hello";  // type inferred as string
x = x + 5;               // reassignment
```

### Operators

```runescript
// Arithmetic
x + y    x - y    x * y    x / y   // integer division

// Comparison (all return bool)
x == y   x != y   x < y   x <= y   x > y   x >= y

// Logical
!is_valid    // NOT
```

### Control Flow

```runescript
if (score >= 90) {
    print("A");
} else if (score >= 80) {
    print("B");
} else {
    print("C");
}

let i: int = 0;
while (i < 5) {
    print(i);
    i = i + 1;
}

// Blocks create their own scope
{
    let temp: int = 42;
    print(temp);
}
// temp no longer exists here
```

### Print

`print()` accepts any number of arguments and concatenates them on one line:

```runescript
print("Score: ", score, " — great!");
```

### The Pipe Operator

The pipe operator `|>` passes the value on its left as the first argument to the expression on its right. It makes chains of operations readable.

```runescript
// Without pipe (nested, read inside-out):
print(multiply(add(value, 5), 2));

// With pipe (flat, read left to right):
value |> (x -> x + 5) |> (x -> x * 2) |> print();
```

### Lambdas and Closures

Lambdas are anonymous functions defined inline. They can be stored in variables, passed around, and they close over variables from the surrounding scope.

```runescript
// Single parameter
let double = (x -> x * 2);
print(double(7));    // 14

// Multiple parameters
let add = (a, b -> a + b);
print(add(3, 4));    // 7

// Closures — captures outer variable at definition time
let offset: int = 100;
let shift = (x -> x + offset);
print(shift(42));    // 142

// Lambdas used inline in pipes
10 |> (x -> x + 5) |> (x -> x * 2) |> print();  // 30
```

---

## How It Works Internally

RuneScript has a classic five-stage compiler pipeline. Each stage transforms the program from one representation to the next.

```
Source text (.rn file)
        ↓
    [ Lexer ]           → tokens
        ↓
    [ Parser ]          → Abstract Syntax Tree (AST)
        ↓
    [ Resolver ]        → type-checked AST
        ↓
  [ Bytecode Emitter ]  → bytecode instructions
        ↓
  [ Virtual Machine ]   → execution / output
```

### Stage 1 — Lexer (`class Lexer`)

The lexer reads the raw source text character by character and groups characters into **tokens** — the meaningful units of the language.

```
"let x: int = 5 + 3;"
→ [LET] [IDENTIFIER:x] [COLON] [IDENTIFIER:int] [EQUAL] [NUMBER:5] [PLUS] [NUMBER:3] [SEMICOLON]
```

Each token has a type, its text, and the line number it came from. Comments and whitespace are discarded here.

### Stage 2 — Parser (`class Parser`)

The parser takes the flat list of tokens and builds a tree that represents the **structure and meaning** of the code. This tree is called an Abstract Syntax Tree (AST).

```
let x: int = 5 + 3;
→ Stmt.Var(
      name: x,
      type: int,
      initializer: Expr.Binary(
          left:  Expr.Literal(5),
          op:    PLUS,
          right: Expr.Literal(3)
      )
  )
```

The parser is a **recursive descent parser** — it calls itself recursively to handle nested expressions and statements, following the grammar of the language.

AST nodes are defined using Java 21 **sealed interfaces** and **records**, which makes them clean and pattern-matchable.

### Stage 3 — Resolver (`class Resolver`)

The resolver walks the AST and performs **static analysis** before execution:

- Checks that variables are declared before use
- Checks that types match (e.g. you can't assign a `string` to an `int` variable)
- Reports errors with line numbers

If any errors are found, execution stops here.

### Stage 4 — Bytecode Emitter (`class BytecodeEmitter`)

The emitter walks the type-checked AST and translates it into a sequence of simple **bytecode instructions** — a compact binary format designed for the VM to execute efficiently.

```
let x: int = 5 + 3;
→ CONSTANT 5
  CONSTANT 3
  ADD
  (x is now declared at stack slot 0)
```

Instructions include things like:
`CONSTANT`, `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `EQUAL`, `LESS`, `GREATER`, `NOT`, `NEGATE`, `JUMP`, `JUMP_IF_FALSE`, `GET_LOCAL`, `SET_LOCAL`, `PRINT`, `MAKE_LAMBDA`, `CALL`, `RETURN`

Lambdas are compiled into `LambdaTemplate` objects stored in the constant pool. When `MAKE_LAMBDA` executes at runtime, it snapshots the current local variable values to create a closure.

### Stage 5 — Virtual Machine (`class VM`)

The VM executes the bytecode. It is a **stack-based machine** — all computation happens by pushing and popping values on an operand stack.

```
CONSTANT 5     → stack: [5]
CONSTANT 3     → stack: [5, 3]
ADD            → stack: [8]       (popped 5 and 3, pushed 8)
```

Local variables live at fixed positions at the bottom of the stack. `GET_LOCAL n` pushes the value at slot `n`. `SET_LOCAL n` updates slot `n`.

Control flow works via jump instructions: `JUMP_IF_FALSE` pops a boolean from the stack and jumps past a block if it is false. `JUMP` jumps unconditionally (used for loop back-edges and skipping else branches).

When a lambda is called via the `CALL` instruction, the VM switches to a **tree-walk evaluator** that interprets the lambda's AST body directly in the closure's captured environment. This keeps lambda execution simple and correct without needing a full call-stack redesign.

---

## Example Scripts

The `examples/` folder contains standalone `.rn` programs, each focused on one concept:

- **hello.rn** — the simplest possible program; shows basic `print` and string literals
- **arithmetic.rn** — all arithmetic and comparison operators; demonstrates integer division and multi-argument print
- **variables.rn** — declaring typed variables, reassignment, and block scoping (a variable declared inside `{}` does not exist outside it)
- **control_flow.rn** — `if / else if / else` chains, `while` loops, and logical NOT (`!`)
- **lambdas.rn** — defining anonymous functions, storing them in variables, calling them, single vs multi-parameter, boolean-returning lambdas
- **closures.rn** — how lambdas capture outer variables at definition time (capture by value), and how changing the outer variable afterwards does not affect the already-captured closure
- **pipe.rn** — the `|>` operator chaining values through a series of transformations left-to-right instead of nesting calls
- **demo.rn** — everything together in one script: variables, arithmetic, control flow, loops, pipe chains, lambdas, and closures

---

## Project Structure

```
runescript/
├── src/
│   ├── RuneScript.java     ← the entire implementation (one file)
│   └── runescript.jar      ← compiled and packaged JAR
├── examples/               ← sample .rn programs
├── docs/                   ← online documentation source
├── build.sh                ← build script (Linux/Mac)
├── build.bat               ← build script (Windows)
├── manifest.txt            ← JAR entry point declaration
└── NOTES.md                ← this file
```

### Building from source

**Linux / Mac:**
```bash
bash build.sh
```

**Windows:**
```bat
build.bat
```

Both scripts compile `src/RuneScript.java` and package all `.class` files into `src/runescript.jar`. Java 21 or higher is required.

---

## Running Programs

```bash
# Run a script file
java -jar src/runescript.jar examples/demo.rn

# Start the interactive REPL
java -jar src/runescript.jar
```

### REPL (interactive mode)

When started with no arguments, RuneScript drops into a line-by-line prompt. Variable state persists across lines — a variable declared on one line is available on the next. This lets you experiment incrementally without writing a full file.

```
rune> let x: int = 10;
rune> x = x + 5;
rune> print(x);
15
```

### Debug flags

These flags pause the pipeline and print what the compiler is doing at that stage:

```bash
java -jar src/runescript.jar --emit-tokens program.rn
```
Prints every token the lexer produced — type, text, and line number. Useful for checking that the lexer is reading your syntax correctly.

```bash
java -jar src/runescript.jar --emit-ast program.rn
```
Prints the Abstract Syntax Tree — the structured representation of your program after parsing. Shows how the parser interpreted operator precedence and nesting.

```bash
java -jar src/runescript.jar --emit-bytecode program.rn
```
Prints the bytecode instructions generated for each statement. Shows exactly what the VM will execute, including stack operations, jump offsets, and constant indices.

---

## Current Limitations

- **No named functions** — there is no `fun` or `def` keyword. Only lambdas (anonymous functions stored in variables) are supported.
- **No `&&` / `||`** — logical AND and OR are not implemented. Use nested `if` statements instead.
- **No arrays or collections** — only scalar values (`int`, `bool`, `string`).
- **No string operations** — strings can be printed and stored but cannot be concatenated or manipulated.
- **255-constant limit** — the constant pool and local variable slots use single bytes, so programs are limited to 255 unique constants and 255 local variables. Fine for learning programs.
- **Jump range limit** — jump offsets are two-byte signed integers, limiting the distance a jump can span. Very long functions or deeply nested blocks could hit this ceiling.
- **No error recovery** — the compiler stops at the first error. There is no attempt to continue and report multiple errors at once.
- **Column numbers in errors** — errors report line numbers but not column positions within the line.

---

## Classes Inside RuneScript.java

| Class / Interface | Role |
|---|---|
| `RuneScript` | Entry point; routes to file, REPL, or debug modes |
| `Token` | A single lexed unit (type + text + line number) |
| `TokenType` | Enum of all token kinds |
| `Expr` | Sealed interface for all expression AST nodes |
| `Stmt` | Sealed interface for all statement AST nodes |
| `Lexer` | Converts source text → token list |
| `Parser` | Converts token list → AST |
| `Type` | Sealed interface for the type system (`IntType`, `BoolType`, etc.) |
| `Resolver` | Type-checks and validates the AST |
| `Instruction` | Sealed interface for bytecode instruction types |
| `Chunk` | Holds a bytecode sequence and its constant pool |
| `LambdaTemplate` | Compile-time lambda description (params + body AST + captured names) |
| `LambdaValue` | Runtime closure (params + body AST + captured variable values) |
| `BytecodeEmitter` | Compiles AST → bytecode `Chunk` |
| `VM` | Executes bytecode; also tree-walks lambda bodies |
| `RuneScriptInterpreter` | Orchestrates the full pipeline for a single run |

---

## Key Design Decisions

**Single-file implementation.** Everything is in `src/RuneScript.java`. This makes the project portable and easy to study — you can read the entire implementation in one sitting without jumping between files.

**Sealed interfaces and records for the AST.** Java 21's sealed interfaces make the AST exhaustive and pattern-matchable. Adding a new expression type requires updating the `permits` clause, which forces all switch statements to handle it.

**Stack-based VM.** Simpler to implement than a register-based VM. Local variables live at the bottom of the stack at fixed slots, and expression temporaries stack on top of them. This means no separate register allocation step.

**Closures via tree-walk.** Rather than implementing a full call stack with multiple frames, lambda calls use a lightweight tree-walk evaluator. The main program still compiles and runs as bytecode for performance; lambdas trade some speed for simplicity. For an educational project this is the right tradeoff.

**Capture by value.** Closures snapshot the values of outer variables at the moment the lambda is defined, not references to them. This avoids the complexity of mutable shared state across closures and makes behavior predictable.

**Persistent REPL state.** The REPL keeps a single VM instance alive across lines. Variable values stay on the VM stack between inputs. The bytecode emitter carries forward the list of declared variable names so each new line can reference variables from previous lines.

---

## Benefits of RuneScript

**As a learning tool:**
- You can read the entire compiler in one file — most real compilers span thousands of files across dozens of modules. Here you can trace exactly what happens to your code from text to execution in one sitting.
- Every stage is visible. The `--emit-tokens`, `--emit-ast`, `--emit-bytecode` flags let you pause the pipeline and inspect what the compiler is actually doing at each step.
- The pipe operator teaches functional thinking — writing `x |> f |> g` instead of `g(f(x))` is a different way of thinking about data flow that shows up in languages like Elixir, F#, and modern JavaScript proposals.

**As a project:**
- It demonstrates that building a language is not magic — the full implementation is around 1000 lines of readable code.
- It covers concepts that come up everywhere in software: lexing (used in linters, formatters, syntax highlighters), ASTs (used in Babel, ESLint, TypeScript), bytecode VMs (used in Python, Java, Lua, Ruby).

---

## Why Java

**Practical reasons:**

- **Java 21 sealed interfaces and records** make the AST extremely clean. `sealed interface Expr permits Expr.Binary, Expr.Literal, Expr.Lambda...` means the compiler literally cannot forget to handle a node type — the switch statement becomes exhaustive and the type system enforces it.
- **Single `.java` → single `.jar`** — the entire project compiles to one portable file that runs anywhere Java is installed, on any OS, without installing anything else.
- **Strong static typing** — bugs in the compiler itself get caught at compile time, not at runtime when debugging is harder.
- **Readability** — Java is taught widely so the implementation is accessible to more people, which matters for an educational project.

**Why not other languages:**

- Python would work but the lack of static types makes a compiler implementation harder to reason about.
- C/C++ would give more control but memory management overhead would bury the actual compiler concepts under boilerplate.
- Haskell or OCaml would be elegant for the AST parts but have a steeper learning curve, defeating the educational purpose.

Java hits the sweet spot: typed enough, readable enough, and portable enough for exactly this kind of project.
