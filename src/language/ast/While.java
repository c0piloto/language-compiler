package language.ast;

import language.visitor.Visitor;

public final class While extends Stmt {
  private final Expr cond;
  private final Block body;

  public While(Expr cond, Block body, int line, int col) {
    super(line, col);
    this.cond = cond;
    this.body = body;
  }

  public Expr getCond() {
    return this.cond;
  }

  public Block getBody() {
    return this.body;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitWhile(this);
  }
}
