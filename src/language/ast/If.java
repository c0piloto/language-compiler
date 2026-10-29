package language.ast;

import language.visitor.Visitor;

public final class If extends Stmt {
  private final Expr cond;
  private final Block thenBranch;
  private final Stmt elseBranch;   // null, Block ou If (else if)

  public If(Expr cond, Block thenBranch, Stmt elseBranch, int line, int col) {
    super(line, col);
    this.cond = cond;
    this.thenBranch = thenBranch;
    this.elseBranch = elseBranch;
  }

  public Expr getCond() {
    return this.cond;
  }

  public Block getThenBranch() {
    return this.thenBranch;
  }

  public Stmt getElseBranch() {
    return this.elseBranch;
  }

  public boolean hasElse() {
    return this.elseBranch != null;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitIf(this);
  }
}
