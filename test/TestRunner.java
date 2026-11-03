import language.lexer.Lexer;
import language.parser.ParseResult;
import language.parser.Parser;
import language.visitor.AstPrinter;
import language.visitor.SourcePrinter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executor de testes do CP3 (classe main com asserts simples, sem JUnit).
 *
 * Como rodar, a partir da raiz do repositório:
 *
 *   javac -d out $(find src -name '*.java')
 *   javac -cp out -d out test/TestRunner.java
 *   java -cp out TestRunner
 *
 * Cada teste termina em OK, FALHOU ou PENDENTE (falta algo que o grupo precisa escrever).
 * O programa só sai com código 0 se tudo estiver OK.
 *
 *
 * Arquivos esperados:
 *   test/esperado/NOME.txt     árvore no formato do AstPrinter (seções 1 e 2 da main)
 *   test/esperado/NOME.erros   (opcional) uma linha "linha:coluna" por erro sintático esperado
 */
public class TestRunner {

  private static final Path DIR = Path.of("test");
  private static final Path ESPERADO = DIR.resolve("esperado");
  private static final Pattern POSICAO = Pattern.compile("(\\d+):(\\d+)");

  private static int ok = 0;
  private static int falhas = 0;
  private static int pendentes = 0;

  private record Prog(String nome, Supplier<String> fonte) {}

  private interface Teste {
    void rodar() throws Exception;
  }

  @SuppressWarnings("serial")
  private static final class Falha extends RuntimeException {
    Falha(String mensagem) {
      super(mensagem, null, false, false);
    }
  }

  @SuppressWarnings("serial")
  private static final class Pendente extends RuntimeException {
    Pendente(String mensagem) {
      super(mensagem, null, false, false);
    }
  }

  // ===========================================================================================
  // Lista de testes. Para acrescentar um caso, acrescente uma linha aqui.
  // ===========================================================================================
  public static void main(String[] args) {
    List<Prog> validos = List.of(
        new Prog("valido1", () -> arq("valido1.txt")),
        new Prog("valido2", () -> arq("valido2.txt")),
        new Prog("valido3", () -> arq("valido3.txt")));

    List<Prog> expressoes = List.of(
        new Prog("prec_mul", () -> "int r = a + b * c;"),
        new Prog("prec_assoc", () -> "int r = a - b - c;"),
        new Prog("prec_paren", () -> "int r = a - (b - c);"));

    secao("1. Programas válidos: árvore impressa == test/esperado/NOME.txt");
    for (Prog p : validos) {
      testeArvore(p.nome(), p.fonte());
    }

    secao("2. Precedência e associatividade: árvore impressa == test/esperado/NOME.txt");
    for (Prog p : expressoes) {
      testeArvore(p.nome(), p.fonte());
    }
    testeDiferentes("a - b - c  x  a - (b - c)", expressoes.get(1).fonte(), expressoes.get(2).fonte());

    secao("3. A AST não é a árvore de derivação: ((a)) tem a mesma árvore que a");
    testeMesmaArvore("parenteses_redundantes", () -> "int r = ((a));", () -> "int r = a;");

    secao("4. Ida e volta: imprimir -> ler -> imprimir (programas válidos)");
    for (Prog p : validos) {
      testeIdaEVolta(p.nome(), p.fonte());
    }
    for (Prog p : expressoes) {
      testeIdaEVolta(p.nome(), p.fonte());
    }

    secao("5. Regressão do CP2");
    testeRejeita("invalido1", () -> arq("invalido1.txt"));
    testeRejeita("invalido2", () -> arq("invalido2.txt"));
    testeRejeita("invalido3", () -> arq("invalido3.txt"));
    testeMultiplosErros("multiplos_erros", () -> arq("multiplos_erros.txt"), 2);

    System.out.printf("%nResumo: %d OK, %d FALHOU, %d PENDENTE%n", ok, falhas, pendentes);
    System.exit(falhas + pendentes == 0 ? 0 : 1);
  }

  // ===========================================================================================
  // Tipos de teste
  // ===========================================================================================

  // Programa válido: a árvore impressa tem que ser igual ao arquivo esperado (escrito à mão).
  private static void testeArvore(String nome, Supplier<String> fonte) {
    teste("árvore: " + nome, () -> {
      ParseResult r = analisar(fonte.get());
      exigirValido(r);
      Path esperado = ESPERADO.resolve(nome + ".txt");
      if (!Files.exists(esperado)) {
        throw new Pendente("escrevam à mão " + esperado + " antes de rodar o impressor");
      }
      comparar(Files.readString(esperado), arvore(r), "árvore diferente da esperada");
    });
  }

  // As duas entradas são válidas e o impressor da árvore as distingue.
  private static void testeDiferentes(String nome, Supplier<String> a, Supplier<String> b) {
    teste("árvores diferentes: " + nome, () -> {
      ParseResult ra = analisar(a.get());
      ParseResult rb = analisar(b.get());
      exigirValido(ra);
      exigirValido(rb);
      if (normalizar(arvore(ra)).equals(normalizar(arvore(rb)))) {
        throw new Falha("as duas árvores saíram iguais: o impressor não distingue as duas formas");
      }
    });
  }

  // As duas entradas geram a mesma árvore e ela não tem nenhum nó de parênteses.
  private static void testeMesmaArvore(String nome, Supplier<String> a, Supplier<String> b) {
    teste("mesma árvore: " + nome, () -> {
      ParseResult ra = analisar(a.get());
      ParseResult rb = analisar(b.get());
      exigirValido(ra);
      exigirValido(rb);
      String arvoreA = arvore(ra);
      comparar(arvore(rb), arvoreA, "as árvores deveriam ser iguais");
      if (arvoreA.toLowerCase().contains("paren")) {
        throw new Falha("a árvore tem um nó de parênteses");
      }
    });
  }

  // Ler, reimprimir, ler o texto reimpresso e reimprimir de novo. Os dois textos reimpressos
  // e as duas árvores (pelo impressor da árvore) têm que ser iguais.
  private static void testeIdaEVolta(String nome, Supplier<String> fonte) {
    teste("ida e volta: " + nome, () -> {
      ParseResult r1 = analisar(fonte.get());
      exigirValido(r1);
      String texto1 = reimprimir(r1);

      ParseResult r2 = analisar(texto1);
      if (!r2.isValid()) {
        throw new Falha("o parser não leu de volta o texto reimpresso: " + r2.errors() + " "
            + r2.lexicalErrors() + "\n--- texto reimpresso ---\n" + texto1);
      }
      String texto2 = reimprimir(r2);

      comparar(texto1, texto2, "o segundo texto reimpresso difere do primeiro");
      comparar(arvore(r1), arvore(r2), "as árvores diferem depois da ida e volta");
    });
  }

  // Programa inválido: tem que ser rejeitado, com erro reportado, sem exceção.
  private static void testeRejeita(String nome, Supplier<String> fonte) {
    teste("rejeita: " + nome, () -> {
      ParseResult r = analisar(fonte.get());
      if (r.isValid()) {
        throw new Falha("o parser aceitou um programa inválido");
      }
      if (r.errors().isEmpty() && r.lexicalErrors().isEmpty()) {
        throw new Falha("rejeitou sem reportar nenhum erro");
      }
    });
  }

  // Vários erros na mesma rodada (modo pânico). Se existir test/esperado/NOME.erros, as posições
  // reportadas também têm que ser exatamente as listadas lá.
  private static void testeMultiplosErros(String nome, Supplier<String> fonte, int minimo) {
    teste("múltiplos erros: " + nome, () -> {
      ParseResult r = analisar(fonte.get());
      if (r.isValid()) {
        throw new Falha("o parser aceitou um programa inválido");
      }
      if (r.errors().size() < minimo) {
        throw new Falha("esperava pelo menos " + minimo + " erros sintáticos na mesma rodada, vieram "
            + r.errors().size());
      }
      Path esperado = ESPERADO.resolve(nome + ".erros");
      if (Files.exists(esperado)) {
        comparar(Files.readString(esperado), posicoes(r), "posições dos erros diferentes das esperadas");
      }
    });
  }

  // ===========================================================================================
  // Auxiliares
  // ===========================================================================================

  private static ParseResult analisar(String fonte) {
    return new Parser(new Lexer(fonte)).parse();
  }

  private static void exigirValido(ParseResult r) {
    if (!r.isValid()) {
      throw new Falha("o parser rejeitou o programa: " + r.errors() + " " + r.lexicalErrors());
    }
  }

  private static String arvore(ParseResult r) {
    AstPrinter impressora = new AstPrinter();
    r.tree().accept(impressora);
    return impressora.getOutput();
  }

  private static String reimprimir(ParseResult r) {
    String texto = r.tree().accept(new SourcePrinter());
    if (texto == null) {
      throw new Falha("o SourcePrinter devolveu null");
    }
    return texto;
  }

  // "linha:coluna" de cada erro sintático, uma por linha.
  private static String posicoes(ParseResult r) {
    StringBuilder sb = new StringBuilder();
    for (String erro : r.errors()) {
      Matcher m = POSICAO.matcher(erro);
      sb.append(m.find() ? m.group(1) + ":" + m.group(2) : "?").append('\n');
    }
    return sb.toString();
  }

  private static String arq(String nome) {
    try {
      return Files.readString(DIR.resolve(nome));
    } catch (IOException e) {
      throw new Falha("não consegui ler " + DIR.resolve(nome) + " (rode a partir da raiz do repositório)");
    }
  }

  // Ignora diferenças de fim de linha e espaços no fim das linhas; a indentação é preservada.
  private static String normalizar(String s) {
    String[] linhas = s.replace("\r\n", "\n").split("\n", -1);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < linhas.length; i++) {
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(linhas[i].stripTrailing());
    }
    return sb.toString().stripTrailing();
  }

  private static void comparar(String esperado, String obtido, String mensagem) {
    String[] e = normalizar(esperado).split("\n", -1);
    String[] o = normalizar(obtido).split("\n", -1);
    if (Arrays.equals(e, o)) {
      return;
    }
    int i = 0;
    while (i < e.length && i < o.length && e[i].equals(o[i])) {
      i++;
    }
    String linhaEsperada = i < e.length ? e[i] : "(fim do texto)";
    String linhaObtida = i < o.length ? o[i] : "(fim do texto)";
    throw new Falha(mensagem + "; primeira diferença na linha " + (i + 1) + ":\n"
        + "        esperado: " + linhaEsperada + "\n"
        + "        obtido:   " + linhaObtida);
  }

  private static void secao(String titulo) {
    System.out.println("\n== " + titulo + " ==");
  }

  private static void teste(String nome, Teste t) {
    try {
      t.rodar();
      ok++;
      System.out.println("  OK        " + nome);
    } catch (Pendente | UnsupportedOperationException e) {
      pendentes++;
      System.out.println("  PENDENTE  " + nome + " -> " + e.getMessage());
    } catch (Falha e) {
      falhas++;
      System.out.println("  FALHOU    " + nome + " -> " + e.getMessage());
    } catch (Throwable e) {
      falhas++;
      System.out.println("  FALHOU    " + nome + " -> EXCEÇÃO ESCAPOU: " + e);
    }
  }
}
