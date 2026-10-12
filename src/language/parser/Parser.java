package language.parser;

import language.lexer.Lexer;
import language.lexer.Token;
import language.lexer.TokenType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class Parser {
  private final Lexer lexer;
  private Token token; // Lookahead
  private final List<String> errors = new ArrayList<>();

  // Internal exception just to abort in the first error
  private static final class ParseError extends RuntimeException {
    ParseError(String msg) {
      super(msg, null, false, false);
    }
  }

  public Parser(Lexer lexer) {
    this.lexer = lexer;
    this.token = lexer.nextToken();
  }

  public Token getToken() {
    return this.token;
  }

  public List<String> getErrors() {
    return errors;
  }

  private void advance() {
    this.token = this.lexer.nextToken();
  }

  private boolean at(TokenType type) {
    return this.token.getType() == type;
  }

  private boolean inSet(Set<TokenType> set) {
    return set.contains(this.token.getType());
  }

  // General matching method
  private Token match(TokenType expected) {
    if (!at(expected)) {
      fail(EnumSet.of(expected));
    }

    Token consumed = this.token;
    advance();
    return consumed;
  }

  // Error method
  private void fail(Set<TokenType> expected) {
    throw new RuntimeException(String.format(
          "Sintax error %d:%d expected %s, found %s ('%s')",
          this.token.getLine(), this.token.getCol(), Arrays.toString(expected), this.token.getType(), this.token.getLexema();
          ));
  }

  // API

  // true = acepted; false = rejected (see getErrors()).
  public boolean parse() {
    errors.clear();

    try {
      parseProgram();
      match(TokenType.EOF);
      return true;
    } catch (ParseError e) {
      errors.add(e.getMessage());
      return false;
    }
  }

  // FIRST / FOLLOW sets

  // Todo: Fill the rest of the first and follows in the EnumSet.of()
  private static final Set<TokenType> FIRST_STMT = EnumSet.of(TokenType.EOF);
  private static final Set<TokenType> FOLLOW_STMT_LIST = EnumSet.of(TokenType.EOF);

  // One method per non-terminal

  // Program -> StmtList
  private void parseProgram() {
    parseStmtList();
  }

  // StmtList -> Stmt StmtList | ε
  private void parseStmtList() {
    // Non empty alternative, decided by FIRST
    if (inSet(FIRST_STMT)) {
      parseStmt();
      parseStmtList();
    } else if (inSet(FOLLOW_STMT_LIST)) { // ε: only acepted if lookahead ∈ FOLLOW
      return;
    } else {
      Set<TokenType> expected = EnumSet.copyOf(FIRST_STMT);
      expected.addAll(FOLLOW_STMT_LIST);
      fail(expected);
    }
  }

  // Stmt -> IfStmt | ...
  private void parseStmt() {
    switch (token.getType()) {
      // case IF -> parseIfStmt();
      default -> fail(FIRST_STMT);
    }
  }

  // IfStmt -> 'if' '(' Expr ')' Stmt
  private void parseIfStmt() {
    match(TokenType.IF);
    match(TokenType.LPAREN);
    // parseExpr();
    match(TokenType.RPAREN);
    parseStmt();
  }

  // Todo the rest of the non-terminal parsers
}
