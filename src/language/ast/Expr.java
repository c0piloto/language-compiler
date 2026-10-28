package language.ast;

public abstract class Expr extends Node {
  protected Expr(int line, int col) {
    super(line, col);
  }
}
