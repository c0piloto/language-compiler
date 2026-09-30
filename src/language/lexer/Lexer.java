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
        token = readComment();
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
    StringBuilder lexema = new StringBuilder();
    lexema.append(advance());
    int state = 1;

    while (true) {
      char c = peek();
      switch (state) {
        case 1 -> {
          if (isLetter(c) || isDigit(c) || c == '_') {
            lexema.append(advance());
          } else {
            state = 2;
          }
        }
        case 2 -> {
          String s = lexema.toString();
          return token(TokenType.identifierType(s), s);
        }
      }
    }
  }

  private Token readNumber() {
    StringBuilder lexema = new StringBuilder();
    lexema.append(advance());
    int state = 1;

    while (true) {
      char c = peek();
      switch (state) {
        case 1 -> {
          if (isDigit(c)) {
            lexema.append(advance());
          } else if (c == '.') {
            lexema.append(advance());
            state = 2;
          } else if (isLetter(c)) {
            return gluedToLetter();
          } else {
            state = 4;
          }
        }
        case 2 -> {
          if (isDigit(c)) {
            lexema.append(advance());
            state = 3;
          } else {
            error("Dot w/o digit after it. eg.: 3.", this.tokLine, this.tokCol);
            return null;
          }
        }
        case 3 -> {
          if (isDigit(c)) {
            lexema.append(advance());
          } else if (isLetter(c)) {
            return gluedToLetter();
          } else {
            state = 5;
          }
        }
        case 4 -> {
          return token(TokenType.LIT_INT, lexema.toString());
        }
        case 5 -> {
          return token(TokenType.LIT_DOUBLE, lexema.toString());
        }
      }
    }

  }

  private Token readString() {
    StringBuilder lexema = new StringBuilder();
    lexema.append(advance());
    int state = 1;

    while (true) {
      char c = peek();
      switch (state) {
        case 1 -> {
          if (end() || c == '\n' || c == '\r') {
            error("Unclosed string (end of line)", this.tokLine, this.tokCol);
            return null;
          } else if (c == '"') {
            lexema.append(advance());
            state = 3;
          } else if (c == '\\') {
            lexema.append(advance());
            state = 2;
          } else {
            lexema.append(advance());
          }
        }
        case 2 -> {
          if (c == '"' || c == '\\' || c == 'n' || c == 't') {
            lexema.append(advance());
          } else {
            error("Unknown escape sequence", this.tokLine, this.tokCol);
          }
          state = 1;
        }
        case 3 -> {
          return token(TokenType.LIT_STRING, lexema.toString());
        }
      }
    }
  }

  private Token readChar() {
    StringBuilder lexema = new StringBuilder();
    lexema.append(advance());
    int state = 1;

    while (true) {
      char c = peek();
      boolean lineEnd = end() || c == '\n' || c == '\r';

      switch (state) {
        case 1 -> {
          if (c == '\'') {
            advance();
            error("Empty character", this.tokLine, this.tokCol);
            return null;
          } else if (lineEnd) {
            error("Unclosed char", this.tokLine, this.tokCol);
            return null;
          } else if (c == '\\') {
            lexema.append(advance());
            state = 2;
          } else {
            lexema.append(advance());
            state = 3;
          }
        }
        case 2 -> {
          if (c == '\'' || c == 'n' || c == 't' || c == '\\') {
            lexema.append(advance());
          } else if (!lineEnd) {
            error("Unknown escape sequence", this.tokLine, this.tokCol);
            lexema.append(advance());
          }
          state = 3;
        }
        case 3 -> {
          if (c == '\'') {
            lexema.append(advance());
            state = 4;
          } else {
            if (lineEnd) {
              error("Unclosed char", this.tokLine, this.tokCol);
            } else {
              error("Char with more than one character", this.tokLine, this.tokCol);
              while (!end() && peek() != '\'' && peek() != '\n' && peek() != '\r') {
                advance();
              }
              if (peek() == '\'') {
                advance();
              }
            }
            return null;
          }
        }
        case 4 -> {
          return token(TokenType.LIT_CHAR, lexema.toString());
        }
      }
    }
  }

  private Token readComment() {
    advance();
    int state = 1;

    while (true) {
      char c = peek();
      boolean lineEnd = end() || c == '\n' || c == '\r';

      switch (state) {
        case 1 -> {
          if (c == '/') {
            advance();
            state = 2;
          } else if (c == '*') {
            advance();
            state = 3;
          } else {
            return token(TokenType.SLASH, "/");
          }
        }
        case 2 -> {
          if (lineEnd) {
            return null;
          }
          advance();
        }
        case 3 -> {
          if (end()) {
            error("Comment block not closed", this.tokLine, this.tokCol);
            return null;
          } else if (c == '*') {
            advance();
            state = 4;
          } else {
            advance();
          }
        }
        case 4 -> {
          if (end()) {
            error("Comment block not closed", this.tokLine, this.tokCol);
            return null;
          } else if (c == '/') {
            return token(TokenType.SLASH, "/");
          } else {
            advance();
            state = 3;
          }
        }
      }
    }
  }

  private Token readOperator() {
    char c = advance();

    switch (c) {
      case '=' -> {
        return match('=') ? token(TokenType.EQ, "==") : token(TokenType.ASSIGN, "=");
      }
      case '<' -> {
        return match('=') ? token(TokenType.LE, "<=") : token(TokenType.LT, "<");
      }
      case '>' -> {
        return match('=') ? token(TokenType.GE, ">=") : token(TokenType.GT, ">");
      }
      case '!' -> {
        return match('=') ? token(TokenType.NE, "!=") : token(TokenType.NOT, "!");
      }
      case '&' -> {
        if (match('&')) {
          return token(TokenType.AND, "&&");
        } else {
          error("Isolated '&' (expected '&&')", this.tokLine, this.tokCol);
          return null;
        }
      }
      case '|' -> {
        if (match('|')) {
          return token(TokenType.OR, "||");
        } else {
          error("Isolated '|' (expected '||')", this.tokLine, this.tokCol);
          return null;
        }
      }
      case '+' -> {
        return token(TokenType.PLUS, "+");
      }
      case '-' -> {
        return token(TokenType.MINUS, "-");
      }
      case '*' -> {
        return token(TokenType.STAR, "*");
      }
      default -> throw new IllegalStateException("readOperator called with '" + c + "'");
    }
  }

  private Token readDelimiter() {
    char c = advance();

    TokenType token = switch (c) {
      case '(' -> TokenType.LPAREN;
      case ')' -> TokenType.RPAREN;
      case '{' -> TokenType.LBRACE;
      case '}' -> TokenType.RBRACE;
      case ',' -> TokenType.COMMA;
      case ';' -> TokenType.SEMICOLON;
      default -> throw new IllegalStateException("readDelimiter called with '" + c + "'");
    };

    return token(token, String.valueOf(c));
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

  private Token gluedToLetter() {
    error("Number followed by letter w/o space!", this.tokLine, this.tokCol);
    while (isLetter(peek()) || isDigit(peek()) || peek() == '_') {
      advance();
    }
    return null;
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

  private boolean match(char expected) {
    if (peek() == expected) {
      advance();
      return true;
    }

    return false;
  }

  private Token token(TokenType type, String lexema) {
    return new Token(type, lexema, this.tokLine, this.tokCol);
  }

  private void error(String message, int line, int col) {
    errors.add(new LexicalError(message, line, col));
  }

  private static boolean isLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private static boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
}
