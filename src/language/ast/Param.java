package language.ast;

import language.visitor.Visitor;

public final class Param extends Node {
  private final TypeKind type;
  private final String name;
  // Mais tarde: private Symbol symbol;

  public Param(TypeKind type, String name, int line, int col) {
    super(line, col);
    this.type = type;
    this.name = name;
  }

  public TypeKind getType() {
    return this.type;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitParam(this);
  }
}
