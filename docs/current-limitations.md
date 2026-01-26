# Current Limitations

This document outlines the current limitations of the RuneScript compiler implementation. These are known issues that would be addressed in a production compiler but are acceptable for this educational reference implementation.

## Known Limitations

### 1. Numeric Type Conversion Issues
- **Issue**: Numbers may not display correctly due to internal conversion issues
- **Example**: `print(42);` might print `null` instead of `42`
- **Status**: Reference implementation limitation
- **Workaround**: Use string literals for reliable output

### 2. Variable Scoping Runtime Issues
- **Issue**: Variable scoping has some runtime issues
- **Example**: Complex variable assignments may not work as expected
- **Status**: Implementation limitation in current VM design
- **Workaround**: Use simple variable declarations and assignments

### 3. Multi-Argument Function Support
- **Issue**: Print function only supports single arguments
- **Example**: `print("Value:", 42);` is not supported
- **Status**: Current implementation only supports single argument functions
- **Workaround**: Use separate print statements: `print("Value:"); print(42);`

### 4. Complex Expression Handling
- **Issue**: Some complex expressions may cause runtime errors
- **Example**: Nested function calls or complex arithmetic
- **Status**: VM stack management needs refinement
- **Workaround**: Use simpler expressions and break down complex operations

### 5. Error Message Precision
- **Issue**: Some error messages may not pinpoint exact locations
- **Status**: Error recovery mechanism could be improved
- **Workaround**: Carefully review code around reported line numbers

## Implementation Notes

### Stack-Based VM Design
- The current VM uses a unified stack for both operands and local variables
- This can cause conflicts between computation stack and variable storage
- A production implementation would separate these concerns

### Type Conversion
- The current implementation has issues with Double to Integer conversion
- Numbers from the lexer come as Double values (e.g., 5.0)
- Conversion logic needs refinement for consistent behavior

### Memory Management
- No garbage collection implemented
- Limited memory management capabilities
- Suitable for small programs but not scalable

## Educational Value Despite Limitations

These limitations are intentional for an educational project because they demonstrate:

1. **Real-world challenges** in compiler implementation
2. **Trade-offs** between simplicity and functionality
3. **Areas for improvement** in language design
4. **Problem-solving opportunities** for students

## Future Enhancements

A production version would include:

- [ ] Fixed numeric type conversion
- [ ] Improved variable scoping and storage
- [ ] Multi-argument function support
- [ ] Enhanced error reporting
- [ ] Better memory management
- [ ] Garbage collection
- [ ] Standard library expansion
- [ ] Performance optimizations

## Working Features

Despite these limitations, the following features work reliably:

- ✅ Basic string printing
- ✅ Simple variable declarations
- ✅ Basic control flow (if/else, while)
- ✅ Tokenization and parsing
- ✅ Type checking (basic)
- ✅ Bytecode generation
- ✅ Simple arithmetic (when values work correctly)

## Conclusion

This reference implementation demonstrates the complete compiler pipeline while acknowledging realistic limitations that occur in actual compiler development. Students can learn from both the successes and the challenges presented in this implementation.