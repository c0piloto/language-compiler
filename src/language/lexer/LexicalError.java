package language.lexer;

public final class LexicalError {
  private final String message;
  private final int line;
  private final int col;

  public LexicalError(String message, int line, int col) {
    this.message = message;
    this.line = line;
    this.col = col;
  }

  // Getters
  public String getMessage() {
    return this.message;
  }

  public int getLine() {
    return this.line;
  }

  public int getCol() {
    return this.col;
  }

  @Override
  public String toString() {
    return String.format("Lexical error [%d:%d]: %s", this.line, this.col, this.message);
  }
}
