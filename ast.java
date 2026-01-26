import java.util.List;

// Base interface for all expressions
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

// Base interface for all statements
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