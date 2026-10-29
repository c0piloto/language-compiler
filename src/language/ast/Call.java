package language.ast;

import java.util.List;

import languange.visitor.Visitor;

public final class Call extends Expr {
  private Id callee;
  private List<Expr> args;
  
  public Call(Id callee, List<Expr> args, int line, int col) {
    super(line, col);
    this.callee = callee;
    this.args = args;
  }

  public Id getCallee() {
	return callee;
  }
  
  public List<Expr> getArgs() {
	return args;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitCall();
  }
}
