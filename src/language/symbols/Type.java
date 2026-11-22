package language.symbols;

import language.ast.TypeKind;

public enum Type {
  INT("int"), DOUBLE("double"), BOOL("bool"), CHAR("char"), STRING("string"), VOID("void"), ERROR("error");

  private final String label;

  Type(String label) {
    this.label = label;
  }

  public static Type of(TypeKind kind) {
    if(kind == null) return ERROR;
    return switch(kind) {
      case INT -> INT;
      case DOUBLE -> DOUBLE;
      case BOOL -> BOOL;
      case CHAR -> CHAR;
      case STRING -> STRING;
      case VOID -> VOID;
    };
  }

  public boolean isNumeric() {
    return this == INT || this == DOUBLE;
  }

  public boolean isError() {
    return this == ERROR;
  }

  // Relation T1 <= T2: same type, or int -> double. ERROR combines with everything
  public boolean isAssignableTo(Type target) {
    if(this == ERROR || target == ERROR) return true;
    if(this == target) return true;
    return this == INT && target == DOUBLE;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
