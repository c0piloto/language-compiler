package language.visitor;

import java.util.ArrayList;
import java.util.List;

import language.ast.*;
import language.symbols.Category;
import language.symbols.StackSymbolTable;
import language.symbols.Symbol;
import language.symbols.SymbolTable;

  /*
  * Identification pass: open/closes scopes, declares names and links each use Id to it's declaration
  *
  * Error format (shows only one per line, in the text order):
  *   line L: 'name' already declared in this scope (line L0)
  *   line L: 'name' non-declared
  *
  */

public class IdentificationPass implements Visitor<Void> {
  private final SymbolTable table = new StackSymbolTable();
  private final List<String> errors = new ArrayList<>();
  private final List<String> uses = new ArrayList<>();

  public List<String> getUses() {
    return this.uses;
  }

  public List<String> getErrors() {
    return this.errors;
  }

  // ---------- declaration (used by variables, parameters and function) ----------
  private void declare(String name, Category category, Node node) {
    Symbol symbol = new Symbol(name, category, node, this.table.level());
    if(!this.table.declare(symbol)) {
      Symbol first = this.table.lookupLocal(name);
      this.errors.add("linha " + node.getLine() + ": '" + name + "' já declarado neste escopo (linha " + first.getDeclaration().getLine() + ")");
    }
  }

  // ---------- structure ----------

  @Override
  public Void visitProgram(Program node) {
    for(Node decl: node.getDeclarations()) {
    decl.accept(this);
    }
    return null;
  }

  @Override
  public Void visitVarDecl(VarDecl node) {
    if (node.hasInit()) {
      node.getInit().accept(this);
    }
    declare(node.getName(), Category.VARIABLE, node);
    return null;
  }
  
  @Override
  public Void visitFuncDecl(FuncDecl node) {
    Category category = node.getReturnType() == TypeKind.VOID ? Category.PROCEDURE : Category.FUNCTION;
    declare(node.getName(), category, node); // Extern scope, before openScope() (recursive)
    
    this.table.openScope();
    for(Param param : node.getParams()) {
      param.accept(this);
    }
    // Body in the SAME scope of the params: runs the commands without going through visitBlock
    for(Stmt stmt : node.getBody().getStmts()) {
      stmt.accept(this);
    }
    this.table.closeScope();
    return null;
  }

  @Override
  public Void visitParam(Param node) {
    declare(node.getName(), Category.PARAMETER, node);
    return null;
  }

  @Override
  public Void visitBlock(Block node) {
    this.table.openScope();
    for(Stmt stmt : node.getStmts()) {
      stmt.accept(this);
    }
    this.table.closeScope();
    return null;
  }

  // ---------- use ----------

  @Override
  public Void visitId(Id node) {
    Symbol symbol = this.table.lookup(node.getName());
    if(symbol != null) {
      node.setDeclaration(symbol);
      this.uses.add("linha " + node.getLine() + ": " + node.getName() + "@" + symbol.getDeclaration().getLine());
    } else {
      this.errors.add("linha " + node.getLine() + ": '" + node.getName() + "' não declarado");
    }
    return null;
  }

  // ---------- nodes that only goes down ----------

  @Override
  public Void visitAssign(Assign node) {
    node.getTarget().accept(this);
    node.getValue().accept(this);
    return null;
  }

  @Override
  public Void visitCallStmt(CallStmt node) {
    node.getCall().accept(this);
    return null;
  }

  @Override
  public Void visitCall(Call node) {
    node.getCallee().accept(this);
    for(Expr arg : node.getArgs()) {
      arg.accept(this);
    }
    return null;
  }

  @Override
  public Void visitIf(If node) {
    node.getCond().accept(this);
    node.getThenBranch().accept(this);
    if(node.hasElse()) {
      node.getElseBranch().accept(this);
    }
    return null;
  }

  @Override
  public Void visitWhile(While node) {
    node.getCond().accept(this);
    node.getBody().accept(this);
    return null;
  }

  @Override
  public Void visitReturn(Return node) {
    if(node.hasValue()) {
      node.getValue().accept(this);
    }
    return null;
  }

  @Override
  public Void visitBin(Bin node) {
    node.getLeftOperand().accept(this);
    node.getRightOperand().accept(this);
    return null;
  }

  @Override
  public Void visitUnary(Unary node) {
    node.getOperand().accept(this);
    return null;
  }

  // ---------- literals ----------

  @Override
  public Void visitIntLit(IntLit node) {
    return null;
  }

  @Override
  public Void visitDoubleLit(DoubleLit node) {
    return null;
  }

  @Override
  public Void visitBoolLit(BoolLit node) {
    return null;
  }

  @Override
  public Void visitCharLit(CharLit node) {
    return null;
  }

  @Override
  public Void visitStringLit(StringLit node) {
    return null;
  }
}
