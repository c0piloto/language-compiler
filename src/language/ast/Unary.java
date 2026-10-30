package language.ast;

import language.visitor.Visitor;

public final class Unary extends Expr {
  private final Op operator;        // Op.NOT ou Op.NEG
  private final Expr operand;

  public Unary(Op op, Expr operand, int line, int col) {
    super(line, col);
    this.operator = op;
    this.operand = operand;
  }

  public Op getOperator() {
    return this.operator;
  }

  public Expr getOperand() {
    return this.operand;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitUnary(this);
  }
}
