package language.ast;

public abstract class Expr extends Node {
  // Mais tarde: private Type type;

  protected Expr(int line, int col) {
    super(line, col);
  }
}
