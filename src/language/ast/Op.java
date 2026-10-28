package language.ast;

public enum Op {
  OR("||"), AND("&&"),
  EQ("=="), NE("!="),
  LT("<"), LE("<="), GT(">"), GE(">="),
  ADD("+"), SUB("-"), MUL("*"), DIV("/"),
  NOT("!"), NEG("-");

  public final String symbol;

  Op(String symbol) {
    this.symbol = symbol;
  }
}
