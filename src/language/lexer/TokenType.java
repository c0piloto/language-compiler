package language.lexer;

import java.util.HashMap;
import java.util.Map;

public enum TokenType {
  // Identificator and Literals
  IDENTIFIER,
  LIT_INT,
  LIT_DOUBLE,
  LIT_STRING,
  LIT_CHAR,

  // Keywords
  KW_INT("int"),
  KW_DOUBLE("double"),
  KW_BOOL("bool"),
  KW_CHAR("char"),
  KW_STRING("string"),
  KW_VOID("void"),
  KW_RETURN("return"),
  KW_IF("if"),
  KW_ELSE("else"),
  KW_WHILE("while"),
  KW_TRUE("true"),
  KW_FALSE("false"),

  // Operators
  ASSIGN("="),
  EQ("=="),
  LT("<"),
  LE("<="),
  GT(">"),
  GE(">="),
  NE("!="),
  NOT("!"),
  PLUS("+"),
  MINUS("-"),
  STAR("*"),
  SLASH("/"),
  AND("&&"),
  OR("||"),

  // Delimiters
  LPAREN("("),
  RPAREN(")"),
  LBRACE("{"),
  RBRACE("}"),
  COMMA(","),
  SEMICOLON(";"),

  // End of file
  EOF; // Indicates end of file is NULL

  public final String lexema;

  TokenType() {
    this(null);
  }

  TokenType(String lexema) {
    this.lexema = lexema;
  }

  // Keywords chart: lexema -> type
  private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

  static {
    for (TokenType t : values()) {
      if (t.name().startsWith("KW_")) {
        KEYWORDS.put(t.lexema, t);
      }
    }
  }

  // Consult the chart, basically if the keyword is there it returns that, if not
  // returns IDENTIFICATOR
  public static TokenType identifierType(String lexema) {
    return KEYWORDS.getOrDefault(lexema, IDENTIFIER);
  }
}
