import java.util.*;

// Chunk.java - Stores bytecode and constants
public class Chunk {
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