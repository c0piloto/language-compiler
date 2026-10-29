package language.ast;

import language.visitor.Visitor;

public final class StringLit extends Expr {
  // Lexema como o Lexer entrega: com as aspas e os escapes ainda escritos
  private final String lexeme;

  public StringLit(String lexeme, int line, int col) {
    super(line, col);
    this.lexeme = lexeme;
  }

  public String getLexeme() {
    return this.lexeme;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitStringLit(this);
  }
}
