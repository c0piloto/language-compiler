package language.ast;

import language.visitor.Visitor;

public abstract class Node {
  private final int line, col;
  protected Node(int line, int col) {
    this.line = line;
    this.col = col;
  }
  public int getLine() {
    return this.line;
  }
  public int getCol() {
    return this.col;
  }

  public abstract <T> T accept(Visitor<T> visitor);
}
