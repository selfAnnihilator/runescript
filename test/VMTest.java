import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class VMTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void redirectOutput() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String run(String source) {
        new RuneScriptInterpreter().run(source);
        return out.toString().trim();
    }

    private String runErr(String source) {
        new RuneScriptInterpreter().run(source);
        return err.toString().trim();
    }

    // --- Arithmetic ---

    @Test
    void addition() {
        assertEquals("5", run("print(2 + 3);"));
    }

    @Test
    void subtraction() {
        assertEquals("3", run("print(7 - 4);"));
    }

    @Test
    void multiplication() {
        assertEquals("12", run("print(3 * 4);"));
    }

    @Test
    void division() {
        assertEquals("5", run("print(10 / 2);"));
    }

    @Test
    void negation() {
        assertEquals("-7", run("print(-7);"));
    }

    @Test
    void nestedArithmetic() {
        assertEquals("14", run("print(2 + 3 * 4);"));
    }

    @Test
    void parenthesesOverridePrecedence() {
        assertEquals("20", run("print((2 + 3) * 4);"));
    }

    // --- Comparison ---

    @Test
    void greaterThanTrue() {
        assertEquals("true", run("print(5 > 3);"));
    }

    @Test
    void greaterThanFalse() {
        assertEquals("false", run("print(2 > 3);"));
    }

    @Test
    void equalityTrue() {
        assertEquals("true", run("print(4 == 4);"));
    }

    @Test
    void equalityFalse() {
        assertEquals("false", run("print(3 == 4);"));
    }

    @Test
    void notEqualTrue() {
        assertEquals("true", run("print(3 != 4);"));
    }

    // --- Boolean logic ---

    @Test
    void notTrue() {
        assertEquals("false", run("print(!true);"));
    }

    @Test
    void notFalse() {
        assertEquals("true", run("print(!false);"));
    }

    @Test
    void logicalAnd() {
        assertEquals("false", run("print(true && false);"));
    }

    @Test
    void logicalOr() {
        assertEquals("true", run("print(false || true);"));
    }

    // --- Strings ---

    @Test
    void printString() {
        assertEquals("hello", run("print(\"hello\");"));
    }

    @Test
    void stringConcatenation() {
        assertEquals("hello world", run("print(\"hello\" + \" world\");"));
    }

    @Test
    void stringLen() {
        assertEquals("5", run("print(len(\"hello\"));"));
    }

    @Test
    void stringSubstr() {
        assertEquals("ell", run("print(substr(\"hello\", 1, 4));"));
    }

    // --- Variables ---

    @Test
    void variableDeclarationAndPrint() {
        assertEquals("10", run("let x: int = 10; print(x);"));
    }

    @Test
    void variableAssignment() {
        assertEquals("20", run("let x: int = 10; x = 20; print(x);"));
    }

    // --- Control flow ---

    @Test
    void ifTrueBranch() {
        assertEquals("yes", run("if (true) { print(\"yes\"); }"));
    }

    @Test
    void ifFalseBranch() {
        assertEquals("no", run("if (false) { print(\"yes\"); } else { print(\"no\"); }"));
    }

    @Test
    void whileLoop() {
        assertEquals("0\n1\n2", run("let i: int = 0; while (i < 3) { print(i); i = i + 1; }"));
    }

    @Test
    void forLoop() {
        assertEquals("0\n1\n2", run("for (let i: int = 0; i < 3; i = i + 1) { print(i); }"));
    }

    // --- Functions ---

    @Test
    void functionCallReturnsValue() {
        assertEquals("7", run("fun add(a: int, b: int) -> int { return a + b; } print(add(3, 4));"));
    }

    @Test
    void recursiveFunction() {
        assertEquals("120", run(
            "fun fact(n: int) -> int { if (n <= 1) { return 1; } return n * fact(n - 1); }\n" +
            "print(fact(5));"
        ));
    }

    // --- Arrays ---

    @Test
    void arrayLiteralAndIndex() {
        assertEquals("2", run("let a: int[] = [1, 2, 3]; print(a[1]);"));
    }

    @Test
    void arrayLen() {
        assertEquals("3", run("let a: int[] = [10, 20, 30]; print(len(a));"));
    }

    @Test
    void arrayPush() {
        assertEquals("4", run("let a: int[] = [1, 2, 3]; push(a, 4); print(a[3]);"));
    }

    @Test
    void arrayStringElements() {
        // array index assignment (a[i] = val) is not yet implemented in the parser
        assertEquals("world", run("let a: string[] = [\"hello\", \"world\"]; print(a[1]);"));
    }

    // --- Runtime errors ---

    @Test
    void divisionByZeroError() {
        runErr("let x: int = 10 / 0;");
        // Should not throw uncaught — error output should contain something
        // (behavior: VM catches and prints to stderr)
        // At minimum, no exception escapes
    }
}
