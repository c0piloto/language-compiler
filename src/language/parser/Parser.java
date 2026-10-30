package language.parser;

import language.ast.*;
import language.lexer.*;

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

  // Devolve a árvore (ou null, se houve qualquer erro léxico ou sintático)
  // junto com as listas de erros.
  public ParseResult parse() {
    errors.clear();
    Program tree = parseProgram();
    if (!at(EOF)) {
      fail(EnumSet.of(EOF));
      synchronize(EnumSet.of(EOF));
    }
    List<LexicalError> lexicalErrors = new ArrayList<>(lexer.getErrors());
    boolean valid = errors.isEmpty() && lexicalErrors.isEmpty();
    return new ParseResult(valid ? tree : null, new ArrayList<>(errors), lexicalErrors);
  }

  // Conversões token -> enum da AST (switch sobre TOKEN, não sobre nó)

  private static TypeKind typeOf(TokenType token) {
    return switch (token) {
      case KW_INT -> TypeKind.INT;
      case KW_DOUBLE -> TypeKind.DOUBLE;
      case KW_BOOL -> TypeKind.BOOL;
      case KW_CHAR -> TypeKind.CHAR;
      case KW_STRING -> TypeKind.STRING;
      case KW_VOID -> TypeKind.VOID;
      default -> null;
    };
  }

  private static Op opOf(TokenType token) {
    return switch (token) {
      case OR -> Op.OR;
      case AND -> Op.AND;
      case EQ -> Op.EQ;
      case NE -> Op.NE;
      case LT -> Op.LT;
      case LE -> Op.LE;
      case GT -> Op.GT;
      case GE -> Op.GE;
      case PLUS -> Op.ADD;
      case MINUS -> Op.SUB;
      case STAR -> Op.MUL;
      case SLASH -> Op.DIV;
      default -> null;
    };
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
  private Program parseProgram() {
    Token start = token;
    List<Node> decls = new ArrayList<>();
    parseDecls(decls);
    return new Program(decls, start.getLine(), start.getCol());
  }

  // A recursão Decl Program do CP2, preenchendo a lista
  private Node parseDecls(List<Node> decls) {
    if (inSet(T)) {
      decls.add(parseDecl());
      parseDecls(decls);
    } else if (at(EOF)) {
      return;
    } else {
      fail(union(T, EOF));
      synchronize(union(T, EOF));
      if (!at(EOF)) {
        parseDecls(decls);
      }
    }
  }

  // Decl -> Type ID DeclRest
  private Node parseDecl() {
    Token start = token;
    TypeKind type = parseType();
    Token name = match(IDENTIFIER);
    return parseDeclRest(type, name.getLexema(), start);
  }

  // DeclRest -> ( Params ) Block | VarRest ;
  private Node parseDeclRest(TypeKind type, String name, Token start) {
    if (at(LPAREN)) {
      match(LPAREN);
      List<Param> params = parseParams();
      match(RPAREN);
      Block body = parseBlock();
      return new FuncDecl(type, name, params, body, start.getLine(), start.getCol());
    } else if (at(ASSIGN) || at(SEMICOLON)) {
      Expr init = parseVarRest();
      match(SEMICOLON);
      return new VarDecl(type, name, init, start.getLine(), start.getCol());
    } else {
      fail(EnumSet.of(LPAREN, ASSIGN, SEMICOLON));
      synchronize(union(T, EnumSet.of(SEMICOLON, RBRACE)));
      return null;
    }
  }

  // VarRest -> = Expr | ε   (null = sem inicializador)
  private Expr parseVarRest() {
    if (at(ASSIGN)) {
      match(ASSIGN);
      return parseExpr();
    } else if (at(SEMICOLON)) {
      return null;
    } else {
      fail(EnumSet.of(ASSIGN, SEMICOLON));
      synchronize(EnumSet.of(SEMICOLON));
      return null;
    }
  }

  // Params -> Param ParamTail | ε
  private List<Param> parseParams() {
    List<Param> params = new ArrayList<>();
    if (inSet(T)) {
      params.add(parseParam());
      parseParamTail(params);
    } else if (at(RPAREN)) {
      // ε: lista vazia
    } else {
      fail(union(T, RPAREN));
      synchronize(EnumSet.of(RPAREN));
    }
    return params;
  }

  // ParamTail -> , Param ParamTail | ε
  private void parseParamTail(List<Param> params) {
    if (at(COMMA)) {
      match(COMMA);
      params.add(parseParam());
      parseParamTail(params);
    } else if (at(RPAREN)) {
      return;
    } else {
      fail(EnumSet.of(COMMA, RPAREN));
      synchronize(EnumSet.of(RPAREN));
    }
  }

  // Param -> Type ID
  private Param parseParam() {
    Token start = token;
    TypeKind type = parseType();
    Token name = match(IDENTIFIER);
    return new Param(type, name.getLexema(), start.getLine(), start.getCol());
  }

  // Type -> int | double | bool | char | string | void
  private TypeKind parseType() {
    if (inSet(T)) {
      TypeKind kind = typeOf(token.getType()); // lê antes de avançar
      advance();
      return kind;
    } else {
      fail(T);
      synchronize(union(T, IDENTIFIER, SEMICOLON));
      return null;
    }
  }

  // Block -> { StmtList }
  private Block parseBlock() {
    Token start = token;
    match(LBRACE);
    List<Stmt> stmts = new ArrayList<>();
    parseStmtList(stmts);
    match(RBRACE);
    return new Block(stmts, start.getLine(), start.getCol());
  }

  // StmtList -> Stmt StmtList | ε
  private void parseStmtList(List<Stmt> stmts) {
    if (inSet(S)) {
      stmts.add(parseStmt());
      parseStmtList(stmts);
    } else if (at(RBRACE)) {
      return;
    } else {
      fail(union(S, RBRACE));
      synchronize(F);
      if (!at(RBRACE) && inSet(S)) {
        parseStmtList(stmts);
      }
    }
  }

  // Stmt -> Type ID VarRest ; | ID IdStmt | IfStmt | Block
  // | while ( Expr ) Block | return ReturnVal ;
  private Stmt parseStmt() {
    Token start = token;

    if (inSet(T)) {
      TypeKind type = parseType();
      Token name = match(IDENTIFIER);
      Expr init = parseVarRest();
      match(SEMICOLON);
      return new VarDecl(type, name.getLexema(), init, start.getLine(), start.getCol());
    }

    switch (token.getType()) {
      case IDENTIFIER -> {
        Token id = match(IDENTIFIER);
        return parseIdStmt(id);
      }
      case KW_IF -> {
        return parseIfStmt();
      }
      case LBRACE -> {
        return parseBlock();
      }
      case KW_WHILE -> {
        match(KW_WHILE);
        match(LPAREN);
        Expr cond = parseExpr();
        match(RPAREN);
        Block body = parseBlock();
        return new While(cond, body, start.getLine(), start.getCol());
      }
      case KW_RETURN -> {
        match(KW_RETURN);
        Expr value = parseReturnVal();
        match(SEMICOLON);
        return new Return(value, start.getLine(), start.getCol());
      }
      default -> {
        fail(S);
        synchronize(F);
        return null;
      }
    }
  }

  // IdStmt -> = Expr ; | ( Args ) ;   (o ID já foi consumido por parseStmt)
  private Stmt parseIdStmt(Token id) {
    Id target = new Id(id.getLexema(), id.getLine(), id.getCol());

    switch (token.getType()) {
      case ASSIGN -> {
        match(ASSIGN);
        Expr value = parseExpr();
        match(SEMICOLON);
        return new Assign(target, value, id.getLine(), id.getCol());
      }
      case LPAREN -> {
        match(LPAREN);
        List<Expr> args = parseArgs();
        match(RPAREN);
        match(SEMICOLON);
        Call call = new Call(target, args, id.getLine(), id.getCol());
        return new CallStmt(call, id.getLine(), id.getCol());
      }
      default -> {
        fail(EnumSet.of(ASSIGN, LPAREN));
        synchronize(EnumSet.of(SEMICOLON));
        return null;
      }
    }
  }

  // IfStmt -> if ( Expr ) Block ElsePart
  private If parseIfStmt() {
    Token start = token;
    match(KW_IF);
    match(LPAREN);
    Expr cond = parseExpr();
    match(RPAREN);
    Block thenBranch = parseBlock();
    Stmt elseBranch = parseElsePart();
    return new If(cond, thenBranch, elseBranch, start.getLine(), start.getCol());
  }

  // ElsePart -> else ElseBody | ε   (null = sem else)
  private Stmt parseElsePart() {
    if (at(KW_ELSE)) {
      match(KW_ELSE);
      return parseElseBody();
    } else if (inSet(F)) {
      return null;
    } else {
      fail(union(F, KW_ELSE));
      synchronize(F);
      return null;
    }
  }

  // ElseBody -> Block | IfStmt
  private Stmt parseElseBody() {
    if (at(LBRACE)) {
      return parseBlock();
    } else if (at(KW_IF)) {
      return parseIfStmt();
    } else {
      fail(EnumSet.of(LBRACE, KW_IF));
      synchronize(F);
      return null;
    }
  }

  // ReturnVal -> Expr | ε   (null = return sem valor)
  private Expr parseReturnVal() {
    if (inSet(E)) {
      return parseExpr();
    } else if (at(SEMICOLON)) {
      return null;
    } else {
      fail(union(E, SEMICOLON));
      synchronize(EnumSet.of(SEMICOLON));
      return null;
    }
  }

  // Expr -> AndExpr OrTail
  private Expr parseExpr() {
    return parseOrTail(parseAndExpr());
  }

  // OrTail -> || AndExpr OrTail | ε
  private Expr parseOrTail(Expr left) {
    if (at(OR)) {
      advance();
      Expr right = parseAndExpr();
      return parseOrTail(new Bin(Op.OR, left, right));
    } else if (inSet(X)) {
      return left;
    } else {
      fail(union(X, OR));
      synchronize(X);
      return left;
    }
  }

  // AndExpr -> EqExpr AndTail
  private Expr parseAndExpr() {
    return parseAndTail(parseEqExpr());
  }

  // AndTail -> && EqExpr AndTail | ε
  private Expr parseAndTail(Expr left) {
    if (at(AND)) {
      advance();
      Expr right = parseEqExpr();
      return parseAndTail(new Bin(Op.AND, left, right));
    } else if (inSet(L1)) {
      return left;
    } else {
      fail(union(L1, AND));
      synchronize(L1);
      return left;
    }
  }

  // EqExpr -> RelExpr EqTail
  private Expr parseEqExpr() {
    return parseEqTail(parseRelExpr());
  }

  // EqTail -> == RelExpr EqTail | != RelExpr EqTail | ε
  private Expr parseEqTail(Expr left) {
    if (at(EQ) || at(NE)) {
      Token op = token;
      advance();
      Expr right = parseRelExpr();
      return parseEqTail(new Bin(opOf(op.getType()), left, right));
    } else if (inSet(L2)) {
      return left;
    } else {
      fail(union(L2, EQ, NE));
      synchronize(L2);
      return left;
    }
  }

  // RelExpr -> AddExpr RelTail
  private Expr parseRelExpr() {
    return parseRelTail(parseAddExpr());
  }

  // RelTail -> < AddExpr RelTail | <= AddExpr RelTail
  // | > AddExpr RelTail | >= AddExpr RelTail | ε
  private Expr parseRelTail(Expr left) {
    if (at(LT) || at(LE) || at(GT) || at(GE)) {
      Token op = token;
      advance();
      Expr right = parseAddExpr();
      return parseRelTail(new Bin(opOf(op.getType()), left, right));
    } else if (inSet(L3)) {
      return left;
    } else {
      fail(union(L3, LT, LE, GT, GE));
      synchronize(L3);
      return left;
    }
  }

  // AddExpr -> MulExpr AddTail
  private Expr parseAddExpr() {
    return parseAddTail(parseMulExpr());
  }

  // AddTail -> + MulExpr AddTail | - MulExpr AddTail | ε
  private Expr parseAddTail(Expr left) {
    if (at(PLUS) || at(MINUS)) {
      Token op = token;
      advance();
      Expr right = parseMulExpr();
      return parseAddTail(new Bin(opOf(op.getType()), left, right));
    } else if (inSet(L4)) {
      return left;
    } else {
      fail(union(L4, PLUS, MINUS));
      synchronize(L4);
      return left;
    }
  }

  // MulExpr -> Unary MulTail
  private Expr parseMulExpr() {
    return parseMulTail(parseUnary());
  }

  // MulTail -> * Unary MulTail | / Unary MulTail | ε
  private Expr parseMulTail(Expr left) {
    if (at(STAR) || at(SLASH)) {
      Token op = token;
      advance();
      Expr right = parseUnary();
      return parseMulTail(new Bin(opOf(op.getType()), left, right));
    } else if (inSet(L5)) {
      return left;
    } else {
      fail(union(L5, STAR, SLASH));
      synchronize(L5);
      return left;
    }
  }

  // Unary -> ! Unary | - Unary | Primary
  private Expr parseUnary() {
    if (at(NOT) || at(MINUS)) {
      Token op = token;
      advance();
      Expr operand = parseUnary();
      Op kind = (op.getType() == NOT) ? Op.NOT : Op.NEG;
      return new Unary(kind, operand, op.getLine(), op.getCol());
    } else if (inSet(FIRST_PRIMARY)) {
      return parsePrimary();
    } else {
      fail(E);
      synchronize(L6);
      return null;
    }
  }

  // Primary -> NUM | STR_LIT | CHAR_LIT | True | False | ( Expr ) | ID PrimaryRest
  private Expr parsePrimary() {
    Token t = token;

    switch (t.getType()) {
      case LIT_INT -> {
        advance();
        return new IntLit(t.getLexema(), t.getLine(), t.getCol());
      }
      case LIT_DOUBLE -> {
        advance();
        return new DoubleLit(t.getLexema(), t.getLine(), t.getCol());
      }
      case LIT_STRING -> {
        advance();
        return new StringLit(t.getLexema(), t.getLine(), t.getCol());
      }
      case LIT_CHAR -> {
        advance();
        return new CharLit(t.getLexema(), t.getLine(), t.getCol());
      }
      case KW_TRUE -> {
        advance();
        return new BoolLit(true, t.getLine(), t.getCol());
      }
      case KW_FALSE -> {
        advance();
        return new BoolLit(false, t.getLine(), t.getCol());
      }
      case LPAREN -> {
        match(LPAREN);
        Expr inner = parseExpr();
        match(RPAREN);
        return inner; // parênteses não viram nó
      }
      case IDENTIFIER -> {
        Token id = match(IDENTIFIER);
        return parsePrimaryRest(id);
      }
      default -> {
        fail(FIRST_PRIMARY);
        synchronize(L6);
        return null;
      }
    }
  }

  // PrimaryRest -> ( Args ) | ε   (o ID já foi consumido por parsePrimary)
  private Expr parsePrimaryRest(Token id) {
    Id name = new Id(id.getLexema(), id.getLine(), id.getCol());

    if (at(LPAREN)) {
      match(LPAREN);
      List<Expr> args = parseArgs();
      match(RPAREN);
      return new Call(name, args, id.getLine(), id.getCol());
    } else if (inSet(L6)) {
      return name;
    } else {
      fail(union(L6, LPAREN));
      synchronize(L6);
      return name;
    }
  }

  // Args -> Expr ArgTail | ε
  private List<Expr> parseArgs() {
    List<Expr> args = new ArrayList<>();
    if (inSet(E)) {
      args.add(parseExpr());
      parseArgTail(args);
    } else if (at(RPAREN)) {
      // ε: lista vazia
    } else {
      fail(union(E, RPAREN));
      synchronize(EnumSet.of(RPAREN, COMMA));
    }
    return args;
  }

  // ArgTail -> , Expr ArgTail | ε
  private void parseArgTail(List<Expr> args) {
    if (at(COMMA)) {
      match(COMMA);
      args.add(parseExpr());
      parseArgTail(args);
    } else if (at(RPAREN)) {
      return;
    } else {
      fail(EnumSet.of(COMMA, RPAREN));
      synchronize(EnumSet.of(RPAREN));
    }
  }
}
