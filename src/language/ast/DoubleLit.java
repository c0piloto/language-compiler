package language.ast;

import language.visitor.Visitor;

public final class DoubleLit extends Expr {
  private final String lexeme;

  public DoubleLit(String lexeme, int line, int col) {
    super(line, col);
    this.lexeme = lexeme;
  }

  public String getLexeme() {
    return this.lexeme;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitDoubleLit(this);
  }
}
