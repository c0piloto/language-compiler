import language.lexer.Lexer;
import language.parser.ParseResult;
import language.parser.Parser;
import language.visitor.IdentificationPass;
import language.visitor.TypeChecker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Testes do CP5 (verificador de tipos).
 *
 *   javac -d out $(find src -name '*.java')
 *   javac -cp out -d out test/TypeTestRunner.java
 *   java -cp out TypeTestRunner
 *
 * Programa:  test/programas/NOME.txt
 * Esperado:  test/esperado/NOME.txt  (erros de escopo, depois erros de tipo, depois avisos)
 */
public class TypeTestRunner {

  private static final Path PROGRAMAS = Path.of("test", "programas");
  private static final Path ESPERADO = Path.of("test", "esperado");

  private static final List<String> CASOS = List.of(
      "valido_tipos", "erro_expressao", "cond_int", "return_incompativel",
      "chamada_aridade", "chamada_tipo", "procedimento_em_expressao", "varios_erros",
      "funcao_recursiva", "bloco_aninhado", "aviso_sem_valor",
      "d1_int_double", "d2_char", "d3_bool_aritmetica", "d4_string", "d5_igualdade",
      "d6_funcao_comando", "d7_mutua", "d8_argumentos", "d9_return_caminhos",
      "d10_sobrecarga", "d11_funcao_sem_parenteses");

  public static void main(String[] args) {
    int ok = 0, falhas = 0;

    for (String nome : CASOS) {
      try {
        String fonte = Files.readString(PROGRAMAS.resolve(nome + ".txt"));
        ParseResult r = new Parser(new Lexer(fonte)).parse();
        if (!r.isValid()) {
          throw new RuntimeException("o parser rejeitou o programa: " + r.errors() + " " + r.lexicalErrors());
        }

        IdentificationPass ident = new IdentificationPass();
        r.tree().accept(ident);
        TypeChecker checker = new TypeChecker();
        r.tree().accept(checker);

        List<String> obtido = new ArrayList<>(ident.getErrors());
        obtido.addAll(checker.getErrors());
        obtido.addAll(checker.getWarnings());
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

  private static List<String> linhas(String texto) {
    List<String> out = new ArrayList<>(Arrays.asList(texto.replace("\r\n", "\n").split("\n", -1)));
    while (!out.isEmpty() && out.get(out.size() - 1).isBlank()) {
      out.remove(out.size() - 1);
    }
    out.replaceAll(String::stripTrailing);
    return out;
  }
}
