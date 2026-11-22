package language.visitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import language.ast.*;
import language.symbols.Category;
import language.symbols.Symbol;
import language.symbols.Type;

/**
 * Messages (One per line, in order of visit):
 *  line L: 'if' condition must be bool, is int instead
 *  line L: operator '+' don't apply to bool and int
 *  line L: 'f' expects one argument, recieved 2
 *  line L: argument 1 from 'f'"expected string, it is int instead
 * Warnings: line L: warning: 'x' can be use without value
 */
public class TypeChecker implements Visitor<Type> {
  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  private FuncDecl currentFunc = null;
  private Set<Node> unassigned = new HashSet<>();

  public List<String> getErrors() {
    return this.errors;
  }

  public List<String> getWarnings() {
    return this.warnings;
  }

  // Aux methods
  
  private void error(int line, String message) {
    this.errors.add("linha " + line + ": " + message);
  }

  private Type typeOf(Expr expr) {
    Type type = expr.accept(this);
    return type == null ? Type.ERROR : type;
  }

  private Type done(Expr expr, Type type) {
    expr.setType(type);
    return type;
  }

  private Type declared(Symbol symbol) {
    Type type = symbol.getType();
    return type == null ? Type.ERROR : type;
  }

  private boolean isFunction(Symbol symbol) {
    return symbol.getCategory() == Category.FUNCTION || symbol.getCategory() == Category.PROCEDURE;
  }

  // Structure

  @Override
  public Type visitProgram(Program node) {
    for(Node decl : node.getDeclarations()) {
      decl.accept(this);
    }
    return Type.VOID;
  }

  // [VAR-INIT]: the initializer is checked before declaring the name
  @Override
  public Type visitVarDecl(VarDecl node) {
    Type target = Type.of(node.getType());
    if(node.hasInit()) {
      Type type = typeOf(node.getInit());
      if(!type.isAssignableTo(target)){
        error(node.getLine(), "inicializador de '" + node.getName() + "': esperado " + target + " veio " + type);
      }
    } else if(this.currentFunc != null) {
      this.unassigned.add(node);
    }
    return Type.VOID;
  }

  @Override
  public Type visitFuncDecl(FuncDecl n) {
    FuncDecl savedFunc = this.currentFunc;
    Set<Node> savedUnassigned = this.unassigned;
    this.currentFunc = n;
    this.unassigned = new HashSet<>();
    // Same parameters scope: goes through body directly
    for (Stmt s : n.getBody().getStmts()) {
      s.accept(this);
    }
    this.currentFunc = savedFunc;
    this.unassigned = savedUnassigned;
    return Type.VOID;
  }

  @Override
  public Type visitParam(Param n) {
    return Type.VOID; // Type is already on symbol, nothing to check
  }

  // [BLOCK]
  @Override
  public Type visitBlock(Block n) {
    for (Stmt s : n.getStmts()) {
      s.accept(this);
    }
    return Type.VOID;
  }

  // Commands

  // [ASSIGN]
  @Override
  public Type visitAssign(Assign n) {
    Id target = n.getTarget();
    Symbol s = target.getDeclaration();
    Type expected = Type.ERROR;
    if (s != null) {
      if (isFunction(s)) {
        error(n.getLine(), "não é possível atribuir a '" + target.getName() + "': é função");
      } else {
        expected = declared(s);
      }
    }
    target.setType(expected);

    Type value = typeOf(n.getValue());
    if (!value.isAssignableTo(expected)) {
      error(n.getLine(), "atribuição a '" + target.getName() + "': esperado " + expected + ", veio " + value);
    }
    if (s != null) {
      this.unassigned.remove(s.getDeclaration()); // Now the variable has value
    }
    return Type.VOID;
  }

  // [CALL-STMT] Decision 6: As command the value is discarded
  @Override
  public Type visitCallStmt(CallStmt n) {
    Call call = n.getCall();
    call.setType(checkCall(call));
    return Type.VOID;
  }

  // [IF] Decision 3: Only bool is condition
  @Override
  public Type visitIf(If n) {
    Type c = typeOf(n.getCond());
    if (!c.isError() && c != Type.BOOL) {
      error(n.getLine(), "condição de 'if' deve ser bool, mas é " + c);
    }
    Set<Node> before = new HashSet<>(this.unassigned);
    n.getThenBranch().accept(this);
    Set<Node> afterThen = this.unassigned;
    this.unassigned = new HashSet<>(before);
    if (n.hasElse()) {
      n.getElseBranch().accept(this);
    }
    this.unassigned.addAll(afterThen); // No value if any path left it without value
    return Type.VOID;
  }

  // [WHILE] Decision 3
  @Override
  public Type visitWhile(While n) {
    Type c = typeOf(n.getCond());
    if (!c.isError() && c != Type.BOOL) {
      error(n.getLine(), "condição de 'while' deve ser bool, mas é " + c);
    }
    Set<Node> before = new HashSet<>(this.unassigned);
    n.getBody().accept(this);
    this.unassigned = before; // Body can execute 0 times
    return Type.VOID;
  }

  // [RET-VAL] / [RET-VOID] Decision 9: Each return is checked, no flow analysis
  @Override
  public Type visitReturn(Return n) {
    if (this.currentFunc == null) {
      error(n.getLine(), "'return' fora de função");
      if (n.hasValue()) typeOf(n.getValue());
      return Type.VOID;
    }
    String name = this.currentFunc.getName();
    Type ret = Type.of(this.currentFunc.getReturnType());
    if (n.hasValue()) {
      Type t = typeOf(n.getValue());
      if (ret == Type.VOID) {
        error(n.getLine(), "'return' com valor em procedimento '" + name + "'");
      } else if (!t.isAssignableTo(ret)) {
        error(n.getLine(), "'return' em '" + name + "': esperado " + ret + ", veio " + t);
      }
    } else if (ret != Type.VOID) {
      error(n.getLine(), "'return' sem valor em função '" + name + "' que retorna " + ret);
    }
    return Type.VOID;
  }

  // Expressions

  // [LIT-INT] [LIT-DBL] [LIT-BOOL] [LIT-CHAR] [LIT-STR]
  @Override public Type visitIntLit(IntLit n) { return done(n, Type.INT); }
  @Override public Type visitDoubleLit(DoubleLit n) { return done(n, Type.DOUBLE); }
  @Override public Type visitBoolLit(BoolLit n) { return done(n, Type.BOOL); }
  @Override public Type visitCharLit(CharLit n) { return done(n, Type.CHAR); }
  @Override public Type visitStringLit(StringLit n) { return done(n, Type.STRING); }

  // [ID] / [ID-FUNC] / [ID-NULO]
  @Override
  public Type visitId(Id n) {
    Symbol s = n.getDeclaration();
    if (s == null) {
      return done(n, Type.ERROR);
    }
    if (isFunction(s)) { // [ID-FUNC] Decision 11: Function name without parentheses is and error
      error(n.getLine(), "'" + n.getName() + "' é função e não pode ser usada como valor (faltam parênteses)");
      return done(n, Type.ERROR);
    }
    if (this.unassigned.contains(s.getDeclaration())) {
      this.warnings.add("linha " + n.getLine() + ": aviso: '" + n.getName() + "' pode ser usado sem valor");
    }
    return done(n, declared(s)); // [ID]
  }

  @Override
  public Type visitBin(Bin n) {
    Type l = typeOf(n.getLeftOperand());
    Type r = typeOf(n.getRightOperand());
    if (l.isError() || r.isError()) {
      return done(n, Type.ERROR); // No cascade
    }
    Type result = binaryResult(n.getOperator(), l, r);
    if (result == null) {
      error(n.getLine(), "operador '" + n.getOperator().symbol + "' não se aplica a " + l + " e " + r);
      result = Type.ERROR;
    }
    return done(n, result);
  }

  public Type binaryResult(Op op, Type l, Type r) {
    switch (op) {
      case ADD: case SUB: case MUL: case DIV:
        if (l == Type.INT && r == Type.INT) return Type.INT;           // [ARIT-INT]
        if (l.isNumeric() && r.isNumeric()) return Type.DOUBLE;        // [ARIT-DBL] (Decisions 1, 2, 3, 4)
        return null;
      case LT: case LE: case GT: case GE:
        if (l.isNumeric() && r.isNumeric()) return Type.BOOL;          // [REL]
        return null;
      case EQ: case NE:
        if (l == r && (l == Type.INT || l == Type.DOUBLE || l == Type.BOOL || l == Type.CHAR)) {
          return Type.BOOL;                                            // [EQ-IGUAL] (Decisions 4 e 5)
        }
        if (l.isNumeric() && r.isNumeric()) return Type.BOOL;          // [EQ-NUM]
        return null;
      case AND: case OR:
        if (l == Type.BOOL && r == Type.BOOL) return Type.BOOL;        // [LOG]
        return null;
      default:
        return null;
    }
  }

  @Override
  public Type visitUnary(Unary n) {
    Type t = typeOf(n.getOperand());
    if (t.isError()) {
      return done(n, Type.ERROR);
    }
    Type result = unaryResult(n.getOperator(), t);
    if (result == null) {
      error(n.getLine(), "operador '" + n.getOperator().symbol + "' não se aplica a " + t);
      result = Type.ERROR;
    }
    return done(n, result);
  }

  public Type unaryResult(Op op, Type t) {
    if (op == Op.NOT) {
      if (t == Type.BOOL) return Type.BOOL;                          // [NOT]
    } else if (t.isNumeric()) {
      return t;                                                      // [NEG-INT] / [NEG-DBL]
    }
    return null;
  }

  // [CALL] in expression: procedure (void) cannot be used as value (Decision 6)
  @Override
  public Type visitCall(Call n) {
    Type t = checkCall(n);
    if (t == Type.VOID) {
      error(n.getLine(), "procedimento '" + n.getCallee().getName()
          + "' não retorna valor e não pode ser usado em expressão");
      t = Type.ERROR;
    }
    return done(n, t);
  }

  /**
   * [CALL]: checks that is a function, aridity and type of each argument; returns the type of return
   * (VOID for procedure). The name called does NOT goes through visitId (else [ID-FUNC] triggers).
   * Decision 8: argument type exact (no int -> double).
   */
  private Type checkCall(Call n) {
    Id callee = n.getCallee();
    Symbol s = callee.getDeclaration();
    List<Expr> args = n.getArgs();

    if (s == null) {
      for (Expr a : args) typeOf(a);
      return Type.ERROR;
    }
    if (!isFunction(s)) {
      error(n.getLine(), "'" + callee.getName() + "' não é função");
      for (Expr a : args) typeOf(a);
      return Type.ERROR;
    }

    List<Type> params = s.getParamTypes();
    if (params.size() != args.size()) {
      error(n.getLine(), "'" + callee.getName() + "' espera " + params.size()
          + " argumento(s), recebeu " + args.size());
    }
    for (int i = 0; i < args.size(); i++) {
      Type t = typeOf(args.get(i));
      if (i < params.size() && !t.isError() && t != params.get(i)) {
        error(args.get(i).getLine(), "argumento " + (i + 1) + " de '" + callee.getName()
            + "': esperado " + params.get(i) + ", veio " + t);
      }
    }
    return declared(s);
  }
}
