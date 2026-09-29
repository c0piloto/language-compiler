package language.lexer;

public final class Token {
  private final TokenType type;
  private final String lexema;
  private final int line;
  private final int col;

  public Token(TokenType type, String lexema, int line, int col) {
    this.type = type;
    this.lexema = lexema;
    this.line = line;
    this.col = col;
  }

  // Getters
  public TokenType getType() {
    return this.type;
  }

  public String getLexema() {
    return this.lexema;
  }

  public int getLine() {
    return this.line;
  }

  public int getCol() {
    return this.col;
  }

  @Override
  public String toString() {
    return String.format("%d:%d\t%s\t'%s'", this.line, this.col, this.type, this.lexema);
  }
}
