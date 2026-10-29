package language.ast;

import language.visitor.Visitor;

public final class Assign extends Stmt {
  private Id target;
  private Expr value;

  public Assign(Id target, Expr value, int line, int col) {
    super(line, col);
    this.target = target;
    this.value = value;
  }

  public Id getTarget() {
	return target;
  }

  public Expr getValue() {
	return value;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitAssign(this);
  }
}
