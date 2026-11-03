package language.visitor;

import language.ast.*;

import java.util.ArrayList;
import java.util.List;

// Reimpressor de código-fonte (CP3, parte 2): AST -> texto na sintaxe da linguagem, em forma
// canônica (ast-design.md, seção 8). O texto produzido é lido de volta pelo próprio Parser.
//
// Como funciona: cada visitX devolve o texto do seu nó SEM indentação e SEM quebra de linha no
// fim. Quem decide a indentação é o pai: o visitBlock recua 4 espaços cada linha do texto dos seus
// comandos. Assim o visitor não precisa guardar a profundidade, e o "else if" sai naturalmente:
// o texto do ramo else é um Block ("{ ... }") ou um If ("if (...) ..."), e os dois começam
// direto no caractere certo, logo depois de "} else ".
public final class SourcePrinter implements Visitor<String> {

  private static final String RECUO = "    ";

  // Declarações de topo separadas por uma linha em branco; o texto termina com quebra de linha.
  @Override
  public String visitProgram(Program n) {
    List<String> partes = new ArrayList<>();
    for (Node decl : n.getDeclarations()) {
      partes.add(decl.accept(this));
    }
    return partes.isEmpty() ? "" : String.join("\n\n", partes) + "\n";
  }

  @Override
  public String visitVarDecl(VarDecl n) {
    String texto = n.getType().symbol + " " + n.getName();
    if (n.hasInit()) {
      texto += " = " + n.getInit().accept(this);
    }
    return texto + ";";
  }

  @Override
  public String visitFuncDecl(FuncDecl n) {
    List<String> params = new ArrayList<>();
    for (Param p : n.getParams()) {
      params.add(p.accept(this));
    }
    return n.getReturnType().symbol + " " + n.getName() + "(" + String.join(", ", params) + ") "
        + n.getBody().accept(this);
  }

  @Override
  public String visitParam(Param n) {
    return n.getType().symbol + " " + n.getName();
  }

  // "{", um comando por linha recuado em 4 espaços, e "}" sozinho na última linha.
  @Override
  public String visitBlock(Block n) {
    StringBuilder sb = new StringBuilder("{\n");
    for (Stmt s : n.getStmts()) {
      sb.append(recuar(s.accept(this))).append('\n');
    }
    return sb.append('}').toString();
  }

  @Override
  public String visitAssign(Assign n) {
    return n.getTarget().accept(this) + " = " + n.getValue().accept(this) + ";";
  }

  @Override
  public String visitCallStmt(CallStmt n) {
    return n.getCall().accept(this) + ";";
  }

  // "} else {" e "} else if (...) {" ficam na linha do "}" anterior.
  @Override
  public String visitIf(If n) {
    String texto = "if (" + n.getCond().accept(this) + ") " + n.getThenBranch().accept(this);
    if (n.hasElse()) {
      texto += " else " + n.getElseBranch().accept(this);
    }
    return texto;
  }

  @Override
  public String visitWhile(While n) {
    return "while (" + n.getCond().accept(this) + ") " + n.getBody().accept(this);
  }

  @Override
  public String visitReturn(Return n) {
    return n.hasValue() ? "return " + n.getValue().accept(this) + ";" : "return;";
  }

  // Literais saem com o lexema original, sem reprocessar escapes (007 continua 007).
  @Override
  public String visitIntLit(IntLit n) {
    return n.getLexeme();
  }

  @Override
  public String visitDoubleLit(DoubleLit n) {
    return n.getLexeme();
  }

  @Override
  public String visitBoolLit(BoolLit n) {
    return n.isValue() ? "True" : "False";
  }

  @Override
  public String visitCharLit(CharLit n) {
    return n.getLexeme();
  }

  @Override
  public String visitStringLit(StringLit n) {
    return n.getLexeme();
  }

  @Override
  public String visitId(Id n) {
    return n.getName();
  }

  // Toda Bin e toda Unary entre parênteses, até a de nível mais alto: o parser descarta os
  // parênteses, então o texto relido gera a mesma árvore.
  @Override
  public String visitBin(Bin n) {
    return "(" + n.getLeftOperand().accept(this) + " " + n.getOperator().symbol + " "
        + n.getRightOperand().accept(this) + ")";
  }

  @Override
  public String visitUnary(Unary n) {
    return "(" + n.getOperator().symbol + n.getOperand().accept(this) + ")";
  }

  @Override
  public String visitCall(Call n) {
    List<String> args = new ArrayList<>();
    for (Expr arg : n.getArgs()) {
      args.add(arg.accept(this));
    }
    return n.getCallee().accept(this) + "(" + String.join(", ", args) + ")";
  }

  // Recua em um nível cada linha do texto (linhas vazias ficam vazias, sem espaços sobrando).
  private static String recuar(String texto) {
    StringBuilder sb = new StringBuilder();
    String[] linhas = texto.split("\n", -1);
    for (int i = 0; i < linhas.length; i++) {
      if (i > 0) {
        sb.append('\n');
      }
      if (!linhas[i].isEmpty()) {
        sb.append(RECUO).append(linhas[i]);
      }
    }
    return sb.toString();
  }
}
