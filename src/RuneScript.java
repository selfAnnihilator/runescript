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
            // For now just emit a simple representation of the bytecode
            // In a real implementation, we'd convert AST to bytecode
        }

        Chunk chunk = emitter.getChunk();
        System.out.println("Bytecode instructions:");
        for (int i = 0; i < chunk.codeSize(); i++) {
            System.out.println(i + ": " + Instruction.OpCode.values()[chunk.codeAt(i)]);
        }
    }

    private static void runPrompt() throws IOException {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

        System.out.println("RuneScript REPL - Type 'exit' to quit");
        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null || line.trim().equals("exit")) break;

            new RuneScriptInterpreter().run(line);
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
    COMMA, DOT, SEMICOLON, SLASH, STAR, COLON, PLUS, MINUS,

    // One or two character tokens
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
    PIPE_ARROW,

    // Literals
    IDENTIFIER, STRING, NUMBER,

    // Keywords
    AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

    // Custom keywords for our language
    LET,

    EOF
}

// AST nodes using sealed interfaces
sealed interface Expr permits
    Expr.Assign, Expr.Binary, Expr.Call, Expr.Grouping,
    Expr.Literal, Expr.Logical, Expr.Unary,
    Expr.Variable, Expr.Pipe {

    record Assign(Token name, Expr value) implements Expr {}

    record Binary(Expr left, Token operator, Expr right) implements Expr {}

    record Call(Expr callee, Token paren, List<Expr> arguments) implements Expr {}

    record Grouping(Expr expression) implements Expr {}

    record Literal(Object value) implements Expr {}

    record Logical(Expr left, Token operator, Expr right) implements Expr {}

    record Unary(Token operator, Expr right) implements Expr {}

    record Variable(Token name) implements Expr {}

    record Pipe(Expr value, Expr call) implements Expr {}
}

// Statement AST nodes
sealed interface Stmt permits
    Stmt.Block, Stmt.Expression, Stmt.If, Stmt.Print,
    Stmt.Var, Stmt.While, Stmt.Assign {

    record Block(List<Stmt> statements) implements Stmt {}

    record Expression(Expr expression) implements Stmt {}

    record If(Expr condition, Stmt thenBranch, Stmt elseBranch) implements Stmt {}

    record Print(Expr expression) implements Stmt {}

    record Var(Token name, Token type, Expr initializer) implements Stmt {}

    record While(Expr condition, Stmt body) implements Stmt {}

    record Assign(Token name, Expr value) implements Stmt {}
}

// Lexer class
class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 0;

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
        keywords.put("int", TokenType.IDENTIFIER);
        keywords.put("bool", TokenType.IDENTIFIER);
        keywords.put("string", TokenType.IDENTIFIER);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme
            start = current;
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
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '*': addToken(TokenType.STAR); break;
            case ':': addToken(TokenType.COLON); break;
            case '+': addToken(TokenType.PLUS); break;
            case '-': addToken(TokenType.MINUS); break;

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
            case '|':
                if (match('>')) {
                    addToken(TokenType.PIPE_ARROW);
                } else {
                    // Report error for unexpected pipe character
                    System.err.println("Unexpected character: |");
                }
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
                    System.err.println("Unexpected character: " + c);
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
            System.err.println("Unterminated string.");
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
        tokens.add(new Token(type, text, literal, line, column));
    }
}

// Parser class
class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
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

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name.");
        Token type = null;

        if (match(TokenType.COLON)) {
            type = consume(TokenType.IDENTIFIER, "Expected type name (int, bool, string).");
        }

        consume(TokenType.EQUAL, "Expected '=' after variable name.");
        Expr initializer = expression();
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration.");

        return new Stmt.Var(name, type, initializer);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        // Removed explicit print statement to allow print() as function call

        return expressionStatement();
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
            } else {
                throw error(equals, "Invalid assignment target.");
            }
        }

        return expr;
    }

    private Expr pipe() {
        Expr expr = equality();

        while (match(TokenType.PIPE_ARROW)) {
            Token pipeArrow = previous();
            Expr call = call();

            expr = new Expr.Pipe(expr, call);
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
            return new Expr.Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
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
        System.err.println("[line " + token.line() + "] Error " + message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == TokenType.SEMICOLON) return;

            switch (peek().type()) {
                case CLASS, FUN, VAR, FOR, IF, WHILE, RETURN:
                    return;
            }

            advance();
        }
    }
}

// Type system
sealed interface Type permits
    Type.IntType, Type.BoolType, Type.StringType, Type.NilType, Type.ErrorType {

    record IntType() implements Type {}
    record BoolType() implements Type {}
    record StringType() implements Type {}
    record NilType() implements Type {}
    record ErrorType() implements Type {}

    static Type fromString(String typeName) {
        return switch (typeName) {
            case "int" -> new Type.IntType();
            case "bool" -> new Type.BoolType();
            case "string" -> new Type.StringType();
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

    public Resolver(RuneScriptInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public void resolve(List<Stmt> statements) {
        beginScope(); // Create global scope
        for (Stmt statement : statements) {
            resolve(statement);
        }
        endScope();
    }

    private void resolve(Stmt stmt) {
        if (stmt instanceof Stmt.Var varStmt) {
            declare(varStmt.name(), varStmt.type() != null ?
                   Type.fromString(varStmt.type().lexeme()) : null);
            if (varStmt.initializer() != null) {
                Type initializerType = resolve(varStmt.initializer());
                Type varType = varStmt.type() != null ?
                              Type.fromString(varStmt.type().lexeme()) : initializerType;
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
        } else if (expr instanceof Expr.Grouping groupExpr) {
            return resolve(groupExpr.expression());
        } else if (expr instanceof Expr.Call callExpr) {
            // For our built-in functions like print()
            if (callExpr.callee() instanceof Expr.Variable var &&
                var.name().lexeme().equals("print")) {
                // print accepts any type
                for (Expr arg : callExpr.arguments()) {
                    resolve(arg);
                }
                return new Type.NilType(); // print returns nil
            }
            // For other function calls, we'd need function type checking
            error(callExpr.paren(), "Function calls not yet supported.");
            return new Type.ErrorType();
        } else if (expr instanceof Expr.Pipe pipeExpr) {
            // Resolve the value part
            Type sourceType = resolve(pipeExpr.value());

            // For pipe, the first argument of the call becomes the piped value
            if (pipeExpr.call() instanceof Expr.Call call) {
                // Prepend the source type to the beginning of the call arguments
                // This would require more complex handling
                for (Expr arg : call.arguments()) {
                    resolve(arg);
                }
                // Return type depends on the called function
                return new Type.IntType(); // Placeholder
            }
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
        System.err.printf("[line %d] Error at '%s': %s%n",
                         token.line(), token.lexeme(), message);
        if (interpreter != null) {
            interpreter.setError();
        }
    }

    private void error(Expr expr, String message) {
        System.err.printf("Error: %s%n", message);
        if (interpreter != null) {
            interpreter.setError();
        }
    }
}

// Instruction definitions
sealed interface Instruction permits
    Instruction.Constant, Instruction.Add, Instruction.Subtract,
    Instruction.Multiply, Instruction.Divide, Instruction.Negate,
    Instruction.True, Instruction.False, Instruction.Nil,
    Instruction.Equal, Instruction.Greater, Instruction.Less,
    Instruction.JumpIfFalse, Instruction.Jump, Instruction.Print,
    Instruction.Pop, Instruction.GetLocal, Instruction.SetLocal,
    Instruction.Return {

    enum OpCode {
        CONSTANT,
        ADD, SUBTRACT, MULTIPLY, DIVIDE, NEGATE,
        TRUE, FALSE, NIL,
        EQUAL, GREATER, LESS,
        JUMP_IF_FALSE, JUMP,
        PRINT, POP,
        GET_LOCAL, SET_LOCAL,
        RETURN
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
    record Jump(int offset) implements Instruction {}
    record Print() implements Instruction {}
    record Pop() implements Instruction {}
    record GetLocal(int slot) implements Instruction {}
    record SetLocal(int slot) implements Instruction {}
    record Return() implements Instruction {}
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
    public int lineAt(int index) { return lines.get(index); }
    public Object constantAt(int index) { return constants.get(index); }
    public int codeSize() { return code.size(); }
}

// Bytecode emitter
class BytecodeEmitter {
    private final Chunk chunk;
    private final Map<String, Integer> locals = new HashMap<>();
    private int localCount = 0;

    public BytecodeEmitter() {
        this.chunk = new Chunk();
    }

    public void emit(byte instruction, int line) {
        chunk.write(instruction, line);
    }
    
    public void emit(int instruction, int line) {
        chunk.write((byte) instruction, line);
    }

    public int addConstant(Object value) {
        return chunk.addConstant(value);
    }

    public void emitConstant(Object value, int line) {
        int constantIndex = addConstant(value);
        emit((byte)Instruction.OpCode.CONSTANT.ordinal(), line);
        emit(constantIndex, line);
    }

    public void declareVariable(String name) {
        locals.put(name, localCount++);
    }

    public Integer getVariableSlot(String name) {
        return locals.get(name);
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void emitReturn(int line) {
        emit((byte)Instruction.OpCode.RETURN.ordinal(), line);
    }

    // Visit methods for different expression types
    public void visitLiteral(Expr.Literal literal, int line) {
        if (literal.value() instanceof Integer i) {
            emitConstant(i, line);
        } else if (literal.value() instanceof String s) {
            emitConstant(s, line);
        } else if (literal.value() instanceof Boolean b) {
            if (b) {
                emit((byte)Instruction.OpCode.TRUE.ordinal(), line);
            } else {
                emit((byte)Instruction.OpCode.FALSE.ordinal(), line);
            }
        } else {
            emit((byte)Instruction.OpCode.NIL.ordinal(), line);
        }
    }

    public void visitBinary(Expr.Binary binary) {
        // Visit left operand
        visitExpression(binary.left());

        // Visit right operand
        visitExpression(binary.right());

        // Emit operator
        TokenType op = binary.operator().type();
        switch (op) {
            case PLUS -> emit((byte)Instruction.OpCode.ADD.ordinal(), binary.operator().line());
            case MINUS -> emit((byte)Instruction.OpCode.SUBTRACT.ordinal(), binary.operator().line());
            case STAR -> emit((byte)Instruction.OpCode.MULTIPLY.ordinal(), binary.operator().line());
            case SLASH -> emit((byte)Instruction.OpCode.DIVIDE.ordinal(), binary.operator().line());
            case EQUAL_EQUAL -> emit((byte)Instruction.OpCode.EQUAL.ordinal(), binary.operator().line());
            case GREATER -> emit((byte)Instruction.OpCode.GREATER.ordinal(), binary.operator().line());
            case LESS -> emit((byte)Instruction.OpCode.LESS.ordinal(), binary.operator().line());
            // Handle other comparison operators as needed
        }
    }

    public void visitUnary(Expr.Unary unary) {
        visitExpression(unary.right());

        TokenType op = unary.operator().type();
        if (op == TokenType.MINUS) {
            emit((byte)Instruction.OpCode.NEGATE.ordinal(), unary.operator().line());
        } else if (op == TokenType.BANG) {
            // For now, we'll just push false or true based on the value
            // More sophisticated implementation needed
        }
    }

    public void visitVariable(Expr.Variable variable) {
        Integer slot = getVariableSlot(variable.name().lexeme());
        if (slot != null) {
            emit((byte)Instruction.OpCode.GET_LOCAL.ordinal(), variable.name().line());
            emit(slot, variable.name().line());
        } else {
            // Error: undefined variable
            System.err.println("Error: Undefined variable " + variable.name().lexeme());
        }
    }

    public void visitAssign(Expr.Assign assign) {
        visitExpression(assign.value());

        Integer slot = getVariableSlot(assign.name().lexeme());
        if (slot != null) {
            emit((byte)Instruction.OpCode.SET_LOCAL.ordinal(), assign.name().line());
            emit(slot, assign.name().line());
        } else {
            // Error: undefined variable
            System.err.println("Error: Undefined variable " + assign.name().lexeme());
        }
    }

    public void visitGrouping(Expr.Grouping grouping) {
        visitExpression(grouping.expression());
    }

    public void visitExpression(Expr expr) {
        if (expr instanceof Expr.Literal literal) {
            visitLiteral(literal, 0); // line number should be extracted properly
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
        }
    }

    private void visitCall(Expr.Call call) {
        // For print() builtin
        if (call.callee() instanceof Expr.Variable var &&
            var.name().lexeme().equals("print")) {
            // Process the argument first
            if (!call.arguments().isEmpty()) {
                visitExpression(call.arguments().get(0)); // Just first argument for now
                emit((byte)Instruction.OpCode.PRINT.ordinal(), call.paren().line());
                // Print returns nil, so push nil as return value
                emit((byte)Instruction.OpCode.NIL.ordinal(), call.paren().line());
            } else {
                // Even if no arguments, print returns nil
                emit((byte)Instruction.OpCode.NIL.ordinal(), call.paren().line());
            }
        } else {
            // Handle other function calls
            // For now, just process arguments
            for (Expr arg : call.arguments()) {
                visitExpression(arg);
            }
        }
    }

    private void visitPipe(Expr.Pipe pipe) {
        // Process the piped value first
        visitExpression(pipe.value());

        // Then process the call with the value as additional parameter
        if (pipe.call() instanceof Expr.Call call) {
            // This is a simplified version - in real implementation,
            // we'd need to handle the piped value as the first argument
            for (Expr arg : call.arguments()) {
                visitExpression(arg);
            }
            emit((byte)Instruction.OpCode.PRINT.ordinal(), 0); // Simplified example
        }
    }
}

// Virtual Machine
class VM {
    private static final int STACK_MAX = 256;
    private final Object[] stack = new Object[STACK_MAX];
    private int stackPointer = 0;

    private Chunk chunk;
    private int ip = 0; // instruction pointer

    public static class RuntimeError extends RuntimeException {
        public RuntimeError(String message) {
            super(message);
        }
    }

    public void resetStack() {
        stackPointer = 0;
    }

    public void interpret(Chunk chunk) {
        this.chunk = chunk;
        this.ip = 0;

        try {
            while (true) {
                if (ip >= chunk.codeSize()) {
                    // Reached end of bytecode
                    break;
                }
                
                Instruction.OpCode instruction = readOpCode();
                switch (instruction) {
                    case CONSTANT: constant(); break;
                    case ADD: binaryOpInt(Integer::sum); break;
                    case SUBTRACT: binaryOpInt((a, b) -> (Integer)a - (Integer)b); break;
                    case MULTIPLY: binaryOpInt((a, b) -> (Integer)a * (Integer)b); break;
                    case DIVIDE: binaryOpInt((a, b) -> (Integer)a / (Integer)b); break;
                    case NEGATE: negate(); break;
                    case TRUE: push(true); break;
                    case FALSE: push(false); break;
                    case NIL: push(null); break;
                    case EQUAL: equality(); break;
                    case GREATER: binaryOpCompare((a, b) -> a > b); break;
                    case LESS: binaryOpCompare((a, b) -> a < b); break;
                    case PRINT: print(); break;
                    case POP: pop(); break;
                    case GET_LOCAL: getLocal(); break;
                    case SET_LOCAL: setLocal(); break;
                    case RETURN: returnOp(); return; // Exit the interpret method
                    case JUMP_IF_FALSE: jumpIfFalse(); break;
                    case JUMP: jump(); break;
                    default:
                        throw new RuntimeError("Unknown opcode: " + instruction);
                }
            }
        } catch (RuntimeError e) {
            System.err.println(e.getMessage());
            return;
        }
    }

    private Instruction.OpCode readOpCode() {
        return Instruction.OpCode.values()[chunk.codeAt(ip++)];
    }

    private Object readConstant() {
        int constantIndex = chunk.codeAt(ip++);
        return chunk.constantAt(constantIndex);
    }

    private void push(Object value) {
        stack[stackPointer++] = value;
    }

    private Object pop() {
        if (stackPointer <= 0) {
            throw new RuntimeError("Stack underflow: Attempting to pop from empty stack");
        }
        return stack[--stackPointer];
    }

    private Object peek(int distance) {
        return stack[stackPointer - 1 - distance];
    }

    private void constant() {
        Object value = readConstant();
        // Convert numeric values to integers when appropriate
        if (value instanceof Double) {
            Double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                push(d.intValue());
            } else {
                push(value); // Keep as Double for non-integer values
            }
        } else {
            push(value);
        }
    }

    private void binaryOpInt(BinaryOperator<Integer> op) {
        Object b = pop();
        Object a = pop();

        Integer intA = convertToInteger(a);
        Integer intB = convertToInteger(b);

        if (intA == null || intB == null) {
            throw new RuntimeError("Operands must be numbers.");
        }

        push(op.apply(intA, intB));
    }
    
    private Integer convertToInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Double) {
            Double d = (Double) value;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return d.intValue();
            }
        }
        return null;
    }

    private void binaryOpCompare(BiFunction<Integer, Integer, Boolean> op) {
        Object b = pop();
        Object a = pop();

        Integer intA = convertToInteger(a);
        Integer intB = convertToInteger(b);

        if (intA == null || intB == null) {
            throw new RuntimeError("Operands must be numbers.");
        }

        Boolean result = op.apply(intA, intB);
        push(result);
    }

    private void negate() {
        Object value = pop();

        Integer intValue = convertToInteger(value);
        if (intValue == null) {
            throw new RuntimeError("Operand must be a number.");
        }

        push(-intValue);
    }

    private void equality() {
        Object b = pop();
        Object a = pop();

        push(a.equals(b));
    }

    private void print() {
        Object value = pop();
        System.out.println(value);
    }

    private void getLocal() {
        int slot = chunk.codeAt(ip++);
        push(stack[slot]);
    }

    private void setLocal() {
        int slot = chunk.codeAt(ip++);
        stack[slot] = peek(0);  // Don't pop, we want to keep the value on the stack
    }

    private void jumpIfFalse() {
        int offset = chunk.codeAt(ip++);
        if (!(Boolean)peek(0)) {
            ip += offset;
        } else {
            pop();  // Remove the boolean value from stack
        }
    }

    private void jump() {
        int offset = chunk.codeAt(ip++);
        ip += offset;
    }

    private void returnOp() {
        // The return value should be on top of the stack
        Object result = null;
        if (stackPointer > 0) {
            result = pop();
        }
        System.out.println("Returned: " + result);
        // The return instruction should cause the interpret method to exit
        // This is handled by the caller returning from the interpret method
    }
}

// Main interpreter class
class RuneScriptInterpreter {
    private final VM vm = new VM();
    private static boolean hadError = false;

    public void run(String source) {
        // Reset error flag
        hadError = false;
        
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();

            Parser parser = new Parser(tokens);
            List<Stmt> statements = parser.parse();

            if (hadError()) return;

            // Resolve variables and types
            Resolver resolver = new Resolver(this);
            resolver.resolve(statements);

            if (hadError()) return;

            // Emit bytecode
            BytecodeEmitter emitter = new BytecodeEmitter();
            // Convert statements to bytecode (simplified implementation)
            for (Stmt stmt : statements) {
                // Convert each statement to bytecode
                if (stmt instanceof Stmt.Expression exprStmt) {
                    // Convert expression to bytecode
                    emitter.visitExpression(exprStmt.expression());
                    emitter.emit((byte)Instruction.OpCode.POP.ordinal(), 0); // Pop result
                } else if (stmt instanceof Stmt.Var varStmt) {
                    // Declare variable in emitter
                    emitter.declareVariable(varStmt.name().lexeme());
                    if (varStmt.initializer() != null) {
                        emitter.visitExpression(varStmt.initializer());
                        Integer slot = emitter.getVariableSlot(varStmt.name().lexeme());
                        if (slot != null) {
                            emitter.emit((byte)Instruction.OpCode.SET_LOCAL.ordinal(), 0);
                            emitter.emit((byte)(int)slot, 0);
                        }
                    }
                } else if (stmt instanceof Stmt.Assign assignStmt) {
                    emitter.visitExpression(assignStmt.value());
                    Integer slot = emitter.getVariableSlot(assignStmt.name().lexeme());
                    if (slot != null) {
                        emitter.emit((byte)Instruction.OpCode.SET_LOCAL.ordinal(), 0);
                        emitter.emit((byte)(int)slot, 0);
                    }
                }
            }

            emitter.emitReturn(0);

            Chunk chunk = emitter.getChunk();
            vm.interpret(chunk);
        } catch (Exception ex) {
            System.err.println("Runtime error: " + ex.getMessage());
        }
    }

    private boolean hadError() {
        return hadError;
    }
    
    public void setError() {
        hadError = true;
    }
}