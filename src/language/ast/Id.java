package language.ast;

import language.visitor.Visitor;

public final class Id extends Expr {
  private final String name;
  // Mais tarde: private Symbol symbol; (tabela de símbolos)

  public Id(String name, int line, int col) {
    super(line, col);
    this.name = name;
  }

  public String getName() {
    return this.name;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitId(this);
  }
}
