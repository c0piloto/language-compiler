package language.ast;

import java.util.List;

import language.visitor.Visitor;

// Cobre função e procedimento: procedimento = returnType VOID.
public final class FuncDecl extends Node {
  private final TypeKind returnType;
  private final String name;
  private final List<Param> params;
  private final Block body;
  // Mais tarde: private Symbol symbol;

  public FuncDecl(TypeKind returnType, String name, List<Param> params, Block body,
      int line, int col) {
    super(line, col);
    this.returnType = returnType;
    this.name = name;
    this.params = params;
    this.body = body;
  }

  public TypeKind getReturnType() {
    return this.returnType;
  }

  public String getName() {
    return this.name;
  }

  public List<Param> getParams() {
    return this.params;
  }

  public Block getBody() {
    return this.body;
  }

  @Override
  public <T> T accept(Visitor<T> visitor) {
    return visitor.visitFuncDecl(this);
  }
}
