package language.symbols;

import language.ast.Node;

public class Symbol {
  private final String name;
  private final Category category;
  private final Node declaration;
  private final int level;
  
  private Type type = null; // TODO: Implement type on checkpoint 5
  private int displace = -1;

  public Symbol(String name, Category category, Node declaration, int level) {
    this.name = name;
    this.category = category;
    this.declaration = declaration;
    this.level = level;
  }

  // Getters and Setters
  public String getName() {
	return name;
  }

  public Category getCategory() {
	return category;
  }

  public Node getDeclaration() {
	return declaration;
  }

  public int getLevel() {
	return level;
  }

  /*
  TODO: When Type created uncomment
  public Type getType() {
	return type;
  }
  */
  public int getDisplace() {
	return displace;
  }

  // Simple line return
  public int line() {
    return declaration.getLine();
  }

}
