package language.ast;

public abstract class Stmt extends Node {
  protected Stmt(int line, int col){
    super(line, col);
  }
}
