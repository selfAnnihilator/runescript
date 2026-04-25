# Examples

All examples on this page are complete, runnable programs. Save any snippet to a `.rn` file and run it with:

```bash
java -jar src/runescript.jar yourfile.rn
```

Examples that already exist in the repository are noted with their path.

---

## Hello World

```
print("Hello, RuneScript!");
```

**Output:**
```
Hello, RuneScript!
```

`print` is a built-in function, not a keyword. It accepts any number of arguments and prints them concatenated, followed by a newline. Semicolons are required at the end of every statement.

---

## Variables and Types

```
let x: int = 42;
let name: string = "Alice";
let flag: bool = true;

print(x);
print(name);
print(flag);

x = x + 1;
print(x);

{
    let y: int = 99;
    print(y);
}
```

**Output:**
```
42
Alice
true
43
99
```

The type annotation (`: int`, `: string`, `: bool`) is not optional — omitting it is a compile error. Reassignment is valid as long as the new value matches the declared type. Variables declared inside a `{}` block are local to that block; `y` is not accessible outside the inner braces.

---

## Arithmetic

```
let a: int = 20;
let b: int = 6;

print(a + b);
print(a - b);
print(a * b);
print(a / b);
print(a + b * 2);
print((a + b) * 2);
print(-a);
```

**Output:**
```
26
14
120
3
32
52
-20
```

Integer division truncates toward zero: `20 / 6` is `3`, not `3.333`. Operator precedence follows standard math rules — `*` and `/` bind tighter than `+` and `-`. Parentheses override precedence. There is no `%` (modulo) operator.

---

## String Operations

```
let s: string = "hello";
let t: string = " world";

print(s + t);
print(len(s));
print(substr(s, 1, 3));
print("count: " + 42);
```

**Output:**
```
hello world
5
el
count: 42
```

`+` concatenates strings. `len(s)` returns the number of characters. `substr(s, start, end)` returns a substring from index `start` up to but **not including** `end` — so `substr("hello", 1, 3)` gives `"el"`, not `"ell"`. Integer-to-string coercion happens automatically when one operand of `+` is a string, so `"count: " + 42` works without any explicit cast.

!!! warning
    There are no string escape sequences. Writing `"\n"` in source code produces a two-character string (backslash + `n`), not a newline. To print a newline you must use a separate `print()` call.

---

## Control Flow

### if / else if / else

```
let score: int = 85;

if (score >= 90) {
    print("A");
} else if (score >= 80) {
    print("B");
} else if (score >= 70) {
    print("C");
} else {
    print("F");
}
```

**Output:**
```
B
```

The condition expression must have type `bool`. Writing `if (1)` or `if (score)` is a compile-time type error. `else if` is not a separate keyword — it is `else` followed by another `if` statement.

### while Loop

```
let i: int = 1;
while (i <= 5) {
    print(i);
    i = i + 1;
}

let sum: int = 0;
let n: int = 10;
while (n > 0) {
    sum = sum + n;
    n = n - 1;
}
print(sum);
```

**Output:**
```
1
2
3
4
5
55
```

There is no `++` or `+=`. Increment is written `i = i + 1`. The second loop sums the integers from 1 to 10 (Gauss's formula gives 55).

### for Loop

```
for (let i: int = 0; i < 5; i = i + 1) {
    print(i);
}

let sum: int = 0;
for (let i: int = 1; i <= 10; i = i + 1) {
    sum = sum + i;
}
print(sum);
```

**Output:**
```
0
1
2
3
4
55
```

The initializer variable (`let i`) is scoped to the loop body — it does not exist after the loop ends. All three parts of the `for` header (initializer, condition, increment) are optional.

**Nested loops:**

```
for (let i: int = 0; i < 3; i = i + 1) {
    for (let j: int = 0; j < 3; j = j + 1) {
        print(i * 3 + j);
    }
}
```

**Output:**
```
0
1
2
3
4
5
6
7
8
```

---

## Functions

### Basic Functions

```
fun add(a: int, b: int) -> int {
    return a + b;
}

fun greet(name: string) -> string {
    return "Hello, " + name;
}

print(add(3, 4));
print(greet("Bob"));
```

**Output:**
```
7
Hello, Bob
```

All parameter types and the return type are required in the signature. The resolver verifies that every `return` expression matches the declared return type — returning a `string` from a `-> int` function is a compile error. Calling with the wrong number of arguments is also a compile error.

### Recursive Functions

```
fun factorial(n: int) -> int {
    if (n <= 1) { return 1; }
    return n * factorial(n - 1);
}

print(factorial(5));
print(factorial(10));
```

**Output:**
```
120
3628800
```

Recursive calls work because the function name is available inside its own body. There is no tail-call optimization — very deep recursion will overflow the Java call stack.

### Functions Returning Structs

```
struct Point {
    x: int,
    y: int
}

fun makePoint(a: int, b: int) -> Point {
    return Point { x: a, y: b };
}

let q: Point = makePoint(3, 4);
print(q.x);
print(q.y);
```

**Output:**
```
3
4
```

Struct types are first-class in the type system. A function can declare a struct type as its return type and the resolver will verify it.

---

## Arrays

```
let nums: int[] = [1, 2, 3, 4, 5];

print(len(nums));   // 5
print(nums[0]);     // 1
print(nums[4]);     // 5

push(nums, 6);
print(len(nums));   // 6
print(nums[5]);     // 6

let words: string[] = ["hello", "world"];
print(words[0] + " " + words[1]);

let sum: int = 0;
for (let i: int = 0; i < len(nums); i = i + 1) {
    sum = sum + nums[i];
}
print(sum);
```

**Output:**
```
5
1
5
6
6
hello world
21
```

The element type must be declared explicitly (`int[]`, `string[]`, `bool[]`). Indices are zero-based. `push(arr, val)` appends in place and does not return the array. `len(arr)` returns the current element count — it is evaluated fresh each time, so using it as a loop bound after `push()` calls is safe.

!!! warning
    `arr[i] = val` assignment syntax is not supported by the parser. To replace an element at a specific index, you would need to rebuild the array.

---

## Lambdas

### Storing Lambdas in Variables

```
let double = (x -> x * 2);
let square = (x -> x * x);
let add = (a, b -> a + b);

print(double(7));   // 14
print(square(6));   // 36
print(add(3, 4));   // 7
```

**Output:**
```
14
36
7
```

Lambda syntax: `(params -> expr)`. The body is a single expression — no braces, no `return`, no statements. For anything more complex, use a named function.

### Pipe Chains

```
5 |> double |> print();
```

**Output:**
```
10
```

`value |> func(args)` desugars to `func(value, args)`. So `5 |> double` becomes `double(5)`, and `double(5) |> print()` becomes `print(double(5))`. Empty parentheses `()` are required when piping into a named function with no extra arguments.

Step by step for `5 |> double |> print()`:

1. `5 |> double` → `double(5)` → `10`
2. `10 |> print()` → `print(10)` → prints `10`

### Inline Lambda in a Pipe Chain

```
3 |> (x -> x * x) |> (x -> x + 1) |> print();
```

**Output:**
```
10
```

The equivalent imperative version:

```
let a: int = 3 * 3;   // 9
let b: int = a + 1;   // 10
print(b);
```

Pipes let you write transformation chains without naming intermediate values.

---

## Closures

Closures are lambdas that reference variables from the surrounding scope. RuneScript captures by **reference** — if the outer variable changes after the lambda is created, the lambda sees the new value.

```
let base: int = 10;
let add_base = (x -> x + base);

print(add_base(5));   // 15

base = 999;
print(add_base(5));   // 1004, not 15
```

**Output:**
```
15
1004
```

This is the same behavior as closures in JavaScript or Python. Internally, RuneScript wraps the captured variable in an `UpvalueObj` — a mutable reference box shared between the outer scope and the lambda. When `base` is updated, the `UpvalueObj` is updated too, and the lambda reads through it.

**Practical use — building specialized functions:**

```
let tax_rate: int = 18;
let add_tax = (price -> price + price * tax_rate / 100);

print(add_tax(100));   // 118
print(add_tax(200));   // 236
```

**Output:**
```
118
236
```

`add_tax` closes over `tax_rate`. You can create different versions of a computation by changing the captured variable before constructing the lambda.

---

## Structs

### Defining and Instantiating

```
struct Point {
    x: int,
    y: int
}

let p: Point = Point { x: 10, y: 20 };
print(p.x);
print(p.y);

p.x = 99;
print(p.x);
```

**Output:**
```
10
20
99
```

The struct declaration defines the type. The struct literal `Point { x: 10, y: 20 }` creates an instance. Fields are accessed and mutated with `.`.

### Structs in Functions

```
fun sumPoint(pt: Point) -> int {
    return pt.x + pt.y;
}

print(sumPoint(p));   // 99 + 20 = 119
```

**Output:**
```
119
```

Structs can be passed to and returned from functions like any other type.

### Nominal vs. Structural Typing

```
struct Point { x: int, y: int }
struct Dimensions { x: int, y: int }

// These two types have identical fields but different names.
// You CANNOT pass a Dimensions value where a Point is expected.
// This is a compile-time error:

// fun usePoint(p: Point) -> int { return p.x; }
// let d: Dimensions = Dimensions { x: 1, y: 2 };
// usePoint(d);   // ERROR: type mismatch
```

RuneScript uses **nominal typing** for structs: the name is the identity. This is the same model used by Java, C#, and Go — contrast with TypeScript, which uses structural typing (two types are compatible if they have the same shape).

---

## Diagnostic Flags

These flags stop the pipeline early and print internal representation instead of executing the program. They are most useful when learning how the compiler works or debugging unexpected output.

Using `tests/08_functions.rn` as the input:

### --emit-tokens

```bash
java -jar src/runescript.jar --emit-tokens tests/08_functions.rn
```

Produces one token per line: type, lexeme, line, column. Example output snippet:

```
FUN         'fun'       line=1  col=1
IDENTIFIER  'add'       line=1  col=5
LPAREN      '('         line=1  col=8
IDENTIFIER  'a'         line=1  col=9
COLON       ':'         line=1  col=10
IDENTIFIER  'int'       line=1  col=12
...
```

Use this when a parse error references an unexpected token and you want to see exactly what the lexer produced.

### --emit-ast

```bash
java -jar src/runescript.jar --emit-ast tests/08_functions.rn
```

Produces an S-expression representation of the AST. Example output snippet:

```
(fun add [(a int) (b int)] int
  (return (+ a b)))
(fun factorial [(n int)] int
  (if (<= n 1)
    (return 1))
  (return (* n (call factorial [n - 1]))))
```

Use this when a type error references a node you did not expect, or when you want to verify how an expression was parsed.

### --emit-bytecode

```bash
java -jar src/runescript.jar --emit-bytecode tests/08_functions.rn
```

Produces disassembled bytecode, one instruction per line. Example output snippet:

```
0000  MAKE_LAMBDA   0
0002  SET_LOCAL     0
0005  MAKE_LAMBDA   1
0007  SET_LOCAL     1
...
== chunk: add ==
0000  GET_LOCAL     0
0002  GET_LOCAL     1
0004  ADD
0005  RETURN
```

The VM is stack-based: `GET_LOCAL 0` pushes the value of the first local variable onto the operand stack, `GET_LOCAL 1` pushes the second, `ADD` pops both and pushes their sum, `RETURN` pops the result and returns it to the caller's frame. Use this to understand how expressions map to instructions or to debug incorrect runtime results.

---

## Edge Cases and Error Messages

These examples show what happens at the boundaries of the type system and runtime. Knowing the error format helps you read compile and runtime output.

### Type Mismatch

```
let x: int = "hello";
```

**Output:**
```
[line 1, col 5] Error at 'x': Cannot assign string to int
```

Error format: `[line N, col M] Error at 'token': message`. The column points to the token where the error was detected — in this case, the variable name `x` where the assignment is being resolved.

### Undefined Variable

```
print(undefined_var);
```

**Output:**
```
Error: Undefined variable 'undefined_var'
Stack underflow
```

The resolver emits the first line. Because the undefined variable leaves the stack in a bad state, the VM emits `Stack underflow` when it tries to execute `print`. Both lines are expected — the second is a consequence of the first, not a separate bug.

### Wrong Arity

```
fun add(a: int, b: int) -> int {
    return a + b;
}
add(1);
```

**Output:**
```
[line 4, col 1] Error at 'add': Expected 2 arguments but got 1
```

Arity is checked at compile time by the Resolver. The program does not run.

### Division by Zero

```
let x: int = 10;
let y: int = 0;
print(x / y);
```

**Output (stderr):**
```
Runtime error: Division by zero
```

The Resolver cannot evaluate expressions to detect division by zero, so this passes type checking and fails at runtime. The error goes to stderr; there is nothing on stdout. There is no way to catch or recover from this error in a RuneScript program.

### Array Index Out of Bounds

```
let arr: int[] = [1, 2, 3];
print(arr[10]);
```

**Output (stderr):**
```
Runtime error: Array index 10 out of bounds (size 3)
```

Like division by zero, array bounds are checked at runtime only.
