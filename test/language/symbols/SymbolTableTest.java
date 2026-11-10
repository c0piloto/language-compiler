package language.symbols;

public class SymbolTableTest {
  
  static void check(String nome, boolean condition) {
    if(!condition) throw new AssertionError("FALHA: " + nome);
    System.out.println("OK " + nome);
  }

  static Symbol symbol(String nome, SymbolTable table) {
    return new Symbol(nome, Category.VARIABLE, null, table.level());
  }

  public static void main(String[] args) {
    SymbolTable table = new StackSymbolTable();

    Symbol x1 = symbol("x", table);
    check("declare devolve true", table.declare(x1));
    check("lookup acha x", table.lookup("x") == x1);
    
    table.openScope();
    Symbol x2 = symbol("x", table);
    check("declare x interno", table.declare(x2));
    check("lookup devolve interno", table.lookup("x") == x2);
    table.closeScope();
    check("depois do closeScope volta o externo", table.lookup("x") == x1);

    check("redeclarar devolve false", !table.declare(symbol("x", table)));
    check("primeiro simbolo permanece", table.lookup("x") == x1);

    table.openScope();
    check("lookupLocal não vê o externo", table.lookupLocal("x") == null);
    check("lookup ainda vê o externo", table.lookup("x") == x1);
    table.closeScope();

    check("lookup inexistente é null", table.lookup("zzz") == null);

    check("level global é 0", table.level() == 0);
    table.openScope();
    check("level 1 após openScope()", table.level() == 1);
    table.closeScope();
    check("level volta a 0 após closeScope()", table.level() == 0);

    System.out.println("Tudo passou.");
  }
}
