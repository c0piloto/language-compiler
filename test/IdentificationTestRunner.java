import language.lexer.Lexer;
import language.parser.ParseResult;
import language.parser.Parser;
import language.visitor.IdentificationPass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Testes do CP4 (passada de identificação).
 *
 *   javac -d out $(find src -name '*.java')
 *   javac -cp out -d out test/IdentificationTestRunner.java
 *   java -cp out IdentificationTestRunner
 *
 * Programa:  test/programas/NOME.txt
 * Esperado:  test/esperado/NOME.txt  (erros na ordem do texto, depois os usos resolvidos)
 */
public class IdentificationTestRunner {

  private static final Path PROGRAMAS = Path.of("test", "programas");
  private static final Path ESPERADO = Path.of("test", "esperado");

  private static final List<String> CASOS = List.of(
      "valido_sombreamento", "redeclaracao", "nao_declarado",
      "d1_sombreamento", "d1_redeclaracao", "d2_declarar_antes",
      "d3_mutua", "d4_param_corpo", "d5_namespace",
      "tres_erros", "recursivo", "uso_apos_bloco");

  public static void main(String[] args) {
    int ok = 0, falhas = 0;

    for (String nome : CASOS) {
      try {
        String fonte = Files.readString(PROGRAMAS.resolve(nome + ".txt"));
        ParseResult r = new Parser(new Lexer(fonte)).parse();
        if (!r.isValid()) {
          throw new RuntimeException("o parser rejeitou o programa: " + r.errors() + " " + r.lexicalErrors());
        }

        IdentificationPass pass = new IdentificationPass();
        r.tree().accept(pass);

        List<String> obtido = new ArrayList<>(pass.getErrors());
        obtido.addAll(pass.getUses());
        List<String> esperado = linhas(Files.readString(ESPERADO.resolve(nome + ".txt")));

        if (esperado.equals(obtido)) {
          ok++;
          System.out.println("  OK     " + nome);
        } else {
          falhas++;
          System.out.println("  FALHOU " + nome);
          System.out.println("    esperado: " + esperado);
          System.out.println("    obtido:   " + obtido);
        }
      } catch (IOException e) {
        falhas++;
        System.out.println("  FALHOU " + nome + " -> não consegui ler o arquivo: " + e.getMessage());
      } catch (Throwable e) {
        falhas++;
        System.out.println("  FALHOU " + nome + " -> " + e);
      }
    }

    System.out.printf("%nResumo: %d OK, %d FALHOU%n", ok, falhas);
    System.exit(falhas == 0 ? 0 : 1);
  }

  // Ignora fim de linha do Windows e linhas em branco no final.
  private static List<String> linhas(String texto) {
    List<String> out = new ArrayList<>(Arrays.asList(texto.replace("\r\n", "\n").split("\n", -1)));
    while (!out.isEmpty() && out.get(out.size() - 1).isBlank()) {
      out.remove(out.size() - 1);
    }
    out.replaceAll(String::stripTrailing);
    return out;
  }
}
