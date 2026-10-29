package language.ast;

import java.util.List;

import language.visitor.Visitor;

public final class Block extends Stmt {
  private List<Stmt> stmts;

  public Block(List<Stmt> stmts, int line, int col) {
    super(line, col);
    this.stmts = stmts;
  }

  public List<Stmt> getStmts() {
	return stmts;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitBlock(this);
  }
}
