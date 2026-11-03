package language.ast;

import language.visitor.Visitor;

public final class Bin extends Expr {
  private Op operator;
  private Expr leftOperand, rightOperand;

  // Posição = primeiro token do operando esquerdo (ast-design.md, seção 4).
  // Se o operando for null (só após um erro sintático, quando a árvore é descartada), usa 0:0.
  public Bin(Op operator, Expr leftOperand, Expr rightOperand) {
    super(leftOperand != null ? leftOperand.getLine() : 0,
        leftOperand != null ? leftOperand.getCol() : 0);
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
