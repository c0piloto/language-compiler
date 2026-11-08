package language.symbols;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class StackSymbolTable implements SymbolTable {
  private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

  public StackSymbolTable() {
    this.scopes.push(new HashMap<>());
  }

  @Override
  public void openScope() {
    this.scopes.push(new HashMap<>());
  }

  @Override
  public void closeScope() {
    if(this.scopes.size() == 1){
      throw new IllegalStateException("Tentando tirar da pilha o símbolo 0(inicial)");
    }

    this.scopes.pop();
  }

  @Override
  public boolean declare(Symbol symbol) {
    Map<String, Symbol> top = this.scopes.peek();
    if(top.containsKey(symbol.getName())) {
      return false;
    }

    top.put(symbol.getName(), symbol);
    return true;
  }

  @Override
  public Symbol lookupLocal(String name) {
    return this.scopes.peek().get(name);
  }

  @Override
  public Symbol lookup(String name) {
    for (Map<String, Symbol> scope : this.scopes) {
      Symbol symbol = scope.get(name);
      if(symbol != null) return symbol;
    }
    return null;
  }

  @Override
  public int level() {
    return scopes.size() - 1;
  }
}
