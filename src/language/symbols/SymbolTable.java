package language.symbols;

public interface SymbolTable {
  void openScope();
  void closeScope();
  boolean declare(Symbol symbol);
  Symbol lookupLocal(String name);
  Symbol lookup(String name);
  int level();
}
