import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class LexerTest {

    private List<Token> lex(String source) {
        return new Lexer(source).scanTokens();
    }

    // --- Literals ---

    @Test
    void integerLiteral() {
        List<Token> tokens = lex("42");
        assertEquals(TokenType.NUMBER, tokens.get(0).type);
        assertEquals(42.0, tokens.get(0).literal);
    }

    @Test
    void floatLiteral() {
        List<Token> tokens = lex("3.14");
        assertEquals(TokenType.NUMBER, tokens.get(0).type);
        assertEquals(3.14, tokens.get(0).literal);
    }

    @Test
    void stringLiteral() {
        List<Token> tokens = lex("\"hello\"");
        assertEquals(TokenType.STRING, tokens.get(0).type);
        assertEquals("hello", tokens.get(0).literal);
    }

    @Test
    void emptyString() {
        List<Token> tokens = lex("\"\"");
        assertEquals(TokenType.STRING, tokens.get(0).type);
        assertEquals("", tokens.get(0).literal);
    }

    // --- Keywords ---

    @Test
    void letKeyword() {
        assertEquals(TokenType.LET, lex("let").get(0).type);
    }

    @Test
    void controlFlowKeywords() {
        List<Token> tokens = lex("if else while for");
        assertEquals(TokenType.IF,    tokens.get(0).type);
        assertEquals(TokenType.ELSE,  tokens.get(1).type);
        assertEquals(TokenType.WHILE, tokens.get(2).type);
        assertEquals(TokenType.FOR,   tokens.get(3).type);
    }

    @Test
    void functionKeywords() {
        List<Token> tokens = lex("fun return");
        assertEquals(TokenType.FUN,    tokens.get(0).type);
        assertEquals(TokenType.RETURN, tokens.get(1).type);
    }

    @Test
    void boolLiterals() {
        List<Token> tokens = lex("true false");
        assertEquals(TokenType.TRUE,  tokens.get(0).type);
        assertEquals(TokenType.FALSE, tokens.get(1).type);
    }

    // --- Operators ---

    @Test
    void arithmeticOperators() {
        List<Token> tokens = lex("+ - * /");
        assertEquals(TokenType.PLUS,  tokens.get(0).type);
        assertEquals(TokenType.MINUS, tokens.get(1).type);
        assertEquals(TokenType.STAR,  tokens.get(2).type);
        assertEquals(TokenType.SLASH, tokens.get(3).type);
    }

    @Test
    void comparisonOperators() {
        List<Token> tokens = lex("== != < <= > >=");
        assertEquals(TokenType.EQUAL_EQUAL,   tokens.get(0).type);
        assertEquals(TokenType.BANG_EQUAL,    tokens.get(1).type);
        assertEquals(TokenType.LESS,          tokens.get(2).type);
        assertEquals(TokenType.LESS_EQUAL,    tokens.get(3).type);
        assertEquals(TokenType.GREATER,       tokens.get(4).type);
        assertEquals(TokenType.GREATER_EQUAL, tokens.get(5).type);
    }

    @Test
    void logicalOperators() {
        List<Token> tokens = lex("&& ||");
        assertEquals(TokenType.AND, tokens.get(0).type);
        assertEquals(TokenType.OR,  tokens.get(1).type);
    }

    @Test
    void arrowToken() {
        assertEquals(TokenType.ARROW, lex("->").get(0).type);
    }

    // --- Line / column tracking ---

    @Test
    void singleTokenOnLine1Col1() {
        Token t = lex("x").get(0);
        assertEquals(1, t.line);
        assertEquals(1, t.column);
    }

    @Test
    void multilineTracking() {
        List<Token> tokens = lex("let\n  x");
        assertEquals(1, tokens.get(0).line); // let
        assertEquals(2, tokens.get(1).line); // x
    }

    @Test
    void columnAdvancesOnSameLine() {
        List<Token> tokens = lex("a b");
        assertEquals(1, tokens.get(0).column);
        assertEquals(3, tokens.get(1).column);
    }

    // --- Error collection ---

    @Test
    void errorOnUnterminatedString() {
        Lexer lexer = new Lexer("\"unterminated");
        lexer.scanTokens();
        List<String> errors = lexer.getErrors();
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Unterminated string"));
    }

    @Test
    void errorOnUnexpectedCharacter() {
        Lexer lexer = new Lexer("@");
        lexer.scanTokens();
        List<String> errors = lexer.getErrors();
        assertFalse(errors.isEmpty());
        assertTrue(errors.get(0).contains("Unexpected character"));
    }

    @Test
    void multipleErrorsCollected() {
        Lexer lexer = new Lexer("@ # $");
        lexer.scanTokens();
        assertEquals(3, lexer.getErrors().size());
    }

    @Test
    void noErrorOnValidSource() {
        Lexer lexer = new Lexer("let x: int = 5;");
        lexer.scanTokens();
        assertTrue(lexer.getErrors().isEmpty());
    }

    // --- EOF ---

    @Test
    void eofOnEmptySource() {
        List<Token> tokens = lex("");
        assertEquals(1, tokens.size());
        assertEquals(TokenType.EOF, tokens.get(0).type);
    }

    @Test
    void eofIsAlwaysLast() {
        List<Token> tokens = lex("42");
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).type);
    }

    // --- Comments ---

    @Test
    void commentsAreIgnored() {
        List<Token> tokens = lex("// this is a comment\n42");
        assertEquals(TokenType.NUMBER, tokens.get(0).type);
    }
}
