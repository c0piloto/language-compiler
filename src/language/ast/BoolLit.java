package language.ast;

import language.visitor.Visitor;

public final class BoolLit extends Expr {
  private boolean value;

  public BoolLit(boolean value, int line, int col) {
    super(line, col);
    this.value = value;
  }

  public boolean isValue() {
	return value;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitBoolLit();
  }
}
