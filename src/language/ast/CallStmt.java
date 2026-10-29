package language.ast;

import language.visitor.Visitor;

public final class CallStmt extends Stmt {
  private Call call;

  public CallStmt(Call call, int line, int col) {
    super(line, col);
    this.call = call;
  }

  public Call getCall() {
	return call;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitCallStmt();
  }
}
