package language.ast;

import language.visitor.Visitor;

public final class Bin extends Expr {
  private Op operator;
  private Expr leftOperand, rightOperand;

  public Bin(Op operator, Expr leftOperand, Expr rightOperand, int line, int col) {
    super(line, col);
    this.operator = operator;
    this.leftOperand = leftOperand;
    this.rightOperand = rightOperand;
  }

  public Op getOperator() {
	return operator;
  }

  public Expr getLeftOperand() {
	return leftOperand;
  }

  public Expr getRightOperand() {
	return rightOperand;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitBin(this);
  }
}
