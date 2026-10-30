package.language.visitor;

import language.ast.*;

public class AstPrinter implements Visitor<Void> {
  private final StringBuilder out = new StringBuilder();
  private int depth = 0;

  public String getOutput() {
    return out.toString();
  }

  // Write a line with identation (2 tab-spaces per level)
  private void line(String text) {
    out.append("  ".repeat(depth)).append(text).append('\n');
  }
  
  @Override
  public Void visitProgram(Program node) {
    line("Program");
    depth++;
    for (Node decl : node.getDeclarations()) {
      decl.accept(this);
    }
    depth--;
    return null;
  }

  @Override
  public Void visitVarDecl(VarDecl node) {
    line("VarDecl(" + node.getType().symbol + " " + node.getName() + ")");
    if(node.hasInit()) {
      depth++;
      node.getInit().accept(this);
      depth--;
    }
    return null;
  }

  @Override
  public Void visitFuncDecl(FuncDecl node) {
    line("FuncDecl(" + node.getReturnType().symbol + " " + node.getName() + ")");
    depth++;
    for (Param params : node.getParams()) {
      params.accept(this);
    }
    node.getBody().accept(this);
    depth--;
    return null;
  }

  @Override
  public Void visitParam(Param node) {
    line("Param(" + node.getType().symbol + " " + node.getName() + ")");
    return null;
  }

  @Override
  public Void visitBlock(Block node) {
    line("Block");
    depth++;
    for (Stmt stmt : node.getStmts()) {
      stmt.accept(this);
    }
    depth--;
    return null;
  }

  @Override
  public Void visitAssign(Assign node) {
    line("Assign");
    depth++;
    node.getTarget().accept(this);
    node.getValue().accept(this);
    depth--;
    return null;
  }

  @Override
  public Void visitCallStmt(CallStmt node) {
    line("CallStmt");
    depth++;
    node.getCall().accept(this);
    depth--;
    return null;
  }

  @Override
  public Void visitIf(If node) {
    line("If");
    depth++;
    node.getCond().accept(this);
    node.getThenBranch().accept(this);
    if (node.hasElse()) {
      node.getElseBranch().accept(this);
    }
    depth--;
    return null;
  }

  @Override
  public Void visitWhile(While node) {
    line("While");
    depth++;
    node.getCond().accept(this);
    node.getBody().accept(this);
    depth--;
    return null;
  }

  @Override
  public Void visitReturn(Return node) {
    line("Return");
    if (node.hasValue()) {
      depth++;
      node.getValue().accept(this);
      depth--;
    }
    return null;
  }

  @Override
  public Void visitIntLit(IntLit node) {
    line("IntLit(" + node.getLexeme() + ")");
    return null;
  }
  
  @Override 
  public Void visitDoubleLit(DoubleLit node) {
    line("DoubleLit(" + node.getLexeme() + ")");
    return null;
  }

  @Override
  public Void visitBoolLit(BoolLit node) {
    line("BoolLit(" + (node.isValue() ? "True" : "False") + ")");
    return null;
  }

  @Override
  public Void visitCharLit(CharLit node) {
    line("CharLit(" + node.getLexeme() + ")");
    return null;
  }

  @Override
  public Void visitStringLit(StringLit node) {
    line("StringLit(" + node.getLexeme() + ")");
    return null;
  }

  @Override
  public Void visitId(Id node) {
    line("Id(" + node.getName() + ")");
    return null;
  }
  
  @Override
  public Void visitBin(Bin node) {
    line("Bin(" + node.getOperator().symbol + ")");
    depth++;
    node.getLeftOperand().accept(this);
    node.getRightOperand().accept(this);
    depth--;
    return null;
  }
  
  @Override
  public Void visitUnary(Unary node) {
    line("Unary(" + node.getOperator().symbol + ")");
    depth++;
    node.getOperand().accept(this);
    depth--;
    return null;
  }

  @Override
  public Void visitCall(Call node) {
    line("Call");
    depth++;
    node.getCallee().accept(this);
    for (Expr arg : node.getArgs()) {
      arg.accept(this);
    }
    depth--;
    return null;
  }
}
