package language.ast;

import java.util.List;

import language.visitor.Visitor;

public final class Program extends Node {
  private List<Node> decls;

  public Program(Node declaration, int line, int col) {
    super(line, col);
    this.decls.add(declaration);
  }

  public List<Node> getDeclarations() {
    return this.decls;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitProgram(this);
  }
}
