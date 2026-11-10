package language.ast;

import language.symbols.Symbol;
import language.visitor.Visitor;

public final class Id extends Expr {
  private final String name;
  private Symbol declaration = null;

  public Id(String name, int line, int col) {
    super(line, col);
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  public Symbol getDeclaration() {
    return this.declaration;
  }

  public void setDeclaration(Symbol declaration) {
    this.declaration = declaration;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitId(this);
  }
}
