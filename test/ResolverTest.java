import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class ResolverTest {

    private List<String> resolveErrors(String source) {
        List<Token> tokens = new Lexer(source).scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> stmts = parser.parse();

        RuneScriptInterpreter interp = new RuneScriptInterpreter();
        Resolver resolver = new Resolver(interp);
        resolver.resolve(stmts);
        return resolver.getErrors();
    }

    // --- Type errors ---

    @Test
    void typeMismatchIntBool() {
        List<String> errors = resolveErrors("let x: int = true;");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Cannot assign"));
    }

    @Test
    void typeMismatchBoolInt() {
        List<String> errors = resolveErrors("let x: bool = 42;");
        assertFalse(errors.isEmpty());
    }

    @Test
    void typeMismatchStringInt() {
        List<String> errors = resolveErrors("let x: string = 5;");
        assertFalse(errors.isEmpty());
    }

    @Test
    void correctIntAssignment() {
        assertTrue(resolveErrors("let x: int = 5;").isEmpty());
    }

    @Test
    void correctBoolAssignment() {
        assertTrue(resolveErrors("let x: bool = true;").isEmpty());
    }

    @Test
    void correctStringAssignment() {
        assertTrue(resolveErrors("let x: string = \"hello\";").isEmpty());
    }

    // --- Undefined variable ---

    @Test
    void undefinedVariableInAssignment() {
        // Resolver catches undefined variable in assignment context
        List<String> errors = resolveErrors("undeclared = 5;");
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Undefined variable"));
    }

    @Test
    void definedVariableIsResolved() {
        assertTrue(resolveErrors("let x: int = 1; print(x);").isEmpty());
    }

    // --- Function return type ---

    @Test
    void returnTypeMismatch() {
        List<String> errors = resolveErrors("fun f() -> int { return true; }");
        assertFalse(errors.isEmpty());
    }

    @Test
    void correctReturnType() {
        assertTrue(resolveErrors("fun f() -> int { return 42; }").isEmpty());
    }

    @Test
    void functionCorrectCall() {
        assertTrue(resolveErrors(
            "fun add(a: int, b: int) -> int { return a + b; }\nadd(1, 2);"
        ).isEmpty());
    }

    // --- Arithmetic type rules ---

    @Test
    void addBoolAndIntIsError() {
        List<String> errors = resolveErrors("let x: int = true + 1;");
        assertFalse(errors.isEmpty());
    }

    @Test
    void logicalOpOnNonBoolIsError() {
        List<String> errors = resolveErrors("let x: bool = 1 && 2;");
        assertFalse(errors.isEmpty());
    }

    // --- Multiple errors collected ---

    @Test
    void multipleTypeErrors() {
        List<String> errors = resolveErrors(
            "let a: int = true;\nlet b: bool = 42;"
        );
        assertTrue(errors.size() >= 2);
    }
}
