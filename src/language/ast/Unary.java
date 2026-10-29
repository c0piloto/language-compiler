package language.ast;

import language.visitor.Visitor;

public final class Unary extends Expr {
  private final Op op;        // Op.NOT ou Op.NEG
  private final Expr operand;

  public Unary(Op op, Expr operand, int line, int col) {
    super(line, col);
    this.op = op;
    this.operand = operand;
  }

  public Op getOp() {
    return this.op;
  }

  public Expr getOperand() {
    return this.operand;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitUnary(this);
  }
}
