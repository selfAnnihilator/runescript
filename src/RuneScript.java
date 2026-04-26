import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

// Main entry point class that combines all components
public class RuneScript {
    public static void main(String[] args) throws IOException {
        if (args.length > 2) {
            System.out.println("Usage: java -jar runescript.jar [script.rn]");
            System.out.println("       java -jar runescript.jar --emit-tokens script.rn");
            System.out.println("       java -jar runescript.jar --emit-ast script.rn");
            System.out.println("       java -jar runescript.jar --emit-bytecode script.rn");
            System.exit(64);
        } else if (args.length == 2) {
            if (args[0].equals("--emit-tokens")) {
                runPrintTokens(args[1]);
            } else if (args[0].equals("--emit-ast")) {
                runPrintAST(args[1]);
            } else if (args[0].equals("--emit-bytecode")) {
                runPrintBytecode(args[1]);
            } else {
                System.out.println("Usage: java -jar runescript.jar [--emit-tokens|--emit-ast|--emit-bytecode] script.rn");
                System.exit(64);
            }
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        new RuneScriptInterpreter().run(new String(bytes, Charset.defaultCharset()));
    }

    private static void runPrintTokens(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String source = new String(bytes, Charset.defaultCharset());

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        for (Token token : tokens) {
            System.out.println(token);
        }
    }

    private static void runPrintAST(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String source = new String(bytes, Charset.defaultCharset());

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Print AST representation
        for (Stmt stmt : statements) {
            System.out.println(stmt);
        }
    }

    private static void runPrintBytecode(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String source = new String(bytes, Charset.defaultCharset());

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();

        // Perform type checking
        RuneScriptInterpreter interpreter = new RuneScriptInterpreter();
        Resolver resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        // Generate bytecode
        BytecodeEmitter emitter = new BytecodeEmitter();
        for (Stmt stmt : statements) {
            emitter.compileStmt(stmt);
        }
        emitter.emitReturn(0);

        Chunk chunk = emitter.getChunk();
        System.out.println("Bytecode instructions:");
        for (int i = 0; i < chunk.codeSize();) {
            i = printInstruction(chunk, i);
        }
    }

    private static int printInstruction(Chunk chunk, int offset) {
        int opcodeValue = chunk.codeAt(offset) & 0xFF;
        Instruction.OpCode[] opcodes = Instruction.OpCode.values();
        if (opcodeValue >= opcodes.length) {
            System.out.println(offset + ": <invalid opcode " + opcodeValue + ">");
            return offset + 1;
        }

        Instruction.OpCode opcode = opcodes[opcodeValue];
        int next = offset + 1;
        switch (opcode) {
            case CONSTANT, GET_LOCAL, SET_LOCAL, MAKE_LAMBDA, CAPTURE_UPVALUE,
                 GET_UPVALUE, SET_UPVALUE, GET_FIELD, SET_FIELD -> {
                int operand = readUnsignedShort(chunk, next);
                System.out.println(offset + ": " + opcode + " " + operand);
                next += 2;
            }
            case JUMP_IF_FALSE, JUMP_IF_FALSE_KEEP, JUMP_IF_TRUE_KEEP, JUMP -> {
                int operand = readSignedShort(chunk, next);
                System.out.println(offset + ": " + opcode + " " + operand);
                next += 2;
            }
            case PRINT, CALL, MAKE_ARRAY -> {
                int operand = chunk.codeAt(next) & 0xFF;
                System.out.println(offset + ": " + opcode + " " + operand);
                next += 1;
            }
            case MAKE_STRUCT -> {
                int template = readUnsignedShort(chunk, next);
                int count = chunk.codeAt(next + 2) & 0xFF;
                System.out.println(offset + ": " + opcode + " " + template + " " + count);
                next += 3;
            }
            default -> System.out.println(offset + ": " + opcode);
        }
        return next;
    }

    private static int readUnsignedShort(Chunk chunk, int offset) {
        int high = chunk.codeAt(offset) & 0xFF;
        int low = chunk.codeAt(offset + 1) & 0xFF;
        return (high << 8) | low;
    }

    private static int readSignedShort(Chunk chunk, int offset) {
        int value = readUnsignedShort(chunk, offset);
        return value > 32767 ? value - 65536 : value;
    }

    private static void runPrompt() throws IOException {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

        // Persistent VM and local variable names across REPL lines.
        // The VM's stack is NOT reset between calls, so locals stay live.
        VM replVM = new VM();
        List<String> persistentLocalNames = new ArrayList<>();

        System.out.println("RuneScript REPL - Type 'exit' to quit");
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null || line.trim().equals("exit")) break;
            if (line.isBlank()) continue;

            try {
                Lexer lexer = new Lexer(line);
                List<Token> tokens = lexer.scanTokens();
                Parser parser = new Parser(tokens);
                List<Stmt> statements = parser.parse();
                if (statements.isEmpty()) continue;

                BytecodeEmitter emitter = new BytecodeEmitter(new ArrayList<>(persistentLocalNames));
                for (Stmt stmt : statements) {
                    emitter.compileStmt(stmt);
                }

                // Only expression statements get "Returned: null"; declarations are silent.
                Stmt last = statements.get(statements.size() - 1);
                if (last instanceof Stmt.Expression) {
                    emitter.emit((byte)Instruction.OpCode.NIL.ordinal(), 0);
                    emitter.emit((byte)Instruction.OpCode.RETURN.ordinal(), 0);
                }
                // For Stmt.Var / if / while / block: VM runs to end of chunk silently.

                persistentLocalNames = emitter.getLocalNames();
                replVM.interpret(emitter.getChunk());
            } catch (Exception ex) {
                System.err.println("Error: " + ex.getMessage());
            }
        }
    }
}

// Token class
class Token {
    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int column;

    public Token(TokenType type, String lexeme, Object literal, int line, int column) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    public TokenType type() {
        return type;
    }

    public String lexeme() {
        return lexeme;
    }

    public Object literal() {
        return literal;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    @Override
    public String toString() {
        return type + " " + lexeme + " " + literal;
    }
}

// Token types
enum TokenType {
    // Single-character tokens
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_BRACKET, RIGHT_BRACKET,
    COMMA, DOT, SEMICOLON, SLASH, STAR, COLON, PLUS, MINUS,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    PIPE_ARROW,   // |>
    ARROW,        // ->

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // Custom keywords for our language
    LET,
    STRUCT,

    EOF
}

// AST nodes using sealed interfaces
sealed interface Expr permits
    Expr.Assign, Expr.Binary, Expr.Call, Expr.Grouping,
    Expr.Literal, Expr.Logical, Expr.Unary,
    Expr.Variable, Expr.Pipe, Expr.Lambda,
    Expr.ArrayLiteral, Expr.Index,
    Expr.FieldAccess, Expr.FieldAssign, Expr.StructLiteral {

    record Assign(Token name, Expr value) implements Expr {}

    record Binary(Expr left, Token operator, Expr right) implements Expr {}

    record Call(Expr callee, Token paren, List<Expr> arguments) implements Expr {}

    record Grouping(Expr expression) implements Expr {}

    record Literal(Object value) implements Expr {}

    record Logical(Expr left, Token operator, Expr right) implements Expr {}

    record Unary(Token operator, Expr right) implements Expr {}

    record Variable(Token name) implements Expr {}

    record Pipe(Expr value, Expr call) implements Expr {}

    // (param, param -> body_expr)
    record Lambda(List<Token> params, Expr body) implements Expr {}

    record ArrayLiteral(List<Expr> elements, Token bracket) implements Expr {}

    record Index(Expr array, Token bracket, Expr index) implements Expr {}

    record FieldAccess(Expr object, Token field) implements Expr {}

    record FieldAssign(Expr object, Token field, Expr value) implements Expr {}

    record StructLiteral(Token name, List<Token> fieldNames, List<Expr> fieldValues) implements Expr {}
}

// Statement AST nodes
sealed interface Stmt permits
    Stmt.Block, Stmt.Expression, Stmt.For, Stmt.Function, Stmt.If, Stmt.Print,
    Stmt.Return, Stmt.Var, Stmt.While, Stmt.Assign, Stmt.Struct {

    record Block(List<Stmt> statements) implements Stmt {}

    record Expression(Expr expression) implements Stmt {}

    record Function(Token name, List<Token> params, List<Token> paramTypes,
                    Token returnType, List<Stmt> body) implements Stmt {}

    record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {}

    record Print(Expr expression) implements Stmt {}

    record Return(Token keyword, Expr value) implements Stmt {}

    record Var(Token name, Token type, Expr initializer) implements Stmt {}

    record For(Stmt initializer, Expr condition, Stmt increment, Stmt body) implements Stmt {}

    record While(Expr condition, Stmt body) implements Stmt {}

    record Assign(Token name, Expr value) implements Stmt {}

    record Struct(Token name, List<Token> fieldNames, List<Token> fieldTypes) implements Stmt {}
}

// Lexer class
class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 0;
    private int startColumn = 0;

    private static final Map<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("for", TokenType.FOR);
        keywords.put("fun", TokenType.FUN);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        // Removed "print" from keywords to allow it as function call
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);

        // Our custom keywords
        keywords.put("let", TokenType.LET);
        keywords.put("struct", TokenType.STRUCT);
        keywords.put("int", TokenType.IDENTIFIER);
        keywords.put("bool", TokenType.IDENTIFIER);
        keywords.put("string", TokenType.IDENTIFIER);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            startColumn = column;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line, column));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '[': addToken(TokenType.LEFT_BRACKET); break;
            case ']': addToken(TokenType.RIGHT_BRACKET); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case ':': addToken(TokenType.COLON); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-':
                addToken(match('>') ? TokenType.ARROW : TokenType.MINUS);
                break;

            case '!':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '=':
                addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '<':
                addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '>':
                addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '&':
                if (match('&')) addToken(TokenType.AND);
                else errors.add("[line " + line + ", col " + column + "] Error: Unexpected character: &");
                break;
            case '|':
                if (match('>')) addToken(TokenType.PIPE_ARROW);
                else if (match('|')) addToken(TokenType.OR);
                else errors.add("[line " + line + ", col " + column + "] Error: Unexpected character: |");
                break;

            case '/':
                if (match('/')) {
                    // A comment goes until the end of the line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
                break;

            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace
                break;

            case '\n':
                line++;
                column = 0;
                // Don't add token
                break;

            case '"':
                string();
                break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    errors.add("[line " + line + ", col " + column + "] Error: Unexpected character: '" + c + "'");
                }
                break;
        }
    }

    private void identifier() {
        while (isAlphanumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = TokenType.IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        addToken(TokenType.NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
                column = 0;
            }
            advance();
        }

        if (isAtEnd()) {
            errors.add("[line " + line + ", col " + column + "] Error: Unterminated string.");
            return;
        }

        // The closing ".
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        column++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean isAlphanumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        current++;
        column++;
        return source.charAt(current - 1);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line, startColumn + 1));
    }
}

// Parser class
class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
        }

        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.LET)) return varDeclaration();
            if (match(TokenType.FUN)) return funDeclaration();
            if (match(TokenType.STRUCT)) return structDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt structDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expected struct name.");
        consume(TokenType.LEFT_BRACE, "Expected '{' before struct body.");
        List<Token> fieldNames = new ArrayList<>();
        List<Token> fieldTypes = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            fieldNames.add(consume(TokenType.IDENTIFIER, "Expected field name."));
            consume(TokenType.COLON, "Expected ':' after field name.");
            Token ftype = consume(TokenType.IDENTIFIER, "Expected field type.");
            if (match(TokenType.LEFT_BRACKET)) {
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after '['.");
                ftype = new Token(TokenType.IDENTIFIER, ftype.lexeme() + "[]", null, ftype.line(), ftype.column());
            }
            fieldTypes.add(ftype);
            if (check(TokenType.COMMA)) advance();
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after struct body.");
        return new Stmt.Struct(name, fieldNames, fieldTypes);
    }

    private Stmt funDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expected function name.");
        consume(TokenType.LEFT_PAREN, "Expected '(' after function name.");
        List<Token> params = new ArrayList<>();
        List<Token> paramTypes = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                params.add(consume(TokenType.IDENTIFIER, "Expected parameter name."));
                consume(TokenType.COLON, "Expected ':' after parameter name.");
                Token ptype = consume(TokenType.IDENTIFIER, "Expected parameter type.");
                if (match(TokenType.LEFT_BRACKET)) {
                    consume(TokenType.RIGHT_BRACKET, "Expected ']' after '['.");
                    ptype = new Token(TokenType.IDENTIFIER, ptype.lexeme() + "[]", null, ptype.line(), ptype.column());
                }
                paramTypes.add(ptype);
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters.");
        consume(TokenType.ARROW, "Expected '->' after parameters.");
        Token returnType = consume(TokenType.IDENTIFIER, "Expected return type.");
        if (match(TokenType.LEFT_BRACKET)) {
            consume(TokenType.RIGHT_BRACKET, "Expected ']' after '['.");
            returnType = new Token(TokenType.IDENTIFIER, returnType.lexeme() + "[]", null, returnType.line(), returnType.column());
        }
        consume(TokenType.LEFT_BRACE, "Expected '{' before function body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, paramTypes, returnType, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name.");
        Token type = null;

        if (match(TokenType.COLON)) {
            type = consume(TokenType.IDENTIFIER, "Expected type name (int, bool, string).");
            if (match(TokenType.LEFT_BRACKET)) {
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after '['.");
                type = new Token(TokenType.IDENTIFIER, type.lexeme() + "[]", null, type.line(), type.column());
            }
        }

        consume(TokenType.EQUAL, "Expected '=' after variable name.");
        Expr initializer = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");

        return new Stmt.Var(name, type, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());

        return expressionStatement();
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'if'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'for'.");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.LET)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after for condition.");

        Stmt increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = new Stmt.Expression(expression());
        }
        consume(TokenType.RIGHT_PAREN, "Expected ')' after for clauses.");

        Stmt body = statement();
        return new Stmt.For(initializer, condition, increment, body);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expected '(' after 'while'.");
        Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after condition.");

        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            Stmt stmt = declaration();
            if (stmt != null) {
                statements.add(stmt);
            }
        }

        consume(TokenType.RIGHT_BRACE, "Expected '}' after block.");
        return statements;
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = pipe();

        if (match(TokenType.EQUAL)) {
            Token equals = previous();
            Expr value = assignment();

            if (expr instanceof Expr.Variable variable) {
                Token name = variable.name();
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.FieldAccess fa) {
                return new Expr.FieldAssign(fa.object(), fa.field(), value);
            } else {
                throw error(equals, "Invalid assignment target.");
            }
        }

        return expr;
    }

    private Expr pipe() {
        Expr expr = or();

        while (match(TokenType.PIPE_ARROW)) {
            Token pipeArrow = previous();
            Expr call = call();

            expr = new Expr.Pipe(expr, call);
        }

        return expr;
    }

    private Expr or() {
        Expr expr = and();
        while (match(TokenType.OR)) {
            Token op = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, op, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(TokenType.AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, op, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = addition();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = addition();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr addition() {
        Expr expr = multiplication();

        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = multiplication();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr multiplication() {
        Expr expr = unary();

        while (match(TokenType.STAR, TokenType.SLASH)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        Expr expr = primary();

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(TokenType.LEFT_BRACKET)) {
                Token bracket = previous();
                Expr index = expression();
                consume(TokenType.RIGHT_BRACKET, "Expected ']' after index.");
                expr = new Expr.Index(expr, bracket, index);
            } else if (match(TokenType.DOT)) {
                Token field = consume(TokenType.IDENTIFIER, "Expected field name after '.'.");
                expr = new Expr.FieldAccess(expr, field);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        Token paren = consume(TokenType.RIGHT_PAREN, "Expected ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(TokenType.IDENTIFIER)) {
            Token name = previous();
            if (check(TokenType.LEFT_BRACE)) {
                return parseStructLiteral(name);
            }
            return new Expr.Variable(name);
        }

        if (match(TokenType.LEFT_PAREN)) {
            if (isLambdaStart()) {
                return parseLambda();
            }
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(TokenType.LEFT_BRACKET)) {
            Token bracket = previous();
            List<Expr> elements = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    elements.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACKET, "Expected ']' after array elements.");
            return new Expr.ArrayLiteral(elements, bracket);
        }

        throw error(peek(), "Expected expression.");
    }

    // Look-ahead to determine if '(' starts a lambda rather than a grouped expr.
    // Lambdas: (-> body) or (param, ... -> body)
    private boolean isLambdaStart() {
        int saved = current;
        if (check(TokenType.ARROW)) { return true; } // zero-param lambda
        if (check(TokenType.IDENTIFIER)) {
            advance();
            while (match(TokenType.COMMA)) {
                if (!check(TokenType.IDENTIFIER)) { current = saved; return false; }
                advance();
            }
            boolean result = check(TokenType.ARROW);
            current = saved;
            return result;
        }
        current = saved;
        return false;
    }

    // Parse (-> body_expr) or (param, ... -> body_expr) — '(' already consumed.
    private Expr parseLambda() {
        List<Token> params = new ArrayList<>();
        if (!check(TokenType.ARROW)) {
            params.add(consume(TokenType.IDENTIFIER, "Expected parameter name."));
            while (match(TokenType.COMMA)) {
                params.add(consume(TokenType.IDENTIFIER, "Expected parameter name."));
            }
        }
        consume(TokenType.ARROW, "Expected '->' in lambda.");
        Expr body = expression();
        consume(TokenType.RIGHT_PAREN, "Expected ')' after lambda body.");
        return new Expr.Lambda(params, body);
    }

    // Parse Name { field: expr, ... } — name already consumed.
    private Expr parseStructLiteral(Token name) {
        consume(TokenType.LEFT_BRACE, "Expected '{' in struct literal.");
        List<Token> fieldNames = new ArrayList<>();
        List<Expr> fieldValues = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            fieldNames.add(consume(TokenType.IDENTIFIER, "Expected field name."));
            consume(TokenType.COLON, "Expected ':' after field name.");
            fieldValues.add(expression());
            if (check(TokenType.COMMA)) advance();
        }
        consume(TokenType.RIGHT_BRACE, "Expected '}' after struct literal.");
        return new Expr.StructLiteral(name, fieldNames, fieldValues);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        errors.add("[line " + token.line() + ", col " + token.column() + "] Error: " + message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == TokenType.SEMICOLON) return;

            switch (peek().type()) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, RETURN, LET, RIGHT_BRACE, STRUCT:
                    return;
            }

            advance();
        }
    }
}

// Type system
sealed interface Type permits
    Type.IntType, Type.BoolType, Type.StringType, Type.NilType,
    Type.ErrorType, Type.FunctionType, Type.ArrayType, Type.StructType {

    record IntType() implements Type { public String toString() { return "int"; } }
    record BoolType() implements Type { public String toString() { return "bool"; } }
    record StringType() implements Type { public String toString() { return "string"; } }
    record NilType() implements Type { public String toString() { return "nil"; } }
    record ErrorType() implements Type { public String toString() { return "<error>"; } }
    record FunctionType(List<Type> paramTypes, Type returnType) implements Type {
        public String toString() { return "<fn>"; }
    }
    record ArrayType(Type element) implements Type {
        public String toString() { return element + "[]"; }
    }
    record StructType(String name, LinkedHashMap<String, Type> fields) implements Type {
        public String toString() { return name; }
    }

    static Type fromString(String typeName) {
        return switch (typeName) {
            case "int"      -> new Type.IntType();
            case "bool"     -> new Type.BoolType();
            case "string"   -> new Type.StringType();
            case "int[]"    -> new Type.ArrayType(new Type.IntType());
            case "bool[]"   -> new Type.ArrayType(new Type.BoolType());
            case "string[]" -> new Type.ArrayType(new Type.StringType());
            default -> new Type.ErrorType();
        };
    }

    default boolean isPrimitive() {
        return !(this instanceof Type.ErrorType);
    }
}

// Resolver (semantic analyzer)
class Resolver {
    private final RuneScriptInterpreter interpreter;
    private final Stack<Map<String, Type>> scopes = new Stack<>();
    private final List<String> errors = new ArrayList<>();
    private Type currentReturnType = null;
    private final Map<String, Type.StructType> structTypes = new HashMap<>();

    public Resolver(RuneScriptInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public List<String> getErrors() {
        return errors;
    }

    private Type resolveTypeAnnotation(String typeName) {
        Type t = Type.fromString(typeName);
        if (t instanceof Type.ErrorType && structTypes.containsKey(typeName)) {
            return structTypes.get(typeName);
        }
        return t;
    }

    public void resolve(List<Stmt> statements) {
        beginScope(); // Create global scope
        for (Stmt statement : statements) {
            resolve(statement);
        }
        endScope();
    }

    private void resolve(Stmt stmt) {
        if (stmt instanceof Stmt.Struct structStmt) {
            LinkedHashMap<String, Type> fields = new LinkedHashMap<>();
            for (int i = 0; i < structStmt.fieldNames().size(); i++) {
                String fname = structStmt.fieldNames().get(i).lexeme();
                Type ftype = resolveTypeAnnotation(structStmt.fieldTypes().get(i).lexeme());
                fields.put(fname, ftype);
            }
            Type.StructType st = new Type.StructType(structStmt.name().lexeme(), fields);
            structTypes.put(structStmt.name().lexeme(), st);
        } else if (stmt instanceof Stmt.Var varStmt) {
            declare(varStmt.name(), varStmt.type() != null ?
                   resolveTypeAnnotation(varStmt.type().lexeme()) : null);
            if (varStmt.initializer() != null) {
                Type initializerType = resolve(varStmt.initializer());
                Type varType = varStmt.type() != null ?
                              resolveTypeAnnotation(varStmt.type().lexeme()) : initializerType;
                if (!isCompatible(varType, initializerType)) {
                    error(varStmt.name(),
                          "Cannot assign " + initializerType + " to " + varType);
                }
                assign(varStmt.name(), varType);
            }
        } else if (stmt instanceof Stmt.Assign assignStmt) {
            Type valueType = resolve(assignStmt.value());
            Type varType = lookup(assignStmt.name());
            if (varType == null) {
                error(assignStmt.name(), "Undefined variable '" +
                      assignStmt.name().lexeme() + "'.");
            } else if (!isCompatible(varType, valueType)) {
                error(assignStmt.name(),
                      "Cannot assign " + valueType + " to " + varType);
            }
        } else if (stmt instanceof Stmt.Expression exprStmt) {
            resolve(exprStmt.expression());
        } else if (stmt instanceof Stmt.If ifStmt) {
            Type conditionType = resolve(ifStmt.condition());
            if (!(conditionType instanceof Type.BoolType)) {
                error(ifStmt.condition(), "Condition must be boolean.");
            }
            resolve(ifStmt.thenBranch());
            if (ifStmt.elseBranch() != null) {
                resolve(ifStmt.elseBranch());
            }
        } else if (stmt instanceof Stmt.For forStmt) {
            beginScope();
            if (forStmt.initializer() != null) resolve(forStmt.initializer());
            if (forStmt.condition() != null) {
                Type conditionType = resolve(forStmt.condition());
                if (!(conditionType instanceof Type.BoolType)) {
                    error(forStmt.condition(), "For condition must be boolean.");
                }
            }
            if (forStmt.increment() != null) resolve(forStmt.increment());
            resolve(forStmt.body());
            endScope();
        } else if (stmt instanceof Stmt.While whileStmt) {
            Type conditionType = resolve(whileStmt.condition());
            if (!(conditionType instanceof Type.BoolType)) {
                error(whileStmt.condition(), "Condition must be boolean.");
            }
            resolve(whileStmt.body());
        } else if (stmt instanceof Stmt.Block block) {
            beginScope();
            for (Stmt statement : block.statements()) {
                resolve(statement);
            }
            endScope();
        } else if (stmt instanceof Stmt.Print printStmt) {
            resolve(printStmt.expression());
        } else if (stmt instanceof Stmt.Function funStmt) {
            List<Type> paramTypes = funStmt.paramTypes().stream()
                .map(t -> resolveTypeAnnotation(t.lexeme())).toList();
            Type retType = resolveTypeAnnotation(funStmt.returnType().lexeme());
            Type.FunctionType fnType = new Type.FunctionType(paramTypes, retType);
            declare(funStmt.name(), fnType);
            assign(funStmt.name(), fnType);
            Type savedReturnType = currentReturnType;
            currentReturnType = resolveTypeAnnotation(funStmt.returnType().lexeme());
            beginScope();
            for (int i = 0; i < funStmt.params().size(); i++) {
                Type paramType = resolveTypeAnnotation(funStmt.paramTypes().get(i).lexeme());
                declare(funStmt.params().get(i), paramType);
            }
            for (Stmt s : funStmt.body()) resolve(s);
            endScope();
            currentReturnType = savedReturnType;
        } else if (stmt instanceof Stmt.Return retStmt) {
            if (currentReturnType == null) {
                error(retStmt.keyword(), "Cannot use 'return' outside a function.");
            } else if (retStmt.value() != null) {
                Type valueType = resolve(retStmt.value());
                if (!isCompatible(currentReturnType, valueType)) {
                    error(retStmt.keyword(), "Expected return type " + currentReturnType +
                          " but got " + valueType + ".");
                }
            }
        }
    }

    private Type resolve(Expr expr) {
        if (expr instanceof Expr.Assign assignExpr) {
            Type valueType = resolve(assignExpr.value());
            Type varType = lookup(assignExpr.name());
            if (varType == null) {
                error(assignExpr.name(), "Undefined variable '" +
                      assignExpr.name().lexeme() + "'.");
                return new Type.ErrorType();
            } else if (!isCompatible(varType, valueType)) {
                error(assignExpr.name(),
                      "Cannot assign " + valueType + " to " + varType);
            }
            return varType;
        } else if (expr instanceof Expr.Binary binExpr) {
            Type leftType = resolve(binExpr.left());
            Type rightType = resolve(binExpr.right());

            TokenType op = binExpr.operator().type();

            if (isArithmeticOperator(op)) {
                if (op == TokenType.PLUS &&
                    (leftType instanceof Type.StringType || rightType instanceof Type.StringType)) {
                    return new Type.StringType();
                }
                if (!(leftType instanceof Type.IntType) ||
                    !(rightType instanceof Type.IntType)) {
                    error(binExpr.operator(), "Operands must be integers.");
                    return new Type.ErrorType();
                }
                return new Type.IntType();
            } else if (isComparisonOperator(op)) {
                if (!isCompatible(leftType, rightType)) {
                    error(binExpr.operator(), "Operands must be compatible for comparison.");
                    return new Type.ErrorType();
                }
                return new Type.BoolType();
            } else if (op == TokenType.EQUAL_EQUAL || op == TokenType.BANG_EQUAL) {
                // Equality checks can be performed on any compatible types
                return new Type.BoolType();
            }
        } else if (expr instanceof Expr.Unary unaryExpr) {
            Type rightType = resolve(unaryExpr.right());
            TokenType op = unaryExpr.operator().type();

            if (op == TokenType.MINUS) {
                if (!(rightType instanceof Type.IntType)) {
                    error(unaryExpr.operator(), "Operand must be integer.");
                    return new Type.ErrorType();
                }
                return new Type.IntType();
            } else if (op == TokenType.BANG) {
                if (!(rightType instanceof Type.BoolType)) {
                    error(unaryExpr.operator(), "Operand must be boolean.");
                    return new Type.ErrorType();
                }
                return new Type.BoolType();
            }
        } else if (expr instanceof Expr.Variable varExpr) {
            return lookup(varExpr.name());
        } else if (expr instanceof Expr.Literal litExpr) {
            if (litExpr.value() instanceof Integer) {
                return new Type.IntType();
            } else if (litExpr.value() instanceof Double d) {
                // Check if it's a whole number (represents an integer)
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return new Type.IntType();
                } else {
                    // Could be a float type if we had one, for now treat as error
                    return new Type.IntType(); // Treat as int for now
                }
            } else if (litExpr.value() instanceof Boolean) {
                return new Type.BoolType();
            } else if (litExpr.value() instanceof String) {
                return new Type.StringType();
            } else {
                return new Type.ErrorType();
            }
        } else if (expr instanceof Expr.Logical logExpr) {
            Type leftType = resolve(logExpr.left());
            Type rightType = resolve(logExpr.right());
            if (!(leftType instanceof Type.BoolType) || !(rightType instanceof Type.BoolType)) {
                error(logExpr.operator(), "Operands of '&&'/'||' must both be boolean.");
                return new Type.ErrorType();
            }
            return new Type.BoolType();
        } else if (expr instanceof Expr.Grouping groupExpr) {
            return resolve(groupExpr.expression());
        } else if (expr instanceof Expr.Lambda lambdaExpr) {
            // Don't deeply type-check lambda bodies; just return a function type placeholder.
            return new Type.NilType();
        } else if (expr instanceof Expr.Call callExpr) {
            if (callExpr.callee() instanceof Expr.Variable var) {
                String fname = var.name().lexeme();
                if (fname.equals("print")) {
                    for (Expr arg : callExpr.arguments()) resolve(arg);
                    return new Type.NilType();
                }
                if (fname.equals("len")) {
                    if (callExpr.arguments().size() != 1) {
                        error(var.name(), "len() expects 1 argument.");
                        return new Type.ErrorType();
                    }
                    Type argType = resolve(callExpr.arguments().get(0));
                    if (!(argType instanceof Type.StringType) && !(argType instanceof Type.ArrayType)) {
                        error(var.name(), "len() expects a string or array argument.");
                        return new Type.ErrorType();
                    }
                    return new Type.IntType();
                }
                if (fname.equals("push")) {
                    if (callExpr.arguments().size() != 2) {
                        error(var.name(), "push() expects 2 arguments: push(arr, val).");
                        return new Type.ErrorType();
                    }
                    Type arrType = resolve(callExpr.arguments().get(0));
                    resolve(callExpr.arguments().get(1));
                    if (!(arrType instanceof Type.ArrayType)) {
                        error(var.name(), "push() expects an array as first argument.");
                        return new Type.ErrorType();
                    }
                    return new Type.NilType();
                }
                if (fname.equals("substr")) {
                    if (callExpr.arguments().size() != 3) {
                        error(var.name(), "substr() expects 3 arguments: substr(s, start, end).");
                        return new Type.ErrorType();
                    }
                    Type s     = resolve(callExpr.arguments().get(0));
                    Type start = resolve(callExpr.arguments().get(1));
                    Type end   = resolve(callExpr.arguments().get(2));
                    if (!(s instanceof Type.StringType) ||
                        !(start instanceof Type.IntType) ||
                        !(end instanceof Type.IntType)) {
                        error(var.name(), "substr(s, start, end) expects string, int, int.");
                        return new Type.ErrorType();
                    }
                    return new Type.StringType();
                }
            }
            // Generic call: look up the callee; if it's a named function, return its return type.
            Type calleeType = resolve(callExpr.callee());
            for (Expr arg : callExpr.arguments()) resolve(arg);
            if (calleeType instanceof Type.FunctionType ft) return ft.returnType();
            return new Type.NilType();
        } else if (expr instanceof Expr.Pipe pipeExpr) {
            resolve(pipeExpr.value());
            Expr rhs = pipeExpr.call();
            if (rhs instanceof Expr.Call call) {
                resolve(call.callee());
                for (Expr arg : call.arguments()) resolve(arg);
            } else {
                resolve(rhs);
            }
            return new Type.NilType();
        } else if (expr instanceof Expr.ArrayLiteral arrExpr) {
            if (arrExpr.elements().isEmpty()) return new Type.ArrayType(new Type.NilType());
            Type elemType = resolve(arrExpr.elements().get(0));
            for (int i = 1; i < arrExpr.elements().size(); i++) {
                Type t = resolve(arrExpr.elements().get(i));
                if (!isCompatible(elemType, t)) {
                    error(arrExpr.bracket(), "Array elements must have the same type.");
                    return new Type.ErrorType();
                }
            }
            return new Type.ArrayType(elemType);
        } else if (expr instanceof Expr.Index idxExpr) {
            Type arrayType = resolve(idxExpr.array());
            Type indexType = resolve(idxExpr.index());
            if (!(arrayType instanceof Type.ArrayType at)) {
                error(idxExpr.bracket(), "Index operator requires an array.");
                return new Type.ErrorType();
            }
            if (!(indexType instanceof Type.IntType)) {
                error(idxExpr.bracket(), "Array index must be an integer.");
                return new Type.ErrorType();
            }
            return at.element();
        } else if (expr instanceof Expr.StructLiteral sl) {
            Type.StructType st = structTypes.get(sl.name().lexeme());
            if (st == null) {
                error(sl.name(), "Undefined struct '" + sl.name().lexeme() + "'.");
                return new Type.ErrorType();
            }
            for (int i = 0; i < sl.fieldNames().size(); i++) {
                Token fname = sl.fieldNames().get(i);
                Type ftype = resolve(sl.fieldValues().get(i));
                Type expected = st.fields().get(fname.lexeme());
                if (expected == null) {
                    error(fname, "Unknown field '" + fname.lexeme() + "' in struct '" + st.name() + "'.");
                } else if (!isCompatible(expected, ftype)) {
                    error(fname, "Cannot assign " + ftype + " to field '" + fname.lexeme() +
                          "' of type " + expected + ".");
                }
            }
            return st;
        } else if (expr instanceof Expr.FieldAccess fa) {
            Type objType = resolve(fa.object());
            if (!(objType instanceof Type.StructType st)) {
                error(fa.field(), "Field access requires a struct.");
                return new Type.ErrorType();
            }
            Type fieldType = st.fields().get(fa.field().lexeme());
            if (fieldType == null) {
                error(fa.field(), "Unknown field '" + fa.field().lexeme() + "' in struct '" + st.name() + "'.");
                return new Type.ErrorType();
            }
            return fieldType;
        } else if (expr instanceof Expr.FieldAssign fassign) {
            Type objType = resolve(fassign.object());
            Type valueType = resolve(fassign.value());
            if (!(objType instanceof Type.StructType st)) {
                error(fassign.field(), "Field assignment requires a struct.");
                return new Type.ErrorType();
            }
            Type fieldType = st.fields().get(fassign.field().lexeme());
            if (fieldType == null) {
                error(fassign.field(), "Unknown field '" + fassign.field().lexeme() +
                      "' in struct '" + st.name() + "'.");
                return new Type.ErrorType();
            }
            if (!isCompatible(fieldType, valueType)) {
                error(fassign.field(), "Cannot assign " + valueType + " to field '" +
                      fassign.field().lexeme() + "' of type " + fieldType + ".");
                return new Type.ErrorType();
            }
            return fieldType;
        }

        return new Type.ErrorType();
    }

    private boolean isArithmeticOperator(TokenType op) {
        return op == TokenType.PLUS || op == TokenType.MINUS ||
               op == TokenType.STAR || op == TokenType.SLASH;
    }

    private boolean isComparisonOperator(TokenType op) {
        return op == TokenType.GREATER || op == TokenType.GREATER_EQUAL ||
               op == TokenType.LESS || op == TokenType.LESS_EQUAL;
    }

    private boolean isCompatible(Type expected, Type actual) {
        if (expected == null || actual == null ||
            expected instanceof Type.ErrorType || actual instanceof Type.ErrorType) {
            return false;
        }
        if (expected instanceof Type.StructType s1 && actual instanceof Type.StructType s2) {
            return s1.name().equals(s2.name());
        }
        return expected.getClass().equals(actual.getClass());
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name, Type type) {
        if (scopes.isEmpty()) return;

        Map<String, Type> scope = scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            error(name, "Variable with this name already declared in this scope.");
        }
        scope.put(name.lexeme(), type);
    }

    private void assign(Token name, Type type) {
        if (scopes.isEmpty()) return;

        for (int i = scopes.size() - 1; i >= 0; i--) {
            scopes.get(i).put(name.lexeme(), type);
        }
    }

    private Type lookup(Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Type type = scopes.get(i).get(name.lexeme());
            if (type != null) return type;
        }
        return null; // Variable not found
    }

    private void error(Token token, String message) {
        errors.add(String.format("[line %d, col %d] Error at '%s': %s",
                                 token.line(), token.column(), token.lexeme(), message));
    }

    private void error(Expr expr, String message) {
        errors.add("Error: " + message);
    }
}

// Instruction definitions
sealed interface Instruction permits
    Instruction.Constant, Instruction.Add, Instruction.Subtract,
    Instruction.Multiply, Instruction.Divide, Instruction.Negate,
    Instruction.True, Instruction.False, Instruction.Nil,
    Instruction.Equal, Instruction.Greater, Instruction.Less,
    Instruction.JumpIfFalse, Instruction.JumpIfFalseKeep,
    Instruction.JumpIfTrueKeep, Instruction.Jump, Instruction.Print,
    Instruction.Pop, Instruction.GetLocal, Instruction.SetLocal,
    Instruction.Return, Instruction.Not,
    Instruction.MakeLambda, Instruction.Call,
    Instruction.Len, Instruction.Substr,
    Instruction.CaptureUpvalue, Instruction.GetUpvalue, Instruction.SetUpvalue,
    Instruction.MakeStruct, Instruction.GetField, Instruction.SetField {

    enum OpCode {
        CONSTANT,
        ADD, SUBTRACT, MULTIPLY, DIVIDE, NEGATE,
        TRUE, FALSE, NIL,
        EQUAL, GREATER, LESS,
        JUMP_IF_FALSE,
        JUMP_IF_FALSE_KEEP,   // jump if false, but leave value on stack (&&)
        JUMP_IF_TRUE_KEEP,    // jump if true, but leave value on stack (||)
        JUMP,
        PRINT, POP,
        GET_LOCAL, SET_LOCAL,
        RETURN, NOT,
        MAKE_LAMBDA,
        CALL,
        LEN,
        SUBSTR,
        MAKE_ARRAY,
        GET_INDEX,
        SET_INDEX,
        PUSH_ARR,
        CAPTURE_UPVALUE,
        GET_UPVALUE,
        SET_UPVALUE,
        MAKE_STRUCT,
        GET_FIELD,
        SET_FIELD
    }

    record Constant(int constantIndex) implements Instruction {}
    record Add() implements Instruction {}
    record Subtract() implements Instruction {}
    record Multiply() implements Instruction {}
    record Divide() implements Instruction {}
    record Negate() implements Instruction {}
    record True() implements Instruction {}
    record False() implements Instruction {}
    record Nil() implements Instruction {}
    record Equal() implements Instruction {}
    record Greater() implements Instruction {}
    record Less() implements Instruction {}
    record JumpIfFalse(int offset) implements Instruction {}
    record JumpIfFalseKeep(int offset) implements Instruction {}
    record JumpIfTrueKeep(int offset) implements Instruction {}
    record Jump(int offset) implements Instruction {}
    record Print() implements Instruction {}
    record Pop() implements Instruction {}
    record GetLocal(int slot) implements Instruction {}
    record SetLocal(int slot) implements Instruction {}
    record Return() implements Instruction {}
    record Not() implements Instruction {}
    record MakeLambda(int templateIndex) implements Instruction {}
    record Len() implements Instruction {}
    record Substr() implements Instruction {}
    record Call(int argCount) implements Instruction {}
    record CaptureUpvalue(int slot) implements Instruction {}
    record GetUpvalue(int index) implements Instruction {}
    record SetUpvalue(int index) implements Instruction {}
    record MakeStruct(int templateIdx, int count) implements Instruction {}
    record GetField(int fieldNameIdx) implements Instruction {}
    record SetField(int fieldNameIdx) implements Instruction {}
}

// Template stored in the constant pool; carries AST + captured variable names.
class LambdaTemplate {
    final List<String> params;
    final Expr body;           // null for named functions
    final List<Stmt> stmtBody; // null for inline lambdas
    final List<String> capturedNames;
    final String selfName;     // non-null for named functions (enables self-reference)

    LambdaTemplate(List<String> params, Expr body, List<String> capturedNames) {
        this.params = params;
        this.body = body;
        this.stmtBody = null;
        this.capturedNames = capturedNames;
        this.selfName = null;
    }

    LambdaTemplate(List<String> params, List<Stmt> stmtBody,
                   List<String> capturedNames, String selfName) {
        this.params = params;
        this.body = null;
        this.stmtBody = stmtBody;
        this.capturedNames = capturedNames;
        this.selfName = selfName;
    }
}

// Runtime closure value: a lambda paired with captured variable values.
class LambdaValue {
    final List<String> params;
    final Expr body;           // null for named functions
    final List<Stmt> stmtBody; // null for inline lambdas
    final Map<String, Object> capturedEnv;

    LambdaValue(List<String> params, Expr body, Map<String, Object> capturedEnv) {
        this.params = params;
        this.body = body;
        this.stmtBody = null;
        this.capturedEnv = capturedEnv;
    }

    LambdaValue(List<String> params, List<Stmt> stmtBody, Map<String, Object> capturedEnv) {
        this.params = params;
        this.body = null;
        this.stmtBody = stmtBody;
        this.capturedEnv = capturedEnv;
    }

    @Override
    public String toString() {
        return "<fn(" + String.join(", ", params) + ")>";
    }
}

// Mutable reference box for by-reference upvalue capture.
class UpvalueObj {
    Object value;
    UpvalueObj(Object v) { this.value = v; }
    @Override public String toString() { return value == null ? "null" : value.toString(); }
}

// Template stored in the constant pool for MAKE_STRUCT.
class StructTemplate {
    final String name;
    final List<String> fieldNames;
    StructTemplate(String name, List<String> fieldNames) {
        this.name = name;
        this.fieldNames = fieldNames;
    }
}

// Chunk class to hold bytecode and constants
class Chunk {
    private final List<Byte> code = new ArrayList<>();
    private final List<Object> constants = new ArrayList<>();
    private final List<Integer> lines = new ArrayList<>();

    public void write(byte byteCode, int line) {
        code.add(byteCode);
        lines.add(line);
    }

    public int addConstant(Object value) {
        constants.add(value);
        return constants.size() - 1;
    }

    public byte codeAt(int index) { return code.get(index); }
    public void patchAt(int index, byte value) { code.set(index, value); }
    public int lineAt(int index) { return lines.get(index); }
    public Object constantAt(int index) { return constants.get(index); }
    public int codeSize() { return code.size(); }
}

// Bytecode emitter
class BytecodeEmitter {
    private final Chunk chunk;
    private final List<String> localNames = new ArrayList<>();
    private int localCount = 0;
    private final List<Integer> scopeStartCounts = new ArrayList<>();

    public BytecodeEmitter() {
        this.chunk = new Chunk();
    }

    public BytecodeEmitter(List<String> initialLocalNames) {
        this.chunk = new Chunk();
        this.localNames.addAll(initialLocalNames);
        this.localCount = initialLocalNames.size();
    }

    public List<String> getLocalNames() {
        return new ArrayList<>(localNames);
    }

    public void emit(byte instruction, int line) {
        chunk.write(instruction, line);
    }

    public void emit(int instruction, int line) {
        chunk.write((byte) instruction, line);
    }

    // Emit a 2-byte big-endian unsigned short (for constant/local indices).
    public void emitShort(int value, int line) {
        chunk.write((byte)((value >> 8) & 0xFF), line);
        chunk.write((byte)(value & 0xFF), line);
    }

    public int addConstant(Object value) {
        return chunk.addConstant(value);
    }

    public void emitConstant(Object value, int line) {
        int constantIndex = addConstant(value);
        emit((byte)Instruction.OpCode.CONSTANT.ordinal(), line);
        emitShort(constantIndex, line);
    }

    public void declareVariable(String name) {
        localNames.add(name);
        localCount++;
    }

    public Integer getVariableSlot(String name) {
        for (int i = localNames.size() - 1; i >= 0; i--) {
            if (localNames.get(i).equals(name)) return i;
        }
        return null;
    }

    public void beginScope() {
        scopeStartCounts.add(localCount);
    }

    public void endScope(int line) {
        int scopeStart = scopeStartCounts.remove(scopeStartCounts.size() - 1);
        int toPop = localCount - scopeStart;
        for (int i = 0; i < toPop; i++) {
            emit((byte)Instruction.OpCode.POP.ordinal(), line);
            localNames.remove(localNames.size() - 1);
        }
        localCount = scopeStart;
    }

    // Emits a jump with a 2-byte placeholder offset; returns position of high byte for patching.
    public int emitJump(int opcode, int line) {
        emit((byte)opcode, line);
        emit((byte)0xFF, line);
        emit((byte)0xFF, line);
        return chunk.codeSize() - 2;
    }

    // Patches a previously emitted jump with the correct forward offset.
    public void patchJump(int jumpPos) {
        int offset = chunk.codeSize() - jumpPos - 2;
        if (offset > 65535) throw new RuntimeException("Jump offset too large");
        chunk.patchAt(jumpPos, (byte)((offset >> 8) & 0xFF));
        chunk.patchAt(jumpPos + 1, (byte)(offset & 0xFF));
    }

    // Emits a backward JUMP to loopStart using a signed 2-byte offset.
    public void emitLoop(int loopStart, int line) {
        emit((byte)Instruction.OpCode.JUMP.ordinal(), line);
        int highBytePos = chunk.codeSize();
        emit((byte)0, line);
        emit((byte)0, line);
        // After VM decodes: ip = highBytePos + 2; we want ip = loopStart.
        int offset = loopStart - chunk.codeSize(); // negative (backward)
        chunk.patchAt(highBytePos, (byte)((offset >> 8) & 0xFF));
        chunk.patchAt(highBytePos + 1, (byte)(offset & 0xFF));
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void emitReturn(int line) {
        // Pop all remaining locals so RETURN always sees null on the stack.
        for (int i = 0; i < localCount; i++) {
            emit((byte)Instruction.OpCode.POP.ordinal(), line);
        }
        emit((byte)Instruction.OpCode.NIL.ordinal(), line);
        emit((byte)Instruction.OpCode.RETURN.ordinal(), line);
    }

    public void compileStmt(Stmt stmt) {
        if (stmt instanceof Stmt.Struct) {
            // Struct declarations are type-level only; no bytecode emitted.
        } else if (stmt instanceof Stmt.Expression exprStmt) {
            visitExpression(exprStmt.expression());
            emit((byte)Instruction.OpCode.POP.ordinal(), 0);
        } else if (stmt instanceof Stmt.Var varStmt) {
            if (varStmt.initializer() != null) {
                visitExpression(varStmt.initializer());
            } else {
                emit((byte)Instruction.OpCode.NIL.ordinal(), 0);
            }
            declareVariable(varStmt.name().lexeme());
        } else if (stmt instanceof Stmt.Block block) {
            beginScope();
            for (Stmt s : block.statements()) {
                compileStmt(s);
            }
            endScope(0);
        } else if (stmt instanceof Stmt.If ifStmt) {
            visitExpression(ifStmt.condition());
            int thenJump = emitJump(Instruction.OpCode.JUMP_IF_FALSE.ordinal(), 0);
            compileStmt(ifStmt.thenBranch());
            int elseJump = emitJump(Instruction.OpCode.JUMP.ordinal(), 0);
            patchJump(thenJump);
            if (ifStmt.elseBranch() != null) {
                compileStmt(ifStmt.elseBranch());
            }
            patchJump(elseJump);
        } else if (stmt instanceof Stmt.For forStmt) {
            beginScope();
            if (forStmt.initializer() != null) compileStmt(forStmt.initializer());
            int loopStart = chunk.codeSize();
            int exitJump = -1;
            if (forStmt.condition() != null) {
                visitExpression(forStmt.condition());
                exitJump = emitJump(Instruction.OpCode.JUMP_IF_FALSE.ordinal(), 0);
            }
            compileStmt(forStmt.body());
            if (forStmt.increment() != null) compileStmt(forStmt.increment());
            emitLoop(loopStart, 0);
            if (exitJump != -1) patchJump(exitJump);
            endScope(0);
        } else if (stmt instanceof Stmt.While whileStmt) {
            int loopStart = chunk.codeSize();
            visitExpression(whileStmt.condition());
            int exitJump = emitJump(Instruction.OpCode.JUMP_IF_FALSE.ordinal(), 0);
            compileStmt(whileStmt.body());
            emitLoop(loopStart, 0);
            patchJump(exitJump);
        } else if (stmt instanceof Stmt.Assign assignStmt) {
            visitExpression(assignStmt.value());
            Integer slot = getVariableSlot(assignStmt.name().lexeme());
            if (slot != null) {
                emit((byte)Instruction.OpCode.SET_LOCAL.ordinal(), 0);
                emitShort(slot, 0);
            }
            emit((byte)Instruction.OpCode.POP.ordinal(), 0);
        } else if (stmt instanceof Stmt.Function funStmt) {
            List<String> params = funStmt.params().stream().map(Token::lexeme).toList();
            for (int i = 0; i < localCount; i++) {
                emit((byte)Instruction.OpCode.CAPTURE_UPVALUE.ordinal(), funStmt.name().line());
                emitShort(i, funStmt.name().line());
            }
            LambdaTemplate template = new LambdaTemplate(
                params, funStmt.body(), new ArrayList<>(localNames), funStmt.name().lexeme());
            int idx = addConstant(template);
            emit((byte)Instruction.OpCode.MAKE_LAMBDA.ordinal(), funStmt.name().line());
            emitShort(idx, funStmt.name().line());
            declareVariable(funStmt.name().lexeme());
        } else if (stmt instanceof Stmt.Return retStmt) {
            if (retStmt.value() != null) {
                visitExpression(retStmt.value());
            } else {
                emit((byte)Instruction.OpCode.NIL.ordinal(), retStmt.keyword().line());
            }
            emit((byte)Instruction.OpCode.RETURN.ordinal(), retStmt.keyword().line());
        }
    }

    public void visitLiteral(Expr.Literal literal, int line) {
        Object val = literal.value();
        if (val instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                emitConstant(d.intValue(), line);
            } else {
                emitConstant(d, line);
            }
        } else if (val instanceof Integer i) {
            emitConstant(i, line);
        } else if (val instanceof String s) {
            emitConstant(s, line);
        } else if (val instanceof Boolean b) {
            emit((byte)(b ? Instruction.OpCode.TRUE.ordinal() : Instruction.OpCode.FALSE.ordinal()), line);
        } else {
            emit((byte)Instruction.OpCode.NIL.ordinal(), line);
        }
    }

    public void visitBinary(Expr.Binary binary) {
        visitExpression(binary.left());
        visitExpression(binary.right());

        int line = binary.operator().line();
        switch (binary.operator().type()) {
            case PLUS        -> emit((byte)Instruction.OpCode.ADD.ordinal(), line);
            case MINUS       -> emit((byte)Instruction.OpCode.SUBTRACT.ordinal(), line);
            case STAR        -> emit((byte)Instruction.OpCode.MULTIPLY.ordinal(), line);
            case SLASH       -> emit((byte)Instruction.OpCode.DIVIDE.ordinal(), line);
            case EQUAL_EQUAL -> emit((byte)Instruction.OpCode.EQUAL.ordinal(), line);
            case BANG_EQUAL  -> { emit((byte)Instruction.OpCode.EQUAL.ordinal(), line);
                                  emit((byte)Instruction.OpCode.NOT.ordinal(), line); }
            case GREATER     -> emit((byte)Instruction.OpCode.GREATER.ordinal(), line);
            case GREATER_EQUAL -> { emit((byte)Instruction.OpCode.LESS.ordinal(), line);
                                    emit((byte)Instruction.OpCode.NOT.ordinal(), line); }
            case LESS        -> emit((byte)Instruction.OpCode.LESS.ordinal(), line);
            case LESS_EQUAL  -> { emit((byte)Instruction.OpCode.GREATER.ordinal(), line);
                                  emit((byte)Instruction.OpCode.NOT.ordinal(), line); }
            default -> System.err.println("Unknown binary operator: " + binary.operator().type());
        }
    }

    public void visitUnary(Expr.Unary unary) {
        visitExpression(unary.right());
        int line = unary.operator().line();
        if (unary.operator().type() == TokenType.MINUS) {
            emit((byte)Instruction.OpCode.NEGATE.ordinal(), line);
        } else if (unary.operator().type() == TokenType.BANG) {
            emit((byte)Instruction.OpCode.NOT.ordinal(), line);
        }
    }

    public void visitVariable(Expr.Variable variable) {
        Integer slot = getVariableSlot(variable.name().lexeme());
        if (slot != null) {
            emit((byte)Instruction.OpCode.GET_LOCAL.ordinal(), variable.name().line());
            emitShort(slot, variable.name().line());
        } else {
            System.err.println("Error: Undefined variable '" + variable.name().lexeme() + "'");
        }
    }

    public void visitAssign(Expr.Assign assign) {
        visitExpression(assign.value());
        Integer slot = getVariableSlot(assign.name().lexeme());
        if (slot != null) {
            emit((byte)Instruction.OpCode.SET_LOCAL.ordinal(), assign.name().line());
            emitShort(slot, assign.name().line());
        } else {
            System.err.println("Error: Undefined variable '" + assign.name().lexeme() + "'");
        }
    }

    public void visitGrouping(Expr.Grouping grouping) {
        visitExpression(grouping.expression());
    }

    public void visitExpression(Expr expr) {
        if (expr instanceof Expr.Literal literal) {
            visitLiteral(literal, 0);
        } else if (expr instanceof Expr.Binary binary) {
            visitBinary(binary);
        } else if (expr instanceof Expr.Unary unary) {
            visitUnary(unary);
        } else if (expr instanceof Expr.Variable variable) {
            visitVariable(variable);
        } else if (expr instanceof Expr.Assign assign) {
            visitAssign(assign);
        } else if (expr instanceof Expr.Grouping grouping) {
            visitGrouping(grouping);
        } else if (expr instanceof Expr.Call call) {
            visitCall(call);
        } else if (expr instanceof Expr.Pipe pipe) {
            visitPipe(pipe);
        } else if (expr instanceof Expr.Lambda lambda) {
            visitLambda(lambda);
        } else if (expr instanceof Expr.Logical logical) {
            visitLogical(logical);
        } else if (expr instanceof Expr.ArrayLiteral arrLit) {
            visitArrayLiteral(arrLit);
        } else if (expr instanceof Expr.Index idxExpr) {
            visitIndex(idxExpr);
        } else if (expr instanceof Expr.StructLiteral sl) {
            visitStructLiteral(sl);
        } else if (expr instanceof Expr.FieldAccess fa) {
            visitFieldAccess(fa);
        } else if (expr instanceof Expr.FieldAssign fassign) {
            visitFieldAssign(fassign);
        }
    }

    private void visitLogical(Expr.Logical logical) {
        visitExpression(logical.left());
        int line = logical.operator().line();
        if (logical.operator().type() == TokenType.AND) {
            // If left is false, skip right and keep false on stack.
            int skipRight = emitJump(Instruction.OpCode.JUMP_IF_FALSE_KEEP.ordinal(), line);
            emit((byte)Instruction.OpCode.POP.ordinal(), line);
            visitExpression(logical.right());
            patchJump(skipRight);
        } else {
            // OR: if left is true, skip right and keep true on stack.
            int skipRight = emitJump(Instruction.OpCode.JUMP_IF_TRUE_KEEP.ordinal(), line);
            emit((byte)Instruction.OpCode.POP.ordinal(), line);
            visitExpression(logical.right());
            patchJump(skipRight);
        }
    }

    // Emit CAPTURE_UPVALUE for each current local, then MAKE_LAMBDA.
    private void visitLambda(Expr.Lambda lambda) {
        List<String> params = lambda.params().stream().map(Token::lexeme).toList();
        for (int i = 0; i < localCount; i++) {
            emit((byte)Instruction.OpCode.CAPTURE_UPVALUE.ordinal(), 0);
            emitShort(i, 0);
        }
        LambdaTemplate template = new LambdaTemplate(params, lambda.body(), new ArrayList<>(localNames));
        int idx = addConstant(template);
        emit((byte)Instruction.OpCode.MAKE_LAMBDA.ordinal(), 0);
        emitShort(idx, 0);
    }

    private void visitArrayLiteral(Expr.ArrayLiteral arrLit) {
        for (Expr elem : arrLit.elements()) visitExpression(elem);
        emit((byte)Instruction.OpCode.MAKE_ARRAY.ordinal(), arrLit.bracket().line());
        emit((byte)arrLit.elements().size(), arrLit.bracket().line());
    }

    private void visitIndex(Expr.Index idxExpr) {
        visitExpression(idxExpr.array());
        visitExpression(idxExpr.index());
        emit((byte)Instruction.OpCode.GET_INDEX.ordinal(), idxExpr.bracket().line());
    }

    private void visitStructLiteral(Expr.StructLiteral sl) {
        for (Expr fval : sl.fieldValues()) visitExpression(fval);
        List<String> names = sl.fieldNames().stream().map(Token::lexeme).toList();
        StructTemplate tmpl = new StructTemplate(sl.name().lexeme(), names);
        int idx = addConstant(tmpl);
        emit((byte)Instruction.OpCode.MAKE_STRUCT.ordinal(), sl.name().line());
        emitShort(idx, sl.name().line());
        emit((byte)sl.fieldValues().size(), sl.name().line());
    }

    private void visitFieldAccess(Expr.FieldAccess fa) {
        visitExpression(fa.object());
        int idx = addConstant(fa.field().lexeme());
        emit((byte)Instruction.OpCode.GET_FIELD.ordinal(), fa.field().line());
        emitShort(idx, fa.field().line());
    }

    private void visitFieldAssign(Expr.FieldAssign fassign) {
        visitExpression(fassign.object());
        visitExpression(fassign.value());
        int idx = addConstant(fassign.field().lexeme());
        emit((byte)Instruction.OpCode.SET_FIELD.ordinal(), fassign.field().line());
        emitShort(idx, fassign.field().line());
    }

    private void visitCall(Expr.Call call) {
        if (call.callee() instanceof Expr.Variable var) {
            String fname = var.name().lexeme();
            if (fname.equals("print")) {
                for (Expr arg : call.arguments()) visitExpression(arg);
                emit((byte)Instruction.OpCode.PRINT.ordinal(), call.paren().line());
                emit((byte)call.arguments().size(), call.paren().line());
                emit((byte)Instruction.OpCode.NIL.ordinal(), call.paren().line());
                return;
            }
            if (fname.equals("len")) {
                visitExpression(call.arguments().get(0));
                emit((byte)Instruction.OpCode.LEN.ordinal(), call.paren().line());
                return;
            }
            if (fname.equals("substr")) {
                for (Expr arg : call.arguments()) visitExpression(arg);
                emit((byte)Instruction.OpCode.SUBSTR.ordinal(), call.paren().line());
                return;
            }
            if (fname.equals("push")) {
                visitExpression(call.arguments().get(0));
                visitExpression(call.arguments().get(1));
                emit((byte)Instruction.OpCode.PUSH_ARR.ordinal(), call.paren().line());
                return;
            }
        }
        {
            // Generic call: push args left-to-right, then push callee, then CALL n.
            // Stack layout: [..., arg0, arg1, ..., argN-1, callee]
            for (Expr arg : call.arguments()) visitExpression(arg);
            visitExpression(call.callee());
            emit((byte)Instruction.OpCode.CALL.ordinal(), call.paren().line());
            emit((byte)call.arguments().size(), call.paren().line());
        }
    }

    private void visitPipe(Expr.Pipe pipe) {
        // The piped value is always arg0; push it first.
        visitExpression(pipe.value());

        Expr rhs = pipe.call();
        if (rhs instanceof Expr.Call call) {
            if (call.callee() instanceof Expr.Variable var) {
                String fname = var.name().lexeme();
                if (fname.equals("print")) {
                    for (Expr arg : call.arguments()) visitExpression(arg);
                    emit((byte)Instruction.OpCode.PRINT.ordinal(), call.paren().line());
                    emit((byte)(1 + call.arguments().size()), call.paren().line());
                    emit((byte)Instruction.OpCode.NIL.ordinal(), call.paren().line());
                    return;
                }
                if (fname.equals("len")) {
                    // piped value is the arg — already on stack
                    emit((byte)Instruction.OpCode.LEN.ordinal(), call.paren().line());
                    return;
                }
                if (fname.equals("substr")) {
                    // piped value is s; push start and end from the call args
                    for (Expr arg : call.arguments()) visitExpression(arg);
                    emit((byte)Instruction.OpCode.SUBSTR.ordinal(), call.paren().line());
                    return;
                }
            }
            // Named function call: push extra args, push callee, CALL (1 + extras).
            for (Expr arg : call.arguments()) visitExpression(arg);
            visitExpression(call.callee());
            emit((byte)Instruction.OpCode.CALL.ordinal(), call.paren().line());
            emit((byte)(1 + call.arguments().size()), call.paren().line());
        } else {
            // RHS is a lambda literal or a variable holding a lambda.
            // Stack: [..., piped_value, callee]
            visitExpression(rhs);
            emit((byte)Instruction.OpCode.CALL.ordinal(), 0);
            emit((byte)1, 0);
        }
    }
}

// Virtual Machine
class VM {
    private static final int STACK_MAX = 256;
    private final Object[] stack = new Object[STACK_MAX];
    private int stackPointer = 0;

    private Chunk chunk;
    private int ip = 0;

    public static class RuntimeError extends RuntimeException {
        public RuntimeError(String message) {
            super(message);
        }
    }

    private static class ReturnValue extends RuntimeException {
        final Object value;
        ReturnValue(Object value) { super(null, null, true, false); this.value = value; }
    }

    public void resetStack() {
        stackPointer = 0;
    }

    public void interpret(Chunk chunk) {
        this.chunk = chunk;
        this.ip = 0;

        try {
            while (true) {
                if (ip >= chunk.codeSize()) break;

                Instruction.OpCode instruction = readOpCode();
                switch (instruction) {
                    case CONSTANT: constant(); break;
                    case ADD: add(); break;
                    case SUBTRACT: binaryOpInt((a, b) -> a - b); break;
                    case MULTIPLY: binaryOpInt((a, b) -> a * b); break;
                    case DIVIDE: binaryOpInt((a, b) -> a / b); break;
                    case NEGATE: negate(); break;
                    case TRUE: push(true); break;
                    case FALSE: push(false); break;
                    case NIL: push(null); break;
                    case EQUAL: equality(); break;
                    case GREATER: binaryOpCompare((a, b) -> a > b); break;
                    case LESS: binaryOpCompare((a, b) -> a < b); break;
                    case NOT: notOp(); break;
                    case PRINT: print(); break;
                    case POP: pop(); break;
                    case GET_LOCAL: getLocal(); break;
                    case SET_LOCAL: setLocal(); break;
                    case RETURN: returnOp(); return;
                    case JUMP_IF_FALSE: jumpIfFalse(); break;
                    case JUMP_IF_FALSE_KEEP: jumpIfFalseKeep(); break;
                    case JUMP_IF_TRUE_KEEP: jumpIfTrueKeep(); break;
                    case JUMP: jump(); break;
                    case MAKE_LAMBDA: makeLambda(); break;
                    case CALL: call(); break;
                    case LEN: len(); break;
                    case SUBSTR: substr(); break;
                    case MAKE_ARRAY: makeArray(); break;
                    case GET_INDEX: getIndex(); break;
                    case SET_INDEX: setIndex(); break;
                    case PUSH_ARR: pushArr(); break;
                    case CAPTURE_UPVALUE: captureUpvalue(); break;
                    case GET_UPVALUE: getUpvalue(); break;
                    case SET_UPVALUE: setUpvalue(); break;
                    case MAKE_STRUCT: makeStruct(); break;
                    case GET_FIELD: getField(); break;
                    case SET_FIELD: setField(); break;
                    default:
                        throw new RuntimeError("Unknown opcode: " + instruction);
                }
            }
        } catch (RuntimeError e) {
            System.err.println(e.getMessage());
        }
    }

    private Instruction.OpCode readOpCode() {
        return Instruction.OpCode.values()[chunk.codeAt(ip++) & 0xFF];
    }

    private int readShortOffset() {
        int high = chunk.codeAt(ip++) & 0xFF;
        int low = chunk.codeAt(ip++) & 0xFF;
        int offset = (high << 8) | low;
        if (offset > 32767) offset -= 65536;
        return offset;
    }

    // Reads a 2-byte big-endian unsigned index (0–65535).
    private int readShortUnsigned() {
        int high = chunk.codeAt(ip++) & 0xFF;
        int low = chunk.codeAt(ip++) & 0xFF;
        return (high << 8) | low;
    }

    private Object readConstant() {
        return chunk.constantAt(readShortUnsigned());
    }

    private void push(Object value) {
        stack[stackPointer++] = value;
    }

    private Object pop() {
        if (stackPointer <= 0) throw new RuntimeError("Stack underflow");
        return stack[--stackPointer];
    }

    private Object peek(int distance) {
        return stack[stackPointer - 1 - distance];
    }

    private void constant() {
        push(readConstant());
    }

    private void add() {
        Object b = pop();
        Object a = pop();
        if (a instanceof String || b instanceof String) {
            push(String.valueOf(a == null ? "null" : a) + String.valueOf(b == null ? "null" : b));
        } else {
            Integer intA = toInt(a);
            Integer intB = toInt(b);
            if (intA == null || intB == null) throw new RuntimeError("Operands must be numbers or strings.");
            push(intA + intB);
        }
    }

    private void len() {
        Object val = pop();
        if (val instanceof String s) { push(s.length()); return; }
        if (val instanceof ArrayList<?> arr) { push(arr.size()); return; }
        throw new RuntimeError("len() expects a string or array.");
    }

    @SuppressWarnings("unchecked")
    private void makeArray() {
        int count = chunk.codeAt(ip++) & 0xFF;
        ArrayList<Object> arr = new ArrayList<>(count);
        for (int i = count - 1; i >= 0; i--) arr.add(null); // reserve slots
        for (int i = count - 1; i >= 0; i--) arr.set(i, pop());
        push(arr);
    }

    @SuppressWarnings("unchecked")
    private void getIndex() {
        Object idx = pop();
        Object arr = pop();
        if (!(arr instanceof ArrayList<?> list)) throw new RuntimeError("Index operator requires an array.");
        Integer i = toInt(idx);
        if (i == null) throw new RuntimeError("Array index must be an integer.");
        if (i < 0 || i >= list.size()) throw new RuntimeError("Array index out of bounds: " + i);
        push(list.get(i));
    }

    @SuppressWarnings("unchecked")
    private void setIndex() {
        Object val = pop();
        Object idx = pop();
        Object arr = pop();
        if (!(arr instanceof ArrayList)) throw new RuntimeError("Index assignment requires an array.");
        Integer i = toInt(idx);
        if (i == null) throw new RuntimeError("Array index must be an integer.");
        ArrayList<Object> list = (ArrayList<Object>) arr;
        if (i < 0 || i >= list.size()) throw new RuntimeError("Array index out of bounds: " + i);
        list.set(i, val);
        push(val);
    }

    @SuppressWarnings("unchecked")
    private void pushArr() {
        Object val = pop();
        Object arr = pop();
        if (!(arr instanceof ArrayList)) throw new RuntimeError("push() expects an array.");
        ((ArrayList<Object>) arr).add(val);
        push(null);
    }

    private void substr() {
        Object end   = pop();
        Object start = pop();
        Object str   = pop();
        if (!(str instanceof String s)) throw new RuntimeError("substr() expects a string as first argument.");
        Integer startIdx = toInt(start);
        Integer endIdx   = toInt(end);
        if (startIdx == null || endIdx == null) throw new RuntimeError("substr() indices must be integers.");
        try {
            push(s.substring(startIdx, endIdx));
        } catch (StringIndexOutOfBoundsException e) {
            throw new RuntimeError("substr() index out of bounds: " + e.getMessage());
        }
    }

    private void binaryOpInt(BinaryOperator<Integer> op) {
        Object b = pop();
        Object a = pop();
        Integer intA = toInt(a);
        Integer intB = toInt(b);
        if (intA == null || intB == null) throw new RuntimeError("Operands must be numbers.");
        push(op.apply(intA, intB));
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer i) return i;
        if (value instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d))
            return d.intValue();
        return null;
    }

    private void binaryOpCompare(BiFunction<Integer, Integer, Boolean> op) {
        Object b = pop();
        Object a = pop();
        Integer intA = toInt(a);
        Integer intB = toInt(b);
        if (intA == null || intB == null) throw new RuntimeError("Operands must be numbers.");
        push(op.apply(intA, intB));
    }

    private void negate() {
        Object value = pop();
        Integer i = toInt(value);
        if (i == null) throw new RuntimeError("Operand must be a number.");
        push(-i);
    }

    private void notOp() {
        Object value = pop();
        if (!(value instanceof Boolean)) throw new RuntimeError("Operand must be boolean.");
        push(!(Boolean)value);
    }

    private void equality() {
        Object b = pop();
        Object a = pop();
        if (a == null && b == null) push(true);
        else if (a == null || b == null) push(false);
        else push(a.equals(b));
    }

    private void print() {
        int count = chunk.codeAt(ip++) & 0xFF;
        Object[] values = new Object[count];
        for (int i = count - 1; i >= 0; i--) {
            values[i] = pop();
        }
        StringBuilder sb = new StringBuilder();
        for (Object v : values) {
            sb.append(v == null ? "null" : v.toString());
        }
        System.out.println(sb);
    }

    private void getLocal() {
        Object v = stack[readShortUnsigned()];
        push(v instanceof UpvalueObj u ? u.value : v);
    }

    private void setLocal() {
        int slot = readShortUnsigned();
        if (stack[slot] instanceof UpvalueObj u) u.value = peek(0);
        else stack[slot] = peek(0);
    }

    private void captureUpvalue() {
        int slot = readShortUnsigned();
        if (!(stack[slot] instanceof UpvalueObj)) {
            stack[slot] = new UpvalueObj(stack[slot]);
        }
    }

    private void getUpvalue() {
        // Reserved for when lambda bodies compile to bytecode.
        throw new RuntimeError("GET_UPVALUE not yet supported.");
    }

    private void setUpvalue() {
        throw new RuntimeError("SET_UPVALUE not yet supported.");
    }

    @SuppressWarnings("unchecked")
    private void makeStruct() {
        StructTemplate tmpl = (StructTemplate) chunk.constantAt(readShortUnsigned());
        int count = chunk.codeAt(ip++) & 0xFF;
        Object[] vals = new Object[count];
        for (int i = count - 1; i >= 0; i--) vals[i] = pop();
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) fields.put(tmpl.fieldNames.get(i), vals[i]);
        push(fields);
    }

    @SuppressWarnings("unchecked")
    private void getField() {
        String fieldName = (String) chunk.constantAt(readShortUnsigned());
        Object obj = pop();
        if (!(obj instanceof LinkedHashMap<?, ?> map))
            throw new RuntimeError("GET_FIELD requires a struct, got: " +
                (obj == null ? "null" : obj.getClass().getSimpleName()));
        if (!((LinkedHashMap<String, Object>) map).containsKey(fieldName))
            throw new RuntimeError("Undefined field '" + fieldName + "'.");
        push(((LinkedHashMap<String, Object>) map).get(fieldName));
    }

    @SuppressWarnings("unchecked")
    private void setField() {
        String fieldName = (String) chunk.constantAt(readShortUnsigned());
        Object val = pop();
        Object obj = pop();
        if (!(obj instanceof LinkedHashMap<?, ?> map))
            throw new RuntimeError("SET_FIELD requires a struct.");
        ((LinkedHashMap<String, Object>) obj).put(fieldName, val);
        push(val);
    }

    private void jumpIfFalse() {
        int offset = readShortOffset();
        Object condition = pop();
        if (condition instanceof Boolean b && !b) {
            ip += offset;
        }
    }

    private void jumpIfFalseKeep() {
        int offset = readShortOffset();
        Object condition = peek(0);
        if (condition instanceof Boolean b && !b) {
            ip += offset;
        }
    }

    private void jumpIfTrueKeep() {
        int offset = readShortOffset();
        Object condition = peek(0);
        if (condition instanceof Boolean b && b) {
            ip += offset;
        }
    }

    private void jump() {
        int offset = readShortOffset();
        ip += offset;
    }

    private void makeLambda() {
        LambdaTemplate template = (LambdaTemplate) chunk.constantAt(readShortUnsigned());
        Map<String, Object> captured = new HashMap<>();
        // Stack slots were boxed by CAPTURE_UPVALUE; store UpvalueObj references directly.
        for (int i = 0; i < template.capturedNames.size() && i < stackPointer; i++) {
            Object slot = stack[i];
            captured.put(template.capturedNames.get(i),
                slot instanceof UpvalueObj ? slot : new UpvalueObj(slot));
        }
        LambdaValue lv;
        if (template.stmtBody != null) {
            lv = new LambdaValue(template.params, template.stmtBody, captured);
            // Self-inject via UpvalueObj so recursive calls go through the same box.
            if (template.selfName != null) {
                UpvalueObj selfBox = new UpvalueObj(lv);
                captured.put(template.selfName, selfBox);
            }
        } else {
            lv = new LambdaValue(template.params, template.body, captured);
        }
        push(lv);
    }

    // Stack before CALL n: [..., arg0, ..., argN-1, callee]
    private void call() {
        int argCount = chunk.codeAt(ip++) & 0xFF;
        Object callee = pop();
        Object[] args = new Object[argCount];
        for (int i = argCount - 1; i >= 0; i--) args[i] = pop();

        if (!(callee instanceof LambdaValue lambda)) {
            throw new RuntimeError("Can only call lambda values, got: " +
                (callee == null ? "null" : callee.getClass().getSimpleName()));
        }
        if (lambda.params.size() != argCount) {
            throw new RuntimeError("Expected " + lambda.params.size() +
                " argument(s) but got " + argCount + ".");
        }
        Map<String, Object> env = new HashMap<>(lambda.capturedEnv);
        for (int i = 0; i < lambda.params.size(); i++) env.put(lambda.params.get(i), args[i]);
        if (lambda.stmtBody != null) {
            push(executeBody(lambda.stmtBody, env));
        } else {
            push(evaluate(lambda.body, env));
        }
    }

    private Object executeBody(List<Stmt> stmts, Map<String, Object> env) {
        try {
            for (Stmt stmt : stmts) executeStmt(stmt, env);
        } catch (ReturnValue rv) {
            return rv.value;
        }
        return null;
    }

    private void executeStmt(Stmt stmt, Map<String, Object> env) {
        if (stmt instanceof Stmt.Expression exprStmt) {
            evaluate(exprStmt.expression(), env);
        } else if (stmt instanceof Stmt.Var varStmt) {
            Object val = varStmt.initializer() != null ? evaluate(varStmt.initializer(), env) : null;
            env.put(varStmt.name().lexeme(), val);
        } else if (stmt instanceof Stmt.Assign assignStmt) {
            Object val = evaluate(assignStmt.value(), env);
            Object existing = env.get(assignStmt.name().lexeme());
            if (existing instanceof UpvalueObj u) u.value = val;
            else env.put(assignStmt.name().lexeme(), val);
        } else if (stmt instanceof Stmt.Return retStmt) {
            Object val = retStmt.value() != null ? evaluate(retStmt.value(), env) : null;
            throw new ReturnValue(val);
        } else if (stmt instanceof Stmt.If ifStmt) {
            Object cond = evaluate(ifStmt.condition(), env);
            if (cond instanceof Boolean b && b) {
                executeStmt(ifStmt.thenBranch(), env);
            } else if (ifStmt.elseBranch() != null) {
                executeStmt(ifStmt.elseBranch(), env);
            }
        } else if (stmt instanceof Stmt.For forStmt) {
            Map<String, Object> forEnv = new HashMap<>(env);
            if (forStmt.initializer() != null) executeStmt(forStmt.initializer(), forEnv);
            while (true) {
                if (forStmt.condition() != null) {
                    Object cond = evaluate(forStmt.condition(), forEnv);
                    if (!(cond instanceof Boolean b && b)) break;
                }
                executeStmt(forStmt.body(), forEnv);
                if (forStmt.increment() != null) executeStmt(forStmt.increment(), forEnv);
            }
            for (String key : env.keySet()) {
                if (forEnv.containsKey(key)) env.put(key, forEnv.get(key));
            }
        } else if (stmt instanceof Stmt.While whileStmt) {
            while (true) {
                Object cond = evaluate(whileStmt.condition(), env);
                if (!(cond instanceof Boolean b && b)) break;
                executeStmt(whileStmt.body(), env);
            }
        } else if (stmt instanceof Stmt.Block block) {
            Map<String, Object> blockEnv = new HashMap<>(env);
            for (Stmt s : block.statements()) executeStmt(s, blockEnv);
            // propagate mutations to outer variables, not new block-local ones
            for (String key : env.keySet()) {
                if (blockEnv.containsKey(key)) env.put(key, blockEnv.get(key));
            }
        } else if (stmt instanceof Stmt.Function funStmt) {
            List<String> params = funStmt.params().stream().map(Token::lexeme).toList();
            env.put(funStmt.name().lexeme(),
                new LambdaValue(params, funStmt.body(), new HashMap<>(env)));
        } else if (stmt instanceof Stmt.Print printStmt) {
            System.out.println(evaluate(printStmt.expression(), env));
        }
    }

    // Tree-walk evaluator used for lambda bodies.
    private Object evaluate(Expr expr, Map<String, Object> env) {
        if (expr instanceof Expr.Literal lit) {
            Object v = lit.value();
            if (v instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d))
                return d.intValue();
            return v;
        }
        if (expr instanceof Expr.Variable var) {
            String name = var.name().lexeme();
            if (!env.containsKey(name))
                throw new RuntimeError("Undefined variable '" + name + "' in lambda body.");
            Object v = env.get(name);
            return v instanceof UpvalueObj u ? u.value : v;
        }
        if (expr instanceof Expr.Grouping g) return evaluate(g.expression(), env);
        if (expr instanceof Expr.Assign asgn) {
            Object val = evaluate(asgn.value(), env);
            Object existing = env.get(asgn.name().lexeme());
            if (existing instanceof UpvalueObj u) u.value = val;
            else env.put(asgn.name().lexeme(), val);
            return val;
        }
        if (expr instanceof Expr.Unary u) {
            Object val = evaluate(u.right(), env);
            return switch (u.operator().type()) {
                case MINUS -> -(toInt(val));
                case BANG  -> !(Boolean) val;
                default    -> throw new RuntimeError("Unknown unary op.");
            };
        }
        if (expr instanceof Expr.Binary b) {
            Object left  = evaluate(b.left(), env);
            Object right = evaluate(b.right(), env);
            return evalBinary(b.operator().type(), left, right);
        }
        if (expr instanceof Expr.Logical log) {
            Object left = evaluate(log.left(), env);
            if (log.operator().type() == TokenType.AND) {
                if (left instanceof Boolean bv && !bv) return left;
            } else {
                if (left instanceof Boolean bv && bv) return left;
            }
            return evaluate(log.right(), env);
        }
        if (expr instanceof Expr.Lambda lam) {
            // Nested lambda: close over current env.
            List<String> params = lam.params().stream().map(Token::lexeme).toList();
            return new LambdaValue(params, lam.body(), new HashMap<>(env));
        }
        if (expr instanceof Expr.ArrayLiteral arrLit) {
            ArrayList<Object> arr = new ArrayList<>();
            for (Expr elem : arrLit.elements()) arr.add(evaluate(elem, env));
            return arr;
        }
        if (expr instanceof Expr.Index idxExpr) {
            Object arr = evaluate(idxExpr.array(), env);
            Object idx = evaluate(idxExpr.index(), env);
            if (!(arr instanceof ArrayList<?> list)) throw new RuntimeError("Index operator requires an array.");
            Integer i = toInt(idx);
            if (i == null) throw new RuntimeError("Array index must be an integer.");
            if (i < 0 || i >= list.size()) throw new RuntimeError("Array index out of bounds: " + i);
            return list.get(i);
        }
        if (expr instanceof Expr.Call call) {
            if (call.callee() instanceof Expr.Variable cv) {
                String fname = cv.name().lexeme();
                if (fname.equals("print")) {
                    StringBuilder sb = new StringBuilder();
                    for (Expr arg : call.arguments()) sb.append(evaluate(arg, env));
                    System.out.println(sb);
                    return null;
                }
                if (fname.equals("len")) {
                    Object val = evaluate(call.arguments().get(0), env);
                    if (val instanceof String s) return s.length();
                    if (val instanceof ArrayList<?> arr) return arr.size();
                    throw new RuntimeError("len() expects a string or array.");
                }
                if (fname.equals("push")) {
                    Object arr = evaluate(call.arguments().get(0), env);
                    Object val = evaluate(call.arguments().get(1), env);
                    if (!(arr instanceof ArrayList<?>)) throw new RuntimeError("push() expects an array.");
                    @SuppressWarnings("unchecked")
                    ArrayList<Object> list = (ArrayList<Object>) arr;
                    list.add(val);
                    return null;
                }
                if (fname.equals("substr")) {
                    Object s     = evaluate(call.arguments().get(0), env);
                    Object start = evaluate(call.arguments().get(1), env);
                    Object end   = evaluate(call.arguments().get(2), env);
                    if (!(s instanceof String str)) throw new RuntimeError("substr() expects a string.");
                    return str.substring(toInt(start), toInt(end));
                }
            }
            Object callee = evaluate(call.callee(), env);
            List<Object> args = new ArrayList<>();
            for (Expr arg : call.arguments()) args.add(evaluate(arg, env));
            return callLambda(callee, args, env);
        }
        if (expr instanceof Expr.StructLiteral sl) {
            LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
            for (int i = 0; i < sl.fieldNames().size(); i++) {
                fields.put(sl.fieldNames().get(i).lexeme(), evaluate(sl.fieldValues().get(i), env));
            }
            return fields;
        }
        if (expr instanceof Expr.FieldAccess fa) {
            Object obj = evaluate(fa.object(), env);
            if (!(obj instanceof LinkedHashMap<?, ?> map))
                throw new RuntimeError("Field access requires a struct.");
            String field = fa.field().lexeme();
            if (!map.containsKey(field))
                throw new RuntimeError("Undefined field '" + field + "'.");
            return map.get(field);
        }
        if (expr instanceof Expr.FieldAssign fassign) {
            Object obj = evaluate(fassign.object(), env);
            Object val = evaluate(fassign.value(), env);
            if (!(obj instanceof LinkedHashMap<?, ?>))
                throw new RuntimeError("Field assignment requires a struct.");
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> fields = (LinkedHashMap<String, Object>) obj;
            fields.put(fassign.field().lexeme(), val);
            return val;
        }
        if (expr instanceof Expr.Pipe pipe) {
            Object piped = evaluate(pipe.value(), env);
            Expr rhs = pipe.call();
            if (rhs instanceof Expr.Call call) {
                if (call.callee() instanceof Expr.Variable cv && cv.name().lexeme().equals("print")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(piped == null ? "null" : piped);
                    for (Expr arg : call.arguments()) sb.append(evaluate(arg, env));
                    System.out.println(sb);
                    return null;
                }
                Object callee = evaluate(call.callee(), env);
                List<Object> args = new ArrayList<>();
                args.add(piped);
                for (Expr arg : call.arguments()) args.add(evaluate(arg, env));
                return callLambda(callee, args, env);
            }
            Object callee = evaluate(rhs, env);
            return callLambda(callee, List.of(piped), env);
        }
        throw new RuntimeError("Cannot evaluate expression in lambda body: " +
            expr.getClass().getSimpleName());
    }

    private Object callLambda(Object callee, List<Object> args, Map<String, Object> env) {
        if (!(callee instanceof LambdaValue lambda))
            throw new RuntimeError("Value is not callable: " + callee);
        if (lambda.params.size() != args.size())
            throw new RuntimeError("Expected " + lambda.params.size() +
                " arg(s), got " + args.size() + ".");
        Map<String, Object> newEnv = new HashMap<>(lambda.capturedEnv);
        for (int i = 0; i < lambda.params.size(); i++) newEnv.put(lambda.params.get(i), args.get(i));
        if (lambda.stmtBody != null) return executeBody(lambda.stmtBody, newEnv);
        return evaluate(lambda.body, newEnv);
    }

    private Object evalBinary(TokenType op, Object left, Object right) {
        return switch (op) {
            case PLUS         -> (left instanceof String || right instanceof String)
                                     ? String.valueOf(left) + String.valueOf(right)
                                     : toInt(left) + toInt(right);
            case MINUS        -> toInt(left) - toInt(right);
            case STAR         -> toInt(left) * toInt(right);
            case SLASH        -> toInt(left) / toInt(right);
            case EQUAL_EQUAL  -> (left == null && right == null) ||
                                  (left != null && left.equals(right));
            case BANG_EQUAL   -> !((left == null && right == null) ||
                                   (left != null && left.equals(right)));
            case GREATER      -> toInt(left) > toInt(right);
            case GREATER_EQUAL-> toInt(left) >= toInt(right);
            case LESS         -> toInt(left) < toInt(right);
            case LESS_EQUAL   -> toInt(left) <= toInt(right);
            default           -> throw new RuntimeError("Unknown operator in lambda: " + op);
        };
    }

    private void returnOp() {
        if (stackPointer > 0) pop();
    }
}

// Main interpreter class
class RuneScriptInterpreter {
    private final VM vm = new VM();
    private static boolean hadError = false;

    public void run(String source) {
        hadError = false;

        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();
            if (reportErrors(lexer.getErrors())) return;

            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();
            if (reportErrors(parser.getErrors())) return;

            Resolver resolver = new Resolver(this);
            resolver.resolve(statements);
            if (reportErrors(resolver.getErrors())) return;

            BytecodeEmitter emitter = new BytecodeEmitter();
            for (Stmt stmt : statements) {
                emitter.compileStmt(stmt);
            }
            emitter.emitReturn(0);

            vm.interpret(emitter.getChunk());
        } catch (Exception ex) {
            System.err.println("Runtime error: " + ex.getMessage());
        }
    }

    private boolean reportErrors(List<String> errors) {
        if (errors.isEmpty()) return false;
        errors.forEach(System.err::println);
        hadError = true;
        return true;
    }

    private boolean hadError() {
        return hadError;
    }

    public void setError() {
        hadError = true;
    }
}
