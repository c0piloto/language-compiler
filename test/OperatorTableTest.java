import language.ast.Op;
import language.symbols.Type;
import language.visitor.TypeChecker;

import java.util.HashMap;
import java.util.Map;

/**
 * Regras isoladas: a tabela de operadores do papel, sem parser e sem AST.
 * Para cada operador e cada par de tipos, compara o resultado com a lista de combinações accepts.
 * Tudo o que não está na lista tem que ser recusado (null).
 */
public class OperatorTableTest {

  private static final Type[] TYPES =
      {Type.INT, Type.DOUBLE, Type.BOOL, Type.CHAR, Type.STRING, Type.VOID};

  private static final Map<String, Type> ACCEPTED = new HashMap<>();
  private static int verifications = 0;
  private static int fails = 0;

  private static void accept(Op op, Type l, Type r, Type result) {
    ACCEPTED.put(op + "|" + l + "|" + r, result);
  }

  private static void check(String name, Type expected, Type gotten) {
    verifications++;
    if (expected != gotten) {
      fails++;
      System.out.println("  FALHOU " + name + ": expected " + expected + ", gotten " + gotten);
    }
  }

  public static void main(String[] args) {
    // [ARIT-INT] e [ARIT-DBL]
    for (Op op : new Op[] {Op.ADD, Op.SUB, Op.MUL, Op.DIV}) {
      accept(op, Type.INT, Type.INT, Type.INT);
      accept(op, Type.INT, Type.DOUBLE, Type.DOUBLE);
      accept(op, Type.DOUBLE, Type.INT, Type.DOUBLE);
      accept(op, Type.DOUBLE, Type.DOUBLE, Type.DOUBLE);
    }
    // [REL]
    for (Op op : new Op[] {Op.LT, Op.LE, Op.GT, Op.GE}) {
      accept(op, Type.INT, Type.INT, Type.BOOL);
      accept(op, Type.INT, Type.DOUBLE, Type.BOOL);
      accept(op, Type.DOUBLE, Type.INT, Type.BOOL);
      accept(op, Type.DOUBLE, Type.DOUBLE, Type.BOOL);
    }
    // [EQ-IGUAL] e [EQ-NUM]
    for (Op op : new Op[] {Op.EQ, Op.NE}) {
      accept(op, Type.INT, Type.INT, Type.BOOL);
      accept(op, Type.DOUBLE, Type.DOUBLE, Type.BOOL);
      accept(op, Type.BOOL, Type.BOOL, Type.BOOL);
      accept(op, Type.CHAR, Type.CHAR, Type.BOOL);
      accept(op, Type.INT, Type.DOUBLE, Type.BOOL);
      accept(op, Type.DOUBLE, Type.INT, Type.BOOL);
    }
    // [LOG]
    for (Op op : new Op[] {Op.AND, Op.OR}) {
      accept(op, Type.BOOL, Type.BOOL, Type.BOOL);
    }

    TypeChecker checker = new TypeChecker();

    for (Op op : Op.values()) {
      if (op == Op.NOT || op == Op.NEG) continue;
      for (Type l : TYPES) {
        for (Type r : TYPES) {
          Type expected = ACCEPTED.get(op + "|" + l + "|" + r);
          check(op.symbol + " com " + l + " e " + r, expected, checker.binaryResult(op, l, r));
        }
      }
    }

    // [NOT], [NEG-INT], [NEG-DBL]
    for (Type t : TYPES) {
      check("! com " + t, t == Type.BOOL ? Type.BOOL : null, checker.unaryResult(Op.NOT, t));
      Type neg = (t == Type.INT || t == Type.DOUBLE) ? t : null;
      check("- (unário) com " + t, neg, checker.unaryResult(Op.NEG, t));
    }

    System.out.printf("%d verificações, %d falharam%n", verifications, fails);
    System.exit(fails == 0 ? 0 : 1);
  }
}
