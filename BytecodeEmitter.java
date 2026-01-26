import java.util.*;

// BytecodeEmitter.java
public class BytecodeEmitter {
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
        emit((byte)constantIndex, line);
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
            visitLiteral(literal, literal.value() != null ? 0 : 0); // Extract line from expression if possible
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