package language.lexer;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
  private final String src;
  private int pos = 0, line = 1, col = 1;
  private final List<LexicalError> errors = new ArrayList<>();
  private int tokLine;
  private int tokCol;

  public Lexer(String src) {
    this.src = src;
  }

  public List<LexicalError> getErrors() {
    return this.errors;
  }

  // q0 state
  public Token nextToken() {
    while (true) {
      ignoreSpaces();

      if (end()) {
        return new Token(TokenType.EOF, "", this.line, this.col);
      }

      this.tokLine = this.line;
      this.tokCol = this.col;
      char c = peek();
      Token token;

      if (isLetter(c)) {
        token = readIdentifier();
      } else if (isDigit(c)) {
        token = readNumber();
      } else if (c == '"') {
        token = readString();
      } else if (c == '\'') {
        token = readChar();
      } else if (c == '/') {
        token = readSlash();
      } else if ("=<>!&|+-*".indexOf(c) >= 0) {
        token = readOperator();
      } else if ("(){},;".indexOf(c) >= 0) {
        token = readDelimiter();
      } else {
        error("Character not in alphabet: '" + c + "'", this.tokLine, this.tokCol);
        advance();
        token = null;
      }

      if (token != null) {
        return token;
      }
    }
  }

  private Token readIdentifier() {
    throw new UnsupportedOperationException("TODO identificador.dot");
  }

  private Token readNumber() {
    throw new UnsupportedOperationException("TODO numero.dot");
  }

  private Token readString() {
    throw new UnsupportedOperationException("TODO string.dot");
  }

  private Token readChar() {
    throw new UnsupportedOperationException("TODO char.dot");
  }

  private Token readSlash() {
    throw new UnsupportedOperationException("TODO comentario.dot");
  }

  private Token readOperator() {
    throw new UnsupportedOperationException("TODO operador.dot");
  }

  private Token readDelimiter() {
    throw new UnsupportedOperationException("TODO delimitador.dot");
  }

  private void ignoreSpaces() {
    while (!end()) {
      char c = peek();
      if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        advance();
      } else {
        break;
      }
    }
  }

  private boolean end() {
    return pos >= src.length();
  }

  private char peek() {
    return peek(0);
  }

  private char peek(int n) {
    int i = pos + n;
    return i < src.length() ? src.charAt(i) : '\0';
  }

  private char advance() {
    char c = src.charAt(pos++);
    if (c == '\n') {
      this.line++;
      this.col = 1;
    } else {
      this.col++;
    }
    return c;
  }

  private Token token(TokenType type, String lexema) {
    return new Token(type, lexema, this.tokLine, this.tokCol);
  }

  private void errors(String message, int line, int col) {
    errors.add(new LexicalError(message, line, col));
  }

  private static boolean isLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
