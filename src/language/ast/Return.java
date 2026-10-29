package language.ast;

import language.visitor.Visitor;

public final class Return extends Stmt {
  private final Expr value;   // null em "return;"

  public Return(Expr value, int line, int col) {
    super(line, col);
    this.value = value;
  }

  public Expr getValue() {
    return this.value;
  }

  public boolean hasValue() {
    return this.value != null;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitReturn(this);
  }
}
