package language.ast;

import java.util.List;

import language.visitor.Visitor;

public final class Program extends Node {
  private final List<Node> decls;

  public Program(List<Node> decls, int line, int col) {
    super(line, col);
    this.decls = decls;
  }

  public List<Node> getDeclarations() {
    return this.decls;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitProgram(this);
  }
}
