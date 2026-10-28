package language.parser;

import language.ast.Program;
import language.lexer.LexicalError;

import java.util.List;

// tree == null significa que houve erro léxico ou sintático:
// a árvore não pode ser entregue às fases seguintes.
public record ParseResult(Program tree, List<String> errors, List<LexicalError> lexicalErrors) {
  public boolean isValid() {
    return tree != null;
  }
}
