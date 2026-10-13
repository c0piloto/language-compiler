package language;

import language.lexer.Lexer;
import language.lexer.LexicalError; // Import necessário
import language.lexer.Token;
import language.lexer.TokenType;
import language.parser.Parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Uso: java -cp out language.Main <caminho-do-arquivo>");
      System.exit(1);
    }

    String filePath = args[0];
    String code = Files.readString(Path.of(filePath));

    System.out.println("========================================");
    System.out.println("Analisando arquivo: " + filePath);
    System.out.println("========================================");

    Lexer lexer = new Lexer(code);
    Parser parser = new Parser(lexer);

    boolean syntaxSuccess = parser.parse();
    
    List<LexicalError> lexicalErrors = lexer.getErrors();
    List<String> syntaxErrors = parser.getErrors();

    if (!lexicalErrors.isEmpty()) {
      System.out.println("\n[ERROS LÉXICOS ENCONTRADOS]:");
      lexicalErrors.forEach(System.out::println);
    }

    if (!syntaxErrors.isEmpty()) {
      System.out.println("\n[ERROS SINTÁTICOS ENCONTRADOS]:");
      syntaxErrors.forEach(System.out::println);
    }

    if (syntaxSuccess && lexicalErrors.isEmpty() && syntaxErrors.isEmpty()) {
      System.out.println("\nSTATUS: Programa ACEITO (sintática e lexicamente válido).");
    } else {
      System.out.println("\nSTATUS: Programa REJEITADO.");
    }
    System.out.println("========================================\n");
  }
}