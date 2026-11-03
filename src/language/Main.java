package language;

import language.lexer.Lexer;
import language.lexer.LexicalError;
import language.parser.ParseResult;
import language.parser.Parser;
import language.visitor.AstPrinter;
import language.visitor.SourcePrinter;

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

    ParseResult result = parser.parse();

    List<LexicalError> lexicalErrors = result.lexicalErrors();
    List<String> syntaxErrors = result.errors();

    if (!lexicalErrors.isEmpty()) {
      System.out.println("\n[ERROS LÉXICOS ENCONTRADOS]:");
      lexicalErrors.forEach(System.out::println);
    }

    if (!syntaxErrors.isEmpty()) {
      System.out.println("\n[ERROS SINTÁTICOS ENCONTRADOS]:");
      syntaxErrors.forEach(System.out::println);
    }

    if (result.isValid()) {
      System.out.println("\nSTATUS: Programa ACEITO (sintática e lexicamente válido).");

      AstPrinter printer = new AstPrinter();
      result.tree().accept(printer);
      System.out.println("\n[AST]:");
      System.out.print(printer.getOutput());

      System.out.println("\n[CÓDIGO REIMPRESSO]:");
      System.out.print(result.tree().accept(new SourcePrinter()));
    } else {
      System.out.println("\nSTATUS: Programa REJEITADO.");
    }
    System.out.println("========================================\n");
  }
}
