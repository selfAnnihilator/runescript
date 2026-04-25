# RuneScript

RuneScript is a statically-typed scripting language that compiles to custom stack-based bytecode and runs on a VM — both written in a single Java file. It started as a compiler design project and grew into a fairly complete little language with functions, closures, structs, arrays, and a pipe operator.

**[Full documentation →](https://selfannihilator.github.io/runescript/)**

---

## Getting Started

You need Java 21 or higher. Check with `java -version`.

The JAR is pre-built in the repo — no compilation needed:

```bash
git clone https://github.com/selfAnnihilator/runescript.git
cd runescript
java -jar src/runescript.jar examples/demo.rn
```

Or start the REPL:

```bash
java -jar src/runescript.jar
```

---

## A Quick Look at the Language

```
// Variables require explicit types
let name: string = "Alice";
let score: int = 95;

// Functions with typed signatures
fun factorial(n: int) -> int {
    if (n <= 1) { return 1; }
    return n * factorial(n - 1);
}
print(factorial(10));   // 3628800

// Closures capture by reference
let rate: int = 10;
let apply_rate = (x -> x + x * rate / 100);
print(apply_rate(200));   // 220
rate = 20;
print(apply_rate(200));   // 240 — closure sees the update

// Pipe operator chains function calls
"hello world" |> len() |> print();   // 11

// Structs
struct Point { x: int, y: int }
let p: Point = Point { x: 3, y: 4 };
print(p.x);   // 3
```

---

## Language Features

- **Types**: `int`, `bool`, `string`, `int[]`, `bool[]`, `string[]`, named structs, first-class functions
- **Control flow**: `if`/`else if`/`else`, `while`, C-style `for`
- **Functions**: typed parameters and return types, recursion
- **Lambdas**: `(x -> expr)` syntax, expression body only
- **Closures**: by-reference capture via upvalue boxing
- **Structs**: nominal typing, field access and mutation
- **Arrays**: literals, indexing, `push()`, `len()`
- **Built-ins**: `print()`, `len()`, `substr()`, `push()`
- **Pipe operator**: `value |> func(args)` → `func(value, args)`
- **Error reporting**: line and column numbers, all errors collected before halting

### Diagnostic flags

```bash
java -jar src/runescript.jar --emit-tokens script.rn    # tokenization
java -jar src/runescript.jar --emit-ast script.rn       # AST
java -jar src/runescript.jar --emit-bytecode script.rn  # disassembled bytecode
```

---

## Building from Source

```bash
bash build.sh
```

Compiles `src/RuneScript.java` and packages `src/runescript.jar`.

---

## Running Tests

Integration tests (15 programs with expected output):

```bash
bash build.sh && bash tests/run_tests.sh
```

Unit tests (JUnit 5, 75 tests across Lexer / Parser / Resolver / VM):

```bash
bash test.sh
```

See the [Testing docs](https://selfannihilator.github.io/runescript/testing/) for a full breakdown of what each test covers.

---

## Project Structure

```
src/RuneScript.java   — entire compiler and VM in one file
src/runescript.jar    — pre-built JAR
tests/                — integration tests (.rn + .expected pairs)
test/                 — JUnit unit tests
examples/             — sample programs
docs/                 — documentation source (deployed via GitHub Pages)
build.sh              — builds the JAR
test.sh               — runs JUnit tests
```

The compiler pipeline in order: **Lexer → Parser → Resolver → Bytecode Emitter → VM**. Each stage is a distinct class in `RuneScript.java`, top to bottom.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Bug fixes, new examples, and additional tests are all welcome.

## License

MIT — see [LICENSE](LICENSE).
