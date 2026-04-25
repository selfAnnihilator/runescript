# RuneScript

RuneScript is a statically-typed scripting language that compiles to custom stack-based bytecode and is executed by a virtual machine — both implemented entirely in a single Java source file (`src/RuneScript.java`). Built as a compiler design project, it walks through every stage of a real compiler: scanning, parsing, type checking, code generation, and execution. It is small enough to read in an afternoon but complete enough to run non-trivial programs.

---

## What Is RuneScript?

RuneScript enforces types at compile time. Every variable must carry an explicit type annotation and an initializer. Every function must declare its parameter types and return type. The compiler rejects mismatches before any code runs, and it reports errors with exact line and column numbers so you know exactly where to look.

Integer arithmetic is exact — there is no `float` or `double` type visible to the programmer. String concatenation works via `+` and coerces integers to strings automatically. Beyond that, there is no implicit behavior: you cannot add a boolean to an integer, pass the wrong number of arguments to a function, or access a field that was never declared.

### The Compilation Pipeline

RuneScript processes source code through five stages in sequence. Each stage collects errors rather than stopping on the first one, so you see all your mistakes at once.

1. **Lexer** — reads the source text character by character and produces a flat list of tokens, recording the line and column of each one.
2. **Parser** — reads the token list and builds an Abstract Syntax Tree (AST) using recursive descent. It synchronizes after errors to keep parsing and collect more.
3. **Resolver** — walks the AST, maintains a scope stack, infers and checks every type, verifies function return types, and checks call arity. If any resolver errors exist, execution does not start.
4. **Bytecode Emitter** — walks the type-annotated AST and emits a sequence of instructions into a `Chunk`. Closures are handled by emitting `CAPTURE_UPVALUE` instructions that box the captured variable into an `UpvalueObj` for by-reference semantics.
5. **VM** — a stack-based interpreter that executes instructions sequentially. It maintains a call stack of frames, handles runtime panics (division by zero, array out-of-bounds) by printing to stderr and halting cleanly.

Because all five stages live in one file, reading `src/RuneScript.java` top to bottom follows the pipeline order exactly. This makes it a useful reference for a compiler design course.

---

## Installation and Usage

### Prerequisites

Java 21 or higher. Verify with:

```bash
java -version
```

No build step is needed for end users — `src/runescript.jar` is pre-built and committed to the repository.

### Running a Script

```bash
java -jar src/runescript.jar myscript.rn
```

Output goes to stdout. Runtime errors (division by zero, array index out of bounds) go to stderr.

### The REPL

Running with no arguments starts an interactive session:

```bash
java -jar src/runescript.jar
```

Each line you type is parsed and executed immediately. Expression statements print their result prefixed with `Returned:`. Variable and function declarations execute silently. Type `exit` to quit.

!!! note
    Multi-line constructs (functions, structs, blocks) must be written on a single line in the REPL. For anything longer than a few tokens, write a `.rn` file instead.

### Diagnostic Flags

These flags print intermediate pipeline output and then stop — they do not execute the program. They are intended for debugging and for studying how source code maps to compiled output.

| Flag | What it prints |
|------|----------------|
| `--emit-tokens` | One token per line with type, lexeme, line, and column |
| `--emit-ast` | S-expression representation of the full AST |
| `--emit-bytecode` | Disassembled bytecode, one instruction per line with operand |

Example:

```bash
java -jar src/runescript.jar --emit-bytecode examples/lambdas.rn
```

---

## Language Reference

### Types

| Type | Description | Example literal |
|------|-------------|-----------------|
| `int` | Whole numbers (no fractional part) | `42`, `-7`, `0` |
| `bool` | Boolean | `true`, `false` |
| `string` | Text | `"hello"` |
| `int[]` | Array of integers | `[1, 2, 3]` |
| `bool[]` | Array of booleans | `[true, false]` |
| `string[]` | Array of strings | `["a", "b"]` |
| struct types | Named records with named fields | depends on declaration |
| function types | First-class lambda values | lambda expressions |

**What does not exist:** there is no `float` or `double` type (floating-point literals are accepted by the lexer but truncated to integers at runtime), no type inference (all `let` declarations must write out the type), and no `null` safety (operating on `nil` values throws a runtime error).

### Variables

```
let name: type = initializer;
```

Both the type annotation and the initializer are required — omitting either is a compile-time error. Reassignment (without `let`) is valid as long as the new value's type matches the declared type. Variables are scoped to their nearest enclosing `{}` block and are not accessible outside it.

```
let x: int = 10;
x = 20;       // valid — same type

{
    let y: int = 99;
    // y is visible here
}
// y is NOT visible here — compile error if referenced
```

### Functions

```
fun name(param: type, ...) -> returnType {
    // body
}
```

All parameter types and the return type are required. The resolver verifies that every `return` statement's expression matches the declared return type. Recursive calls are supported — a function can call itself by name. Named functions are not closures: they do not capture variables from the enclosing scope. Only lambda expressions do that.

```
fun factorial(n: int) -> int {
    if (n <= 1) { return 1; }
    return n * factorial(n - 1);
}
```

### Lambdas and Closures

Lambda syntax:

```
(param -> expr)             // single parameter
(a, b -> expr)              // multiple parameters
(-> expr)                   // no parameters
```

The body is a single expression — there are no braces, no `return`, no statements. Store lambdas in typed variables:

```
let double = (x -> x * 2);
let add = (a, b -> a + b);
print(double(7));    // 14
print(add(3, 4));    // 7
```

**Closures capture by reference.** When a lambda references a variable from the enclosing scope, it captures a live reference to that variable via an `UpvalueObj` box. If the outer variable is mutated after the lambda is created, the lambda sees the updated value:

```
let base: int = 10;
let add_base = (x -> x + base);
print(add_base(5));   // 15
base = 999;
print(add_base(5));   // 1004, not 15
```

This is the same behavior as closures in JavaScript or Python. It is not a bug — it is the definition of a closure.

### Structs

Define a struct type:

```
struct Point {
    x: int,
    y: int
}
```

Instantiate it with named fields:

```
let p: Point = Point { x: 5, y: 10 };
```

Access and mutate fields with `.`:

```
print(p.x);    // 5
p.y = 20;
print(p.y);    // 20
```

Struct matching is **nominal**: two structs with identical field layouts but different names are different types. You cannot use a `Point` where a `Rect` is expected, even if both happen to have `x: int, y: int`.

### Arrays

Declare with an explicit element type:

```
let nums: int[] = [1, 2, 3, 4, 5];
```

Index from zero:

```
print(nums[0]);    // 1
print(nums[4]);    // 5
```

Append a value in place:

```
push(nums, 6);
print(len(nums));  // 6
```

Iterate with a for loop:

```
for (let i: int = 0; i < len(nums); i = i + 1) {
    print(nums[i]);
}
```

!!! warning
    Array element assignment (`arr[i] = val`) is not supported by the parser. To replace an element, rebuild the array or use a helper function.

### Control Flow

**if / else if / else** — the condition must be `bool`:

```
if (score >= 90) {
    print("A");
} else if (score >= 80) {
    print("B");
} else {
    print("F");
}
```

**while** — the condition must be `bool`:

```
let i: int = 0;
while (i < 5) {
    print(i);
    i = i + 1;
}
```

**for** — C-style; the initializer variable is scoped to the loop body:

```
for (let i: int = 0; i < 10; i = i + 1) {
    print(i);
}
```

There is no `break` or `continue`.

### Built-in Functions

| Function | Signature | Description |
|----------|-----------|-------------|
| `print(...)` | variadic | Prints all arguments concatenated, followed by a newline |
| `len(x)` | string or array → int | Length of a string (character count) or array (element count) |
| `substr(s, start, end)` | string, int, int → string | Substring from index `start` up to but not including `end` |
| `push(arr, val)` | array, element → nil | Appends `val` to `arr` in place |

### The Pipe Operator

`value |> func(args)` is syntactic sugar for `func(value, args)` — the left-hand value becomes the first argument. Pipes can be chained:

```
10 |> double() |> print()
// equivalent to: print(double(10))
```

When piping into a function with no additional arguments, empty parentheses are required for named functions:

```
5 |> print()       // prints 5
```

When piping into a lambda, no parentheses are needed:

```
5 |> (x -> x * x) |> print()   // prints 25
```

### Comments

Only single-line comments with `//`. There are no block comments (`/* */`).

---

## Viability and Applications

RuneScript is not a production language. Here is an honest account of what it is and is not good for.

**It is genuinely useful for:**

- **Learning how compilers work.** The single-file design means every stage from character scan to bytecode execution is in one place, in order. You can set a breakpoint in the Emitter and inspect the instructions being generated, then set a breakpoint in the VM and watch them execute.
- **Studying closure and upvalue implementation.** The `UpvalueObj` boxing approach is a textbook reference-closure implementation — the same technique used in Lua. Reading how `CAPTURE_UPVALUE` works and how the VM dereferences `UpvalueObj` boxes on stack access gives you a concrete mental model that applies to many production languages.
- **Running small algorithmic programs.** Anything that can be expressed with integers, strings, booleans, flat arrays, and named functions works. Factorial, Fibonacci, binary search, string reversal, grade calculators, accumulator patterns — all of these work correctly.
- **Exploring language design trade-offs.** Decisions like "no type inference," "expression-only lambdas," and "nominal struct matching" each have a reason. Reading the resolver code and asking "why is this simpler than allowing structural types?" is exactly the kind of thinking a compiler course is trying to build.

**It is not suited for:**

- File I/O — the only output mechanism is `print()`. There is no way to read a file, write a file, or make a network request.
- Floating-point math — all numbers are integers at the VM level.
- Interoperability — there is no FFI, no way to call Java code from a `.rn` file.
- Programs that need complex data structures — nested arrays, maps, sets, and trees are not available.

---

## Known Limitations

### Type System

- **No float type.** Floating-point literals are accepted by the lexer but are truncated to integers at runtime. `3.14` behaves as `3`.
- **No type inference.** Every `let` declaration must include an explicit type annotation. `let x = 5;` is a compile error.
- **Nominal struct matching.** Struct types are identified by name only. Two structs with identical field layouts are not interchangeable.

### Syntax

- **Expression-only lambda body.** Lambda bodies must be a single expression. There is no way to write a multi-statement lambda — use a named function instead.
- **No string escape sequences.** `"\n"` is a two-character string containing a backslash and the letter n, not a newline.
- **No array element assignment.** `arr[i] = val` is not parsed. Arrays can only be grown via `push()`.
- **No `++`, `+=`, `-=`.** Increment and compound assignment operators do not exist. Use `i = i + 1`.

### Runtime

- **Division by zero is a runtime error.** The resolver cannot evaluate constant expressions, so `10 / 0` passes the type checker and panics at runtime.
- **Array out-of-bounds is a runtime error.** Bounds are not checked at compile time.
- **No error handling.** There is no `try`/`catch` or any mechanism to recover from runtime errors. The VM prints to stderr and halts.
