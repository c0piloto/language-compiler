package language.visitor;

import language.ast.*;

public interface Visitor<T> {
  T visitProgram(Program n); // Structure
  T visitVarDecl(VarDecl n); // Structure
  T visitFuncDecl(FuncDecl n); // Structure
  T visitParam(Param n); // Structure
  T visitBlock(Block n); // Structure
  T visitAssign(Assign n); // Goes down only
  T visitCallStmt(CallStmt n); // Goes down only
  T visitIf(If n); // Goes down only
  T visitWhile(While n); // Goes down only
  T visitReturn(Return n); // Goes down only
  T visitIntLit(IntLit n); // Literal
  T visitDoubleLit(DoubleLit n); // Literal
  T visitBoolLit(BoolLit n); // Literal
  T visitCharLit(CharLit n); // Literal
  T visitStringLit(StringLit n); // Literal
  T visitId(Id n); // Use
  T visitBin(Bin n); // Goes down only
  T visitUnary(Unary n); // Goes down only
  T visitCall(Call n); // Goes down only
}
