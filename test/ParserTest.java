import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class ParserTest {

    private List<Stmt> parse(String source) {
        List<Token> tokens = new Lexer(source).scanTokens();
        Parser parser = new Parser(tokens);
        return parser.parse();
    }

    private Parser parserFor(String source) {
        List<Token> tokens = new Lexer(source).scanTokens();
        return new Parser(tokens);
    }

    // --- Variable declarations ---

    @Test
    void varDeclarationWithType() {
        List<Stmt> stmts = parse("let x: int = 5;");
        assertEquals(1, stmts.size());
        assertInstanceOf(Stmt.Var.class, stmts.get(0));
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertEquals("x", var.name().lexeme());
        assertEquals("int", var.type().lexeme());
        assertInstanceOf(Expr.Literal.class, var.initializer());
    }

    @Test
    void varDeclarationString() {
        List<Stmt> stmts = parse("let msg: string = \"hi\";");
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertEquals("msg", var.name().lexeme());
        assertEquals("string", var.type().lexeme());
    }

    @Test
    void varDeclarationBool() {
        List<Stmt> stmts = parse("let flag: bool = true;");
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertEquals("bool", var.type().lexeme());
    }

    @Test
    void varDeclarationArray() {
        List<Stmt> stmts = parse("let nums: int[] = [1, 2, 3];");
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertEquals("int[]", var.type().lexeme());
        assertInstanceOf(Expr.ArrayLiteral.class, var.initializer());
    }

    // --- If statement ---

    @Test
    void ifStatement() {
        List<Stmt> stmts = parse("if (true) { }");
        assertInstanceOf(Stmt.If.class, stmts.get(0));
        Stmt.If ifStmt = (Stmt.If) stmts.get(0);
        // parens are consumed as syntax; condition is the bare expression
        assertInstanceOf(Expr.Literal.class, ifStmt.condition());
        assertNull(ifStmt.elseBranch());
    }

    @Test
    void ifElseStatement() {
        List<Stmt> stmts = parse("if (true) { } else { }");
        Stmt.If ifStmt = (Stmt.If) stmts.get(0);
        assertNotNull(ifStmt.elseBranch());
    }

    // --- While statement ---

    @Test
    void whileStatement() {
        List<Stmt> stmts = parse("while (true) { }");
        assertInstanceOf(Stmt.While.class, stmts.get(0));
        Stmt.While w = (Stmt.While) stmts.get(0);
        // parens are consumed as syntax; condition is the bare expression
        assertInstanceOf(Expr.Literal.class, w.condition());
    }

    // --- For statement ---

    @Test
    void forStatement() {
        List<Stmt> stmts = parse("for (let i: int = 0; i < 10; i = i + 1) { }");
        assertInstanceOf(Stmt.For.class, stmts.get(0));
        Stmt.For forStmt = (Stmt.For) stmts.get(0);
        assertInstanceOf(Stmt.Var.class, forStmt.initializer());
        assertNotNull(forStmt.condition());
        assertNotNull(forStmt.increment());
    }

    // --- Function declaration ---

    @Test
    void functionDeclaration() {
        List<Stmt> stmts = parse("fun add(a: int, b: int) -> int { return a + b; }");
        assertInstanceOf(Stmt.Function.class, stmts.get(0));
        Stmt.Function fn = (Stmt.Function) stmts.get(0);
        assertEquals("add", fn.name().lexeme());
        assertEquals(2, fn.params().size());
        assertEquals("a", fn.params().get(0).lexeme());
        assertEquals("b", fn.params().get(1).lexeme());
        assertEquals("int", fn.returnType().lexeme());
        assertFalse(fn.body().isEmpty());
    }

    @Test
    void functionNoParams() {
        List<Stmt> stmts = parse("fun greet() -> string { return \"hi\"; }");
        Stmt.Function fn = (Stmt.Function) stmts.get(0);
        assertEquals(0, fn.params().size());
    }

    // --- Return statement ---

    @Test
    void returnStatement() {
        List<Stmt> stmts = parse("fun f() -> int { return 42; }");
        Stmt.Function fn = (Stmt.Function) stmts.get(0);
        assertInstanceOf(Stmt.Return.class, fn.body().get(0));
        Stmt.Return ret = (Stmt.Return) fn.body().get(0);
        assertInstanceOf(Expr.Literal.class, ret.value());
    }

    // --- Binary expressions ---

    @Test
    void binaryAddExpression() {
        List<Stmt> stmts = parse("let x: int = 1 + 2;");
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertInstanceOf(Expr.Binary.class, var.initializer());
        Expr.Binary bin = (Expr.Binary) var.initializer();
        assertEquals(TokenType.PLUS, bin.operator().type);
    }

    @Test
    void logicalAndExpression() {
        List<Stmt> stmts = parse("let x: bool = true && false;");
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertInstanceOf(Expr.Logical.class, var.initializer());
        Expr.Logical logical = (Expr.Logical) var.initializer();
        assertEquals(TokenType.AND, logical.operator().type);
    }

    @Test
    void logicalOrExpression() {
        List<Stmt> stmts = parse("let x: bool = true || false;");
        Stmt.Var var = (Stmt.Var) stmts.get(0);
        assertInstanceOf(Expr.Logical.class, var.initializer());
        Expr.Logical logical = (Expr.Logical) var.initializer();
        assertEquals(TokenType.OR, logical.operator().type);
    }

    // --- Error recovery ---

    @Test
    void errorRecoveryCollectsMultipleErrors() {
        Parser parser = parserFor("let = 5; let = 10;");
        parser.parse();
        assertTrue(parser.getErrors().size() >= 2);
    }

    @Test
    void validSourceHasNoErrors() {
        Parser parser = parserFor("let x: int = 5;");
        parser.parse();
        assertTrue(parser.getErrors().isEmpty());
    }
}
