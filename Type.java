// Type.java
public sealed interface Type permits
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