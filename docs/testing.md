# Testing RuneScript

RuneScript has two test layers that complement each other. **Integration tests** run complete `.rn` programs through the full compiler pipeline and compare their output against stored expected values. **Unit tests** target individual compiler stages using JUnit 5 and test them in isolation. Together they give confidence that both the overall behavior and each internal stage are correct.

---

## Integration Tests

### What They Are

The integration tests live in the `tests/` directory. Each test is a pair of files:

- A `.rn` source file containing a complete RuneScript program
- A `.expected` file containing the exact expected output (stdout and stderr combined)

There are 13 feature tests and 2 error tests:

| File | What it exercises |
|------|-------------------|
| `01_hello.rn` | Minimal smoke test — `print("Hello, RuneScript!")` |
| `02_variables.rn` | `let` declarations for `int`/`string`/`bool`, reassignment, block scope |
| `03_arithmetic.rn` | `+`, `-`, `*`, `/`, unary `-`, operator precedence |
| `04_strings.rn` | String concatenation, `len()`, `substr()`, int-to-string coercion via `+` |
| `05_control_flow.rn` | `if`/`else if`/`else`, boolean conditions |
| `06_while_loop.rn` | `while` with counter and accumulator pattern |
| `07_for_loop.rn` | C-style `for`, nested loops |
| `08_functions.rn` | Named functions, typed parameters, recursion (`factorial`) |
| `09_arrays.rn` | Array literals, indexing, `len()`, `push()`, for-loop iteration |
| `10_lambdas.rn` | Lambda assignment, single/multi-param, pipe operator chains |
| `11_closures.rn` | Lambda capturing a mutable outer variable (by-reference semantics) |
| `12_logical_ops.rn` | `&&`, `||`, `!`, short-circuit evaluation |
| `13_structs.rn` | Struct definition, literal instantiation, field access/mutation, passing to/returning from functions |
| `err_01_type_mismatch.rn` | Expected compile-time error: type mismatch message |
| `err_02_undefined_var.rn` | Expected two-line error: undefined variable + stack underflow |

### How the Runner Works

`tests/run_tests.sh` works as follows:

1. For every `.rn` file in `tests/`, it looks for a paired `.expected` file with the same stem.
2. It runs the program: `java -jar src/runescript.jar <file> 2>&1` — the `2>&1` redirect merges stderr into stdout so that error messages appear in the diff.
3. It diffs the actual output against the `.expected` file.
4. It prints `PASS` or `FAIL` per test, shows the unified diff on failure, and exits with status 1 if any test fails.

### Running the Integration Tests

First build the JAR, then run the tests:

```bash
./build.sh
./tests/run_tests.sh
```

If `build.sh` is not executable: `chmod +x build.sh tests/run_tests.sh`.

Expected output when all tests pass:

```
PASS  01_hello.rn
PASS  02_variables.rn
PASS  03_arithmetic.rn
PASS  04_strings.rn
PASS  05_control_flow.rn
PASS  06_while_loop.rn
PASS  07_for_loop.rn
PASS  08_functions.rn
PASS  09_arrays.rn
PASS  10_lambdas.rn
PASS  11_closures.rn
PASS  12_logical_ops.rn
PASS  13_structs.rn
PASS  err_01_type_mismatch.rn
PASS  err_02_undefined_var.rn

Results: 15 passed, 0 failed
```

### Interpreting Failures

When a test fails, the runner prints a unified diff. Lines starting with `<` are what the program actually produced. Lines starting with `>` are what was expected.

```
FAIL  04_strings.rn
< hello world
< 5
< el
< count: 42
---
> hello world
> 5
> el
> count: 42
```

Common causes of failures:

- **Changed error message format** — if you edited how the Resolver or VM formats its error strings, all `err_*.rn` tests will fail. Update the `.expected` files to match.
- **Off-by-one in a runtime result** — check the arithmetic logic in the relevant test program.
- **Stray diagnostic output** — if VM debug prints were accidentally left in, they will appear in the actual output but not in the expected file.

### Adding a New Integration Test

1. Create your program: `tests/NN_name.rn`
2. Capture its output into the expected file:

    ```bash
    java -jar src/runescript.jar tests/NN_name.rn > tests/NN_name.expected 2>&1
    ```

3. Verify the `.expected` content is what you actually want.
4. Commit both files. The runner discovers tests by filename pattern, so no configuration change is needed.

---

## Unit Tests

### What They Are

The unit tests live in `test/` and use JUnit 5. There are four test classes targeting each compiler stage:

**`LexerTest`** — 22 tests

Tests the tokenization stage in isolation by calling `new Lexer(source).scanTokens()` directly.

- Token types for all literals (integers, strings)
- Token types for all keywords (`let`, `if`, `else`, `while`, `for`, `fun`, `return`, `true`, `false`)
- Token types for all operators (arithmetic, comparison, logical, arrow `->`, pipe `|>`)
- Line and column tracking — verifies that the `line` and `col` fields on each token are accurate
- Error collection — unterminated strings and unexpected characters are added to the error list rather than throwing; the test verifies the error list is non-empty and execution continues to the next valid token
- EOF handling — final token is always `EOF`
- Comment skipping — `//` comments produce no tokens

**`ParserTest`** — 16 tests

Tests AST construction by chaining `Lexer → Parser`.

- AST node types for `let` declarations, `if`/`else`, `while`, `for`, function declarations, return statements
- Binary expression nodes for arithmetic and logical operators
- Error recovery — verifies that multiple syntax errors are all reported without the parser crashing

**`ResolverTest`** — 14 tests

Tests the type checker by chaining `Lexer → Parser → Resolver` and inspecting `resolver.getErrors()`.

- Type mismatch detection: assigning a `string` to an `int` variable, assigning a `bool` to an `int`, etc.
- Clean cases: valid assignments pass without errors
- Undefined variable error
- Function return type mismatch
- Arithmetic type rules: `bool + int` is rejected; `string + int` is allowed (coercion)
- Logical operator rules: `&&` and `||` on non-`bool` operands are rejected
- Multiple errors are collected in a single pass — the test verifies `errors.size() == N` rather than just `> 0`

**`VMTest`** — 33 tests

Tests end-to-end execution by running `RuneScriptInterpreter` (the top-level entry point) on short programs and comparing stdout/stderr.

Before each test, `System.out` and `System.err` are redirected to `ByteArrayOutputStream` instances. After each test, they are restored. This means every VM test is a complete pipeline run (Lexer → Parser → Resolver → Emitter → VM) captured in memory.

Coverage:

- Arithmetic: add, subtract, multiply, divide, negate, nested expressions, operator precedence
- Comparison: `<`, `<=`, `>`, `>=`, `==`, `!=`
- Boolean logic: `!`, `&&`, `||`
- Strings: print, concatenation, `len()`, `substr()`
- Variables: declaration, reassignment
- Control flow: `if`/`else`, `while`, `for`
- Functions: calls, return values, recursion
- Arrays: literals, indexing, `len()`, `push()`
- Runtime error: division by zero produces stderr output and does not crash the test runner

### Running the Unit Tests

```bash
./test.sh
```

The script compiles `src/RuneScript.java` into `test-out/`, compiles the four test classes against `lib/junit-standalone.jar`, then runs all classes. If `lib/junit-standalone.jar` is missing, the script prints the `curl` command to download it.

Expected summary output at the end:

```
[         4 containers found      ]
[         4 containers started    ]
[         4 containers successful ]
[         0 containers failed     ]
[        85 tests found           ]
[        85 tests started         ]
[        85 tests successful      ]
[         0 tests failed          ]
```

### How the Unit Tests Are Structured

Each class instantiates the pipeline stage under test directly:

```java
// LexerTest
List<Token> tokens = new Lexer(source).scanTokens();

// ParserTest
List<Token> tokens = new Lexer(source).scanTokens();
List<Stmt> stmts = new Parser(tokens).parse();

// ResolverTest
// ... lex + parse ...
Resolver resolver = new Resolver(stmts);
resolver.resolve();
List<String> errors = resolver.getErrors();

// VMTest (full pipeline via helper)
RuneScriptInterpreter.run(source);
String output = outStream.toString();
```

`VMTest` redirects streams in a `@BeforeEach` / `@AfterEach` pair so each test starts clean:

```java
@BeforeEach
void captureOutput() {
    outStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outStream));
}

@AfterEach
void restoreOutput() {
    System.setOut(originalOut);
}
```

### Adding a Unit Test

To add a test to an existing class, add a `@Test` method and re-run `./test.sh`. JUnit's `ConsoleLauncher` discovers methods by annotation within the class, so no configuration change is needed.

To add a new test class:

1. Create `test/YourClassName.java` following the same pattern as the existing classes.
2. Add `--select-class=YourClassName` to the `java -jar lib/junit-standalone.jar ...` command in `test.sh`.

---

## What the Tests Do Not Cover

Being explicit about gaps helps contributors know where to add coverage:

- **Array element assignment** — `arr[i] = val` is not supported by the parser, so there is no test for it. If support is ever added, new tests will be needed in `ParserTest`, `ResolverTest`, and `VMTest`.
- **REPL mode** — the interactive session is not tested programmatically. It can only be verified by running `java -jar src/runescript.jar` manually.
- **Diagnostic flag output** — `--emit-tokens`, `--emit-ast`, and `--emit-bytecode` have no integration tests that verify their output format. Format changes would not be caught.
- **Struct bytecode paths** — `13_structs.rn` provides end-to-end coverage, but the individual `MAKE_STRUCT`, `GET_FIELD`, `SET_FIELD` instructions are not tested in isolation in `VMTest`.
- **Property-based or fuzz tests** — there are none. The test suite is entirely example-based.
