# RuneScript — Upgrade Plan

Phased roadmap to address current limitations. Each phase is self-contained and ordered by difficulty.

---

## Phase 1 — Quick Wins ✅

### 1.1 Logical AND / OR (`&&`, `||`) ✅
- Add `AND`, `OR` to `TokenType`; lex `&&` and `||` in `Lexer.scanToken()`
- Add `Expr.Logical` AST node (left, operator, right)
- Parse between equality and comparison precedence
- Resolver: both sides must be `bool`, result is `bool`
- Emit short-circuit: `&&` uses `JUMP_IF_FALSE` without pop; `||` uses `JUMP_IF_TRUE` without pop

### 1.2 Column numbers in errors ✅
- Add `col` field to `Token`; track it in lexer alongside `line`
- Pass `col` through all `error()` calls in parser and resolver
- Output format: `[line 4, col 12] Error: ...`

---

## Phase 2 — String Operations ✅

### 2.1 String concatenation ✅
- Overload `+` in resolver: if either operand is `string`, result is `string`
- In VM `ADD` handler: if either value is a string, call `.toString()` on both and concatenate
- No new opcode needed

### 2.2 String built-ins ✅
- `len(s)` — returns string length as `int`
- `substr(s, start, end)` — returns substring
- Implement as special-cased built-in calls in resolver and VM (same pattern as `print`)

---

## Phase 3 — Named Functions ✅

### 3.1 Function declarations ✅
- Add `FUN` token; add `Stmt.Function` AST node (name, params, return type, body)
- Parser: `fun name(params) -> returnType { body }`
- Resolver: add function name to scope before body; validate param and return types
- Emitter: compile body into its own `Chunk`, store as `LambdaTemplate` constant, emit `MAKE_LAMBDA` + `SET_LOCAL`

### 3.2 Return statement + proper call stack ✅
- Add `RETURN` token; add `Stmt.Return` AST node
- Resolver: check returned expression type matches declared return type
- Emitter: emit `RETURN` opcode
- VM: named functions use a tree-walk statement evaluator (`executeBody`/`executeStmt`) with a `ReturnValue` exception for early return; consistent with existing lambda execution model

---

## Phase 4 — Expand VM Capacity ✅

### 4.1 Two-byte constant and local indices ✅
- Change constant pool reads from 1-byte to 2-byte throughout `VM` and `BytecodeEmitter`
- Raises limit from 255 to 65,535 constants and locals
- No AST or feature changes — purely mechanical

---

## Phase 5 — Arrays ✅

### 5.1 Array type and literals ✅
- Add `ArrayType(Type element)` to the `Type` sealed interface
- Syntax: `let nums: int[] = [1, 2, 3];`
- Add `Expr.ArrayLiteral` and `Expr.Index` (`arr[i]`) AST nodes
- New opcodes: `MAKE_ARRAY count`, `GET_INDEX`, `SET_INDEX`
- VM stores arrays as `ArrayList<Object>`

### 5.2 Array built-ins ✅
- `len(arr)` — reuse built-in from Phase 2
- `push(arr, val)` — appends value in place

---

## Phase 6 — Error Recovery ✅

### 6.1 Parser synchronization ✅
- On parse error, call `synchronize()` instead of throwing — advance to next statement boundary (`;`, `let`, `if`, `while`, `}`)
- Collect all errors into a list; print them all after parsing
- Execution still blocked if any errors exist, but user sees all mistakes at once

### 6.2 Full-pipeline error collection ✅
- Lexer collects unexpected characters and unterminated strings into a list with line/col
- Resolver collects all type errors into a list instead of printing inline
- `RuneScriptInterpreter` uses a shared `reportErrors()` helper — prints all errors per stage in batch, stops pipeline on any errors
- `Type` records given proper `toString()` so error messages show `int`/`bool`/`string`

---

## Summary

| Phase | Feature | Difficulty | Depends on |
|---|---|---|---|
| 1.1 | `&&` / `\|\|` | Low | — |
| 1.2 | Column numbers in errors | Low | — |
| 2.1 | String concatenation | Low | — | ✅
| 2.2 | String built-ins | Medium | 2.1 | ✅
| 3.1 | Named functions | Medium | — | ✅
| 3.2 | Return + call stack | High | 3.1 | ✅
| 4.1 | Two-byte indices | Low | — |
| 5.1 | Arrays | High | 4.1 | ✅
| 5.2 | Array built-ins | Low | 5.1 | ✅
| 6.1 | Parser error recovery | Medium | — | ✅
| 6.2 | Full-pipeline error collection | Medium | 6.1 | ✅

**Recommended order:** 1.1 → 1.2 → 4.1 → 2.1 → 2.2 → 6.1 → 6.2 → 3.1 → 3.2 → 5.1 → 5.2

---

## Phase 7 — For Loop ✅

### 7.1 `for` loop ✅
- Add `FOR` token; syntax: `for (init; condition; increment) { body }`
- Add `Stmt.For` AST node (initializer, condition, increment, body)
- Parser: parse `for` with optional init/condition/increment (like C-style for)
- Resolver: resolve each part in its own scope
- Emitter: compile as `init → [condition → JUMP_IF_FALSE exit → body → increment → LOOP]`
- No new opcodes needed — reuse `JUMP_IF_FALSE`, `JUMP`, `LOOP`

---

## Phase 8 — Closures ✅

### 8.1 Upvalue capture ✅
- Add `Upvalue` concept: lambdas/functions capture variables from enclosing scopes by reference
- Add `Expr.Upvalue` or `upvalueIndex` field on `LambdaTemplate`
- Resolver: when a variable is referenced inside a lambda but declared outside, mark it as an upvalue
- Emitter: emit `CAPTURE_UPVALUE` for each captured variable; store upvalue list on `LambdaValue`
- VM: `MAKE_LAMBDA` closes over current stack slots into `UpvalueObj`; `GET_UPVALUE` / `SET_UPVALUE` opcodes

---

## Phase 9 — Structs ✅

### 9.1 Struct declarations ✅
- Add `STRUCT` token; syntax: `struct Point { x: int, y: int }`
- Add `Stmt.Struct` AST node (name, fields map)
- Resolver: register struct type; field access (`p.x`) resolves to field type
- Emitter/VM: structs stored as `HashMap<String, Object>`; new opcodes `MAKE_STRUCT`, `GET_FIELD`, `SET_FIELD`

### 9.2 Struct instantiation and field access ✅
- Syntax: `let p: Point = Point { x: 1, y: 2 };`
- Add `Expr.StructLiteral`, `Expr.FieldAccess`, `Expr.FieldAssign` AST nodes
- Resolver: verify field names and types match struct declaration
- VM: `MAKE_STRUCT` pops N field values and builds the map; `GET_FIELD`/`SET_FIELD` for field access

---

## Phase 10 — Test Suite ✅

### 10.1 Integration tests (shell) ✅
- Write a `tests/` directory with `.rune` source files and corresponding `.expected` output files
- Write a `run_tests.sh` script: compile + run each `.rune` file, diff actual vs expected output
- Cover: variables, control flow, functions, arrays, strings, error cases

### 10.2 Unit tests (Java) ✅
- Add JUnit 5 dependency (or plain Java test runner)
- Unit-test `Lexer` (token types, line/col tracking, error collection)
- Unit-test `Parser` (AST shape for each construct)
- Unit-test `Resolver` (type errors, scope errors)
- Unit-test `VM` (arithmetic, stack correctness, runtime errors)

---

## Phase 11 — Cleanup ✅

### 11.1 Remove duplicate root-level source files ✅
- Delete legacy `.java` files in the project root (`VM.java`, `Lexer.java`, `Parser.java`, `Resolver.java`, `BytecodeEmitter.java`, `Chunk.java`, `Token.java`, `Type.java`, `Instruction.java`, `ast.java`, `Interpreter.java`)
- All active code lives in `src/RuneScript.java`; root files are stale copies from early development
- Update build script / `Makefile` if paths reference root files

---

## Updated Summary

| Phase | Feature | Difficulty | Depends on |
|---|---|---|---|
| 7.1 | `for` loop | Low | — | ✅
| 8.1 | Closures / upvalues | High | 3.1, 3.2 | ✅
| 9.1 | Struct declarations | Medium | — | ✅
| 9.2 | Struct instantiation + field access | Medium | 9.1 | ✅
| 10.1 | Integration tests (shell) | Low | — | ✅
| 10.2 | Unit tests (Java / JUnit) | Medium | — | ✅
| 11.1 | Remove duplicate root-level files | Low | — | ✅

**Recommended order:** 7.1 → 11.1 → 10.1 → 10.2 → 8.1 → 9.1 → 9.2
