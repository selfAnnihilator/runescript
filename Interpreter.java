import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Interpreter {
    private final VM vm = new VM();

    public static void main(String[] args) throws IOException {
        if (args.length > 2) {
            System.out.println("Usage: runescript [script]");
            System.exit(64);
        } else if (args.length == 2) {
            if (args[0].equals("--emit-tokens")) {
                runPrintTokens(args[1]);
            } else if (args[0].equals("--emit-ast")) {
                runPrintAST(args[1]);
            } else if (args[0].equals("--emit-bytecode")) {
                runPrintBytecode(args[1]);
            } else {
                System.out.println("Usage: runescript [--emit-tokens|--emit-ast|--emit-bytecode] script");
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
        new Interpreter().run(new String(bytes, Charset.defaultCharset()));
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
        Resolver resolver = new Resolver(null);
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
        java.io.BufferedReader reader = 
            new java.io.BufferedReader(new java.io.InputStreamReader(System.in));

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();
            if (line == null) break;
            
            new Interpreter().run(line);
        }
    }

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

    private static boolean hadError = false;

    private boolean hadError() {
        return hadError;
    }

    public void setError() {
        hadError = true;
    }
}