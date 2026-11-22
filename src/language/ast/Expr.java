package language.ast;

import language.symbols.Type;

public abstract class Expr extends Node {
  private Type type = null;

  protected Expr(int line, int col) {
    super(line, col);
  }

  public Type getType() {
    return this.type;
  }

  public void setType(Type type) {
    this.type = type;
  }
}
