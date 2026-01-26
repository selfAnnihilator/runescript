# Language Guide

This document describes the syntax and features of the RuneScript programming language.

## Basic Syntax

### Variables

Variables in RuneScript are declared using the `=` operator:

```runescript
name = "RuneScript"
version = 1.0
active = true
```

RuneScript uses dynamic typing, so you don't need to declare variable types explicitly.

### Functions

Functions are defined using the arrow syntax:

```runescript
add = (a, b) -> a + b
greet = (name) -> "Hello, " + name + "!"
```

You can also define multiline functions:

```runescript
calculateArea = (width, height) -> {
  area = width * height
  return area
}
```

### Control Structures

#### Conditionals

```runescript
if (condition) {
  // do something
} else {
  // do something else
}

// Ternary operator
result = if (x > 0) then x else -x
```

#### Loops

```runescript
// For loop
for (i in range(0, 10)) {
  println(i)
}

// While loop
counter = 0
while (counter < 5) {
  println(counter)
  counter = counter + 1
}
```

## Data Types

### Numbers

RuneScript supports integers and floating-point numbers:

```runescript
integer = 42
float = 3.14
negative = -10
```

### Strings

Strings are enclosed in double quotes:

```runescript
text = "Hello, World!"
multiline = """
This is a
multiline string
"""
```

### Lists

Lists are ordered collections of values:

```runescript
numbers = [1, 2, 3, 4, 5]
mixed = [1, "hello", true, 3.14]
```

### Maps

Maps store key-value pairs:

```runescript
person = {
  "name": "Alice",
  "age": 30,
  "active": true
}
```

## The Pipe Operator (`|>`)

One of RuneScript's most powerful features is the pipe operator `|>`, which allows you to chain operations together in a readable way.

### Basic Usage

Instead of nesting function calls:

```runescript
// Without pipe operator (harder to read)
result = sum(filter(map(numbers, x -> x * 2), x -> x > 10))
```

You can use the pipe operator to chain operations:

```runescript
// With pipe operator (more readable)
result = numbers 
  |> map(x -> x * 2) 
  |> filter(x -> x > 10) 
  |> sum()
```

### How It Works

The pipe operator takes the value on the left and passes it as the first argument to the function on the right:

```runescript
// These are equivalent:
value |> func(arg)
func(value, arg)
```

### Practical Examples

Processing a list of numbers:

```runescript
numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

// Find even numbers, double them, and sum the results
result = numbers
  |> filter(x -> x % 2 == 0)  // [2, 4, 6, 8, 10]
  |> map(x -> x * 2)          // [4, 8, 12, 16, 20]
  |> sum()                    // 60
```

Processing text:

```runescript
sentence = "the quick brown fox jumps over the lazy dog"

words = sentence
  |> split(" ")               // Split into words
  |> map(word -> capitalize(word))  // Capitalize each word
  |> join(" ")                // Join back into a sentence
// Result: "The Quick Brown Fox Jumps Over The Lazy Dog"
```

## Modules

You can organize your code into modules:

```runescript
// math_utils.rn
export add = (a, b) -> a + b
export multiply = (a, b) -> a * b

// main.rn
import { add, multiply } from "./math_utils.rn"

result = add(5, 3) |> multiply(2)  // (5 + 3) * 2 = 16
```

## Error Handling

RuneScript provides basic error handling:

```runescript
try {
  riskyOperation()
} catch (error) {
  println("An error occurred: " + error.message)
} finally {
  println("Cleanup code here")
}
```