package language.ast;

import language.visitor.Visitor;

public final class VarDecl extends Stmt {
  private TypeKind type;
  private String name;
  private Expr init;

  public VarDecl(TypeKind type, String name, Expr init, int line, int col) {
    super(line, col);
    this.type = type;
    this.name = name;
    this.init = init;
  }

  public TypeKind getType() {
	return this.type;
  }

  public String getName() {
	return this.name;
  }

  public Expr getInit() {
	return this.init;
  }

  
  public boolean hasInit() {
    return this.init != null;
  }
  
  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitVarDecl(this);
  }

}
