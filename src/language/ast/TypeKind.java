package language.ast;

public enum TypeKind {
  INT("int"),
  DOUBLE("double"),
  BOOL("bool"),
  CHAR("char"),
  STRING("string"),
  VOID("void");

  public final String symbol;

  TypeKind(String symbol) {
    this.symbol = symbol;
  }
}
