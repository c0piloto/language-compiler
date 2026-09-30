package language;

import language.lexer.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      System.err.println("Use: java -cp out language.Main <archive.txt>");
      System.exit(1);
    }

    String code = Files.readString(Path.of(args[0]));

    Lexer lexer = new Lexer(code);

    Token token;

    do {
      token = lexer.nextToken();
      System.out.println(token);
    } while (token.getType() != TokenType.EOF);

    lexer.getErrors().forEach(System.out::println);
  }
}
