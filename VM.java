import java.util.List;

public class VM {
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
    
    private void binaryOpInt(java.util.function.BinaryOperator<Integer> op) {
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

    private void binaryOpCompare(java.util.function.BiFunction<Integer, Integer, Boolean> op) {
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