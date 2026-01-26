import java.util.*;

// Instruction.java
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