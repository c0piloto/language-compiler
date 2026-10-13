package language.parser;

import language.lexer.Lexer;
import language.lexer.Token;
import language.lexer.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static language.lexer.TokenType.*;

public final class Parser {
  private final Lexer lexer;
  private Token token; // Lookahead
  private final List<String> errors = new ArrayList<>();

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
      return this.token;
    }

    Token consumed = this.token;
    advance();
    return consumed;
  }

  // Error method (registra o erro sem interromper a análise)
  private void fail(Set<TokenType> expected) {
    errors.add(String.format(
        "Syntax error %d:%d expected %s, found %s ('%s')",
        this.token.getLine(), this.token.getCol(), expected,
        this.token.getType(), this.token.getLexema()));
  }

  // Recuperação em Modo Pânico (descarta tokens até sincronizar)
  private void synchronize(Set<TokenType> syncSet) {
    while (!at(EOF)) {
      if (syncSet.contains(token.getType())) {
        return;
      }
      advance();
    }
  }

  // API

  // true = aceito sem erros; false = rejeitado (ver getErrors()).
  public boolean parse() {
    errors.clear();
    parseProgram();
    if (!at(EOF)) {
      fail(EnumSet.of(EOF));
      synchronize(EnumSet.of(EOF));
    }
    return errors.isEmpty();
  }

  // Conjuntos FIRST / FOLLOW
// Helpers para união de conjuntos
  private static Set<TokenType> union(Set<TokenType> base, TokenType... extra) {
    EnumSet<TokenType> result = EnumSet.copyOf(base);
    Collections.addAll(result, extra);
    return result;
  }

  private static Set<TokenType> union(Set<TokenType> base, Set<TokenType> extra) {
    EnumSet<TokenType> result = EnumSet.copyOf(base);
    result.addAll(extra);
    return result;
  }

  // T: tipos básicos (início de declaração)
  private static final Set<TokenType> T = EnumSet.of(
      KW_INT, KW_DOUBLE, KW_BOOL, KW_CHAR, KW_STRING, KW_VOID);

  private static final Set<TokenType> FIRST_PRIMARY = EnumSet.of(
      LIT_INT, LIT_DOUBLE, LIT_STRING, LIT_CHAR, KW_TRUE, KW_FALSE, LPAREN, IDENTIFIER);

  // E: início de expressão
  private static final Set<TokenType> E = union(FIRST_PRIMARY, NOT, MINUS);

  // S: início de comando
  private static final Set<TokenType> S = union(T, KW_IF, KW_WHILE, KW_RETURN, LBRACE, IDENTIFIER);

  // F: o que pode seguir um comando
  private static final Set<TokenType> F = union(S, RBRACE);

  // X: o que pode seguir uma expressão completa
  private static final Set<TokenType> X = EnumSet.of(SEMICOLON, RPAREN, COMMA);

  // L1..L6: FOLLOW dos níveis de expressão
  private static final Set<TokenType> L1 = union(X, OR);
  private static final Set<TokenType> L2 = union(L1, AND);
  private static final Set<TokenType> L3 = union(L2, EQ, NE);
  private static final Set<TokenType> L4 = union(L3, LT, LE, GT, GE);
  private static final Set<TokenType> L5 = union(L4, PLUS, MINUS);
  private static final Set<TokenType> L6 = union(L5, STAR, SLASH);

  // Program -> Decl Program | ε
  private void parseProgram() {
    if (inSet(T)) {
      parseDecl();
      parseProgram();
    } else if (at(EOF)) {
      return;
    } else {
      fail(union(T, EOF));
      synchronize(union(T, EOF));
      if (!at(EOF)) {
        parseProgram();
      }
    }
  }

  // Decl -> Type ID DeclRest
  private void parseDecl() {
    parseType();
    match(IDENTIFIER);
    parseDeclRest();
  }

  // DeclRest -> ( Params ) Block | VarRest ;
  private void parseDeclRest() {
    if (at(LPAREN)) {
      match(LPAREN);
      parseParams();
      match(RPAREN);
      parseBlock();
    } else if (at(ASSIGN) || at(SEMICOLON)) {
      parseVarRest();
      match(SEMICOLON);
    } else {
      fail(EnumSet.of(LPAREN, ASSIGN, SEMICOLON));
      synchronize(union(T, EnumSet.of(SEMICOLON, RBRACE)));
    }
  }

  // VarRest -> = Expr | ε
  private void parseVarRest() {
    if (at(ASSIGN)) {
      match(ASSIGN);
      parseExpr();
    } else if (at(SEMICOLON)) {
      return;
    } else {
      fail(EnumSet.of(ASSIGN, SEMICOLON));
      synchronize(EnumSet.of(SEMICOLON));
    }
  }

  // Params -> Param ParamTail | ε
  private void parseParams() {
    if (inSet(T)) {
      parseParam();
      parseParamTail();
    } else if (at(RPAREN)) {
      return;
    } else {
      fail(union(T, RPAREN));
      synchronize(EnumSet.of(RPAREN));
    }
  }

  // ParamTail -> , Param ParamTail | ε
  private void parseParamTail() {
    if (at(COMMA)) {
      match(COMMA);
      parseParam();
      parseParamTail();
    } else if (at(RPAREN)) {
      return;
    } else {
      fail(EnumSet.of(COMMA, RPAREN));
      synchronize(EnumSet.of(RPAREN));
    }
  }

  // Param -> Type ID
  private void parseParam() {
    parseType();
    match(IDENTIFIER);
  }

  // Type -> int | double | bool | char | string | void
  private void parseType() {
    if (inSet(T)) {
      advance();
    } else {
      fail(T);
      synchronize(union(T, IDENTIFIER, SEMICOLON));
    }
  }

  // Block -> { StmtList }
  private void parseBlock() {
    match(LBRACE);
    parseStmtList();
    match(RBRACE);
  }

  // StmtList -> Stmt StmtList | ε
  private void parseStmtList() {
    if (inSet(S)) {
      parseStmt();
      parseStmtList();
    } else if (at(RBRACE)) {
      return;
    } else {
      fail(union(S, RBRACE));
      synchronize(F);
      if (!at(RBRACE) && inSet(S)) {
        parseStmtList();
      }
    }
  }

  // Stmt -> Type ID VarRest ; | ID IdStmt | IfStmt | Block
  // | while ( Expr ) Block | return ReturnVal ;
  private void parseStmt() {
    if (inSet(T)) {
      parseType();
      match(IDENTIFIER);
      parseVarRest();
      match(SEMICOLON);
      return;
    }

    switch (token.getType()) {
      case IDENTIFIER -> {
        match(IDENTIFIER);
        parseIdStmt();
      }
      case KW_IF -> parseIfStmt();
      case LBRACE -> parseBlock();
      case KW_WHILE -> {
        match(KW_WHILE);
        match(LPAREN);
        parseExpr();
        match(RPAREN);
        parseBlock();
      }
      case KW_RETURN -> {
        match(KW_RETURN);
        parseReturnVal();
        match(SEMICOLON);
      }
      default -> {
        fail(S);
        synchronize(F);
      }
    }
  }

  // IdStmt -> = Expr ; | ( Args ) ;
  private void parseIdStmt() {
    switch (token.getType()) {
      case ASSIGN -> {
        match(ASSIGN);
        parseExpr();
        match(SEMICOLON);
      }
      case LPAREN -> {
        match(LPAREN);
        parseArgs();
        match(RPAREN);
        match(SEMICOLON);
      }
      default -> {
        fail(EnumSet.of(ASSIGN, LPAREN));
        synchronize(EnumSet.of(SEMICOLON));
      }
    }
  }

  // IfStmt -> if ( Expr ) Block ElsePart
  private void parseIfStmt() {
    match(KW_IF);
    match(LPAREN);
    parseExpr();
    match(RPAREN);
    parseBlock();
    parseElsePart();
  }

  // ElsePart -> else ElseBody | ε
  private void parseElsePart() {
    if (at(KW_ELSE)) {
      match(KW_ELSE);
      parseElseBody();
    } else if (inSet(F)) {
      return;
    } else {
      fail(union(F, KW_ELSE));
      synchronize(F);
    }
  }

  // ElseBody -> Block | IfStmt
  private void parseElseBody() {
    if (at(LBRACE)) {
      parseBlock();
    } else if (at(KW_IF)) {
      parseIfStmt();
    } else {
      fail(EnumSet.of(LBRACE, KW_IF));
      synchronize(F);
    }
  }

  // ReturnVal -> Expr | ε
  private void parseReturnVal() {
    if (inSet(E)) {
      parseExpr();
    } else if (at(SEMICOLON)) {
      return;
    } else {
      fail(union(E, SEMICOLON));
      synchronize(EnumSet.of(SEMICOLON));
    }
  }

  // Expr -> AndExpr OrTail
  private void parseExpr() {
    parseAndExpr();
    parseOrTail();
  }

  // OrTail -> || AndExpr OrTail | ε
  private void parseOrTail() {
    if (at(OR)) {
      match(OR);
      parseAndExpr();
      parseOrTail();
    } else if (inSet(X)) {
      return;
    } else {
      fail(union(X, OR));
      synchronize(X);
    }
  }

  // AndExpr -> EqExpr AndTail
  private void parseAndExpr() {
    parseEqExpr();
    parseAndTail();
  }

  // AndTail -> && EqExpr AndTail | ε
  private void parseAndTail() {
    if (at(AND)) {
      match(AND);
      parseEqExpr();
      parseAndTail();
    } else if (inSet(L1)) {
      return;
    } else {
      fail(union(L1, AND));
      synchronize(L1);
    }
  }

  // EqExpr -> RelExpr EqTail
  private void parseEqExpr() {
    parseRelExpr();
    parseEqTail();
  }

  // EqTail -> == RelExpr EqTail | != RelExpr EqTail | ε
  private void parseEqTail() {
    if (at(EQ) || at(NE)) {
      advance();
      parseRelExpr();
      parseEqTail();
    } else if (inSet(L2)) {
      return;
    } else {
      fail(union(L2, EQ, NE));
      synchronize(L2);
    }
  }

  // RelExpr -> AddExpr RelTail
  private void parseRelExpr() {
    parseAddExpr();
    parseRelTail();
  }

  // RelTail -> < AddExpr RelTail | <= AddExpr RelTail
  // | > AddExpr RelTail | >= AddExpr RelTail | ε
  private void parseRelTail() {
    if (at(LT) || at(LE) || at(GT) || at(GE)) {
      advance();
      parseAddExpr();
      parseRelTail();
    } else if (inSet(L3)) {
      return;
    } else {
      fail(union(L3, LT, LE, GT, GE));
      synchronize(L3);
    }
  }

  // AddExpr -> MulExpr AddTail
  private void parseAddExpr() {
    parseMulExpr();
    parseAddTail();
  }

  // AddTail -> + MulExpr AddTail | - MulExpr AddTail | ε
  private void parseAddTail() {
    if (at(PLUS) || at(MINUS)) {
      advance();
      parseMulExpr();
      parseAddTail();
    } else if (inSet(L4)) {
      return;
    } else {
      fail(union(L4, PLUS, MINUS));
      synchronize(L4);
    }
  }

  // MulExpr -> Unary MulTail
  private void parseMulExpr() {
    parseUnary();
    parseMulTail();
  }

  // MulTail -> * Unary MulTail | / Unary MulTail | ε
  private void parseMulTail() {
    if (at(STAR) || at(SLASH)) {
      advance();
      parseUnary();
      parseMulTail();
    } else if (inSet(L5)) {
      return;
    } else {
      fail(union(L5, STAR, SLASH));
      synchronize(L5);
    }
  }

  // Unary -> ! Unary | - Unary | Primary
  private void parseUnary() {
    if (at(NOT) || at(MINUS)) {
      advance();
      parseUnary();
    } else if (inSet(FIRST_PRIMARY)) {
      parsePrimary();
    } else {
      fail(E);
      synchronize(L6);
    }
  }

  // Primary -> NUM | STR_LIT | CHAR_LIT | True | False | ( Expr ) | ID PrimaryRest
  private void parsePrimary() {
    switch (token.getType()) {
      case LIT_INT, LIT_DOUBLE, LIT_STRING, LIT_CHAR, KW_TRUE, KW_FALSE -> advance();
      case LPAREN -> {
        match(LPAREN);
        parseExpr();
        match(RPAREN);
      }
      case IDENTIFIER -> {
        match(IDENTIFIER);
        parsePrimaryRest();
      }
      default -> {
        fail(FIRST_PRIMARY);
        synchronize(L6);
      }
    }
  }

  // PrimaryRest -> ( Args ) | ε
  private void parsePrimaryRest() {
    if (at(LPAREN)) {
      match(LPAREN);
      parseArgs();
      match(RPAREN);
    } else if (inSet(L6)) {
      return;
    } else {
      fail(union(L6, LPAREN));
      synchronize(L6);
    }
  }

  // Args -> Expr ArgTail | ε
  private void parseArgs() {
    if (inSet(E)) {
      parseExpr();
      parseArgTail();
    } else if (at(RPAREN)) {
      return;
    } else {
      fail(union(E, RPAREN));
      synchronize(EnumSet.of(RPAREN, COMMA));
    }
  }

  // ArgTail -> , Expr ArgTail | ε
  private void parseArgTail() {
    if (at(COMMA)) {
      match(COMMA);
      parseExpr();
      parseArgTail();
    } else if (at(RPAREN)) {
      return;
    } else {
      fail(EnumSet.of(COMMA, RPAREN));
      synchronize(EnumSet.of(RPAREN));
    }
  }
  public boolean parseComplete() {
    boolean syntaxValid = parse();
    return syntaxValid && lexer.getErrors().isEmpty();
  }

  
}