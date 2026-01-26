import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver {
    private final Interpreter interpreter;
    private final Stack<Map<String, Type>> scopes = new Stack<>();
    
    public Resolver(Interpreter interpreter) {
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
    }
}