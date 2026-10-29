package language.ast;

import language.visitor.Visitor;

public final class CharLit extends Expr {
  private String lexeme;

  public CharLit(String lexeme, int line, int col) {
    super(line, col);
    this.lexeme = lexeme;
  }

  public String getLexeme() {
	return lexeme;
  }
  
  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitCharLit();
  }
}
