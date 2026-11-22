package language;

import language.lexer.Lexer;
import language.lexer.LexicalError;
import language.parser.ParseResult;
import language.parser.Parser;
import language.visitor.AstPrinter;
import language.visitor.IdentificationPass;
import language.visitor.SourcePrinter;
import language.visitor.TypeChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

  // Defina NO_COLOR=1 para desligar as cores (por exemplo, ao redirecionar a saída para um arquivo).
  private static final boolean COLOR = System.getenv("NO_COLOR") == null;

  public static void main(String[] args) throws IOException {
    String filePath = null;
    boolean showAst = false;
    boolean showSource = false;
    boolean showUses = false;

    for (String arg : args) {
      switch (arg) {
        case "--ast" -> showAst = true;
        case "--fonte" -> showSource = true;
        case "--usos" -> showUses = true;
        default -> {
          if (arg.startsWith("--")) {
            System.err.println("Opção desconhecida: " + arg);
            usage();
            System.exit(2);
          }
          filePath = arg;
        }
      }
    }

    if (filePath == null) {
      usage();
      System.exit(2);
    }

    Path path = Path.of(filePath);
    if (!Files.isRegularFile(path)) {
      System.err.println("Arquivo não encontrado: " + filePath);
      System.exit(2);
    }

    String code = Files.readString(path);

    rule('=');
    System.out.println("Compilando: " + filePath + " (" + code.lines().count() + " linhas)");
    rule('=');

    // ---------------------------------------------------------------- Fase 1
    phase(1, "Análise léxica e sintática");
    ParseResult result = new Parser(new Lexer(code)).parse();
    List<LexicalError> lexicalErrors = result.lexicalErrors();
    List<String> syntaxErrors = result.errors();

    if (lexicalErrors.isEmpty()) {
      ok("léxica: nenhum erro");
    } else {
      fail("léxica: " + lexicalErrors.size() + " erro(s)");
      for (LexicalError e : lexicalErrors) {
        System.out.println("         " + e);
      }
    }

    if (syntaxErrors.isEmpty()) {
      ok("sintática: nenhum erro");
    } else {
      fail("sintática: " + syntaxErrors.size() + " erro(s)");
      for (String e : syntaxErrors) {
        System.out.println("         " + e);
      }
    }

    if (!result.isValid()) {
      System.out.println();
      rule('-');
      System.out.println(red("COMPILAÇÃO ABORTADA") + ": o programa tem erros léxicos ou sintáticos.");
      System.out.println("As fases 2 (AST), 3 (identificação) e 4 (tipos) não foram executadas.");
      rule('=');
      System.exit(1);
    }

    // ---------------------------------------------------------------- Fase 2
    phase(2, "Construção da AST");
    ok("árvore construída");

    if (showAst) {
      AstPrinter printer = new AstPrinter();
      result.tree().accept(printer);
      System.out.println();
      System.out.println("  [AST]");
      System.out.print(indent(printer.getOutput(), "    "));
    }

    if (showSource) {
      System.out.println();
      System.out.println("  [CÓDIGO REIMPRESSO]");
      System.out.print(indent(result.tree().accept(new SourcePrinter()), "    "));
    }

    // ---------------------------------------------------------------- Fase 3
    phase(3, "Análise de identificação (escopos e símbolos)");
    IdentificationPass pass = new IdentificationPass();
    result.tree().accept(pass);
    List<String> scopeErrors = pass.getErrors();
    List<String> uses = pass.getUses();

    ok(uses.size() + " uso(s) de nome ligado(s) à sua declaração");
    if (showUses) {
      for (String u : uses) {
        System.out.println("         " + u);
      }
    }

    if (scopeErrors.isEmpty()) {
      ok("nenhum erro de escopo");
    } else {
      fail(scopeErrors.size() + " erro(s) de escopo");
      for (String e : scopeErrors) {
        System.out.println("         " + e);
      }
    }

    // ---------------------------------------------------------------- Fase 4
    phase(4, "Verificação de tipos");
    TypeChecker checker = new TypeChecker();
    result.tree().accept(checker);
    List<String> typeErrors = checker.getErrors();
    List<String> warnings = checker.getWarnings();

    if (typeErrors.isEmpty()) {
      ok("nenhum erro de tipo");
    } else {
      fail(typeErrors.size() + " erro(s) de tipo");
      for (String e : typeErrors) {
        System.out.println("         " + e);
      }
    }

    if (!warnings.isEmpty()) {
      warn(warnings.size() + " aviso(s)");
      for (String w : warnings) {
        System.out.println("         " + w);
      }
    }

    // ---------------------------------------------------------------- Resumo
    int total = scopeErrors.size() + typeErrors.size();

    System.out.println();
    rule('-');
    System.out.println("Resumo");
    ok("Análise léxica e sintática");
    ok("Construção da AST");
    if (scopeErrors.isEmpty()) {
      ok("Análise de identificação");
    } else {
      fail("Análise de identificação: " + scopeErrors.size() + " erro(s)");
    }
    if (typeErrors.isEmpty()) {
      ok("Verificação de tipos" + (warnings.isEmpty() ? "" : " (" + warnings.size() + " aviso(s))"));
    } else {
      fail("Verificação de tipos: " + typeErrors.size() + " erro(s)");
    }
    rule('-');

    if (total == 0) {
      System.out.println(green("COMPILAÇÃO CONCLUÍDA SEM ERROS"));
      rule('=');
    } else {
      System.out.println(red("COMPILAÇÃO FALHOU") + ": " + total + " erro(s). A geração de código não foi iniciada.");
      rule('=');
      System.exit(1);
    }
  }

  // ------------------------------------------------------------------ saída

  private static void usage() {
    System.err.println("Uso: java -cp out language.Main <arquivo> [--ast] [--fonte] [--usos]");
    System.err.println("  --ast    mostra a árvore sintática");
    System.err.println("  --fonte  mostra o código reimpresso a partir da árvore");
    System.err.println("  --usos   mostra cada uso de nome e a declaração a que ele resolve");
  }

  private static void phase(int n, String title) {
    System.out.println();
    System.out.println("Fase " + n + ": " + title);
  }

  private static void ok(String msg) {
    System.out.println("  " + green("[OK]   ") + " " + msg);
  }

  private static void fail(String msg) {
    System.out.println("  " + red("[ERRO] ") + " " + msg);
  }

  private static void warn(String msg) {
    System.out.println("  " + yellow("[AVISO]") + " " + msg);
  }

  private static void rule(char c) {
    System.out.println(String.valueOf(c).repeat(60));
  }

  private static String indent(String text, String prefix) {
    StringBuilder sb = new StringBuilder();
    for (String line : text.split("\n", -1)) {
      if (!line.isEmpty()) {
        sb.append(prefix).append(line).append('\n');
      }
    }
    return sb.toString();
  }

  private static String green(String s) {
    return COLOR ? "\u001B[32m" + s + "\u001B[0m" : s;
  }

  private static String red(String s) {
    return COLOR ? "\u001B[31m" + s + "\u001B[0m" : s;
  }

  private static String yellow(String s) {
    return COLOR ? "\u001B[33m" + s + "\u001B[0m" : s;
  }
}
