# Projeto da AST

Construção de Compiladores · Checkpoint 3

Este documento projeta a árvore de sintaxe abstrata (AST) da linguagem do grupo a partir da gramática LL(1) final do CP2 (`gramatica-first-follow.md`, sem alterações). Os nomes de não-terminais usados aqui são os da gramática do CP2. A implementação é em Java, nos pacotes `language.lexer` (scanner do CP1, sem modificação), `language.parser`, `language.ast` (nós) e `language.visitor` (`Visitor<T>` e os dois visitors). O `Token` do CP1 expõe `getType()`, `getLexema()`, `getLine()` e `getCol()` (linha e coluna começam em 1), e o `Lexer` já distingue `LIT_INT` de `LIT_DOUBLE`.

---

## 1. Hierarquia de nós

Critério: um tipo de nó por categoria sintática que importa para as fases seguintes, não um por não-terminal.

Classes abstratas: `Node` (raiz; guarda `line`, `col` e declara `abstract <T> T accept(Visitor<T> v)`), `Stmt extends Node` e `Expr extends Node`.

`TypeKind` é um `enum { INT, DOUBLE, BOOL, CHAR, STRING, VOID }`. `Op` é um `enum` com os operadores (ver `Bin` e `Unary`).

| Nó | Super | Atributos | Produção(ões) do CP2 |
| :--- | :--- | :--- | :--- |
| `Program` | `Node` | `List<Node> decls` (cada elemento é `VarDecl` ou `FuncDecl`, na ordem do fonte) | `Program -> Decl Program \| ε` |
| `VarDecl` | `Stmt` | `TypeKind type`, `String name`, `Expr init` (pode ser `null`) | `Decl -> Type ID DeclRest` com `DeclRest -> VarRest ;`; `Stmt -> Type ID VarRest ;`; `VarRest -> = Expr \| ε` |
| `FuncDecl` | `Node` | `TypeKind returnType`, `String name`, `List<Param> params`, `Block body` | `Decl -> Type ID DeclRest` com `DeclRest -> ( Params ) Block` |
| `Param` | `Node` | `TypeKind type`, `String name` | `Param -> Type ID` |
| `Block` | `Stmt` | `List<Stmt> stmts` | `Block -> { StmtList }`; `Stmt -> Block` |
| `Assign` | `Stmt` | `Id target`, `Expr value` | `Stmt -> ID IdStmt`, `IdStmt -> = Expr ;` |
| `CallStmt` | `Stmt` | `Call call` | `Stmt -> ID IdStmt`, `IdStmt -> ( Args ) ;` |
| `If` | `Stmt` | `Expr cond`, `Block thenBranch`, `Stmt elseBranch` (`null`, `Block` ou `If`) | `IfStmt -> if ( Expr ) Block ElsePart`; `ElsePart -> else ElseBody \| ε`; `ElseBody -> Block \| IfStmt` |
| `While` | `Stmt` | `Expr cond`, `Block body` | `Stmt -> while ( Expr ) Block` |
| `Return` | `Stmt` | `Expr value` (pode ser `null`) | `Stmt -> return ReturnVal ;`; `ReturnVal -> Expr \| ε` |
| `IntLit` | `Expr` | `String lexeme` | `Primary -> NUM` (token `LIT_INT`) |
| `DoubleLit` | `Expr` | `String lexeme` | `Primary -> NUM` (token `LIT_DOUBLE`) |
| `BoolLit` | `Expr` | `boolean value` | `Primary -> True \| False` |
| `CharLit` | `Expr` | `String lexeme` (como escrito, com aspas e escapes) | `Primary -> CHAR_LIT` (token `LIT_CHAR`) |
| `StringLit` | `Expr` | `String lexeme` (como escrito, com aspas e escapes) | `Primary -> STR_LIT` (token `LIT_STRING`) |
| `Id` | `Expr` | `String name` | `Primary -> ID PrimaryRest` (com `PrimaryRest -> ε`); alvo de `Assign` |
| `Bin` | `Expr` | `Op op`, `Expr left`, `Expr right` | `OrTail`, `AndTail`, `EqTail`, `RelTail`, `AddTail`, `MulTail` |
| `Unary` | `Expr` | `Op op` (`NOT` ou `NEG`), `Expr operand` | `Unary -> ! Unary \| - Unary` |
| `Call` | `Expr` | `Id callee`, `List<Expr> args` | `Primary -> ID PrimaryRest`, `PrimaryRest -> ( Args )`; também dentro de `CallStmt` |

`Op` para `Bin`: `OR` (`||`), `AND` (`&&`), `EQ` (`==`), `NE` (`!=`), `LT`, `LE`, `GT`, `GE`, `ADD`, `SUB`, `MUL`, `DIV`. Para `Unary`: `NOT` (`!`) e `NEG` (`-`).

Total: 19 tipos concretos de nó.

---

## 2. Mapeamento da gramática para a árvore

Cada não-terminal da gramática do CP2 vira um nó, vira atributo de outro nó, ou não vira nada.

| Não-terminal | Destino | Justificativa / o que acontece |
| :--- | :--- | :--- |
| `Program` | nó `Program` | O `Program -> Decl Program` recursivo à direita vira a lista `decls`. |
| `Decl` | nó `VarDecl` ou `FuncDecl` | É um único não-terminal na gramática, mas dois conceitos distintos; o parser decide só depois de ler `Type ID`, olhando o `DeclRest`. |
| `DeclRest` | nada | **(c) fatoração.** Só existe para adiar a escolha entre variável e função; o parser usa o primeiro token (`(` ou outro) para decidir qual nó criar. |
| `VarRest` | atributo `init` de `VarDecl` | `ε` vira `null`. |
| `Params` / `ParamTail` | atributo `params` de `FuncDecl` | **(c) fatoração** (de `Param \| Param , Params`). A lista é montada por laço; as vírgulas somem. |
| `Param` | nó `Param` | |
| `Type` | atributo `TypeKind` | Um enum basta; a posição é a do próprio nó dono, que começa nesse token. |
| `Block` | nó `Block` | |
| `StmtList` | atributo `stmts` de `Block` | Recursão à direita vira lista. |
| `Stmt` | um dos nós `Stmt` | Não é nó próprio; é a escolha entre `VarDecl`, `Assign`, `CallStmt`, `If`, `Block`, `While`, `Return`. |
| `IdStmt` | nó `Assign` ou `CallStmt` | **(c) fatoração** do prefixo comum `ID`. O `ID` já lido vira `Id` (alvo) ou `callee`. |
| `IfStmt` | nó `If` | |
| `ElsePart` / `ElseBody` | atributo `elseBranch` de `If` | **(c) fatoração**. `ε` vira `null`; `else if` vira um `If` aninhado. |
| `ReturnVal` | atributo `value` de `Return` | **(c) fatoração**. `ε` vira `null`. |
| `Expr`, `AndExpr`, `EqExpr`, `RelExpr`, `AddExpr`, `MulExpr` | nada | **(a) precedência.** Os 6 níveis só impõem a ordem de precedência na derivação. Na AST a precedência está na forma da árvore (quem fica mais fundo liga primeiro), então os níveis não viram nó. Cada um só devolve o que o nível abaixo devolveu, ou um `Bin` quando há operador. |
| `OrTail`, `AndTail`, `EqTail`, `RelTail`, `AddTail`, `MulTail` | nada | **(b) eliminação de recursão à esquerda.** Recebem o operando esquerdo já construído, e a cada operador lido devolvem `Bin(op, esquerda, direita)` como novo operando esquerdo. O `ε` encerra o laço e devolve a árvore montada. |
| `Unary` | nó `Unary` (se houver `!` ou `-`) | Sem operador, só repassa o `Primary`. |
| `Primary` | nó literal, `Id` ou `Call`; ou a expressão interna | `( Expr )` devolve a própria `Expr` interna: **parênteses não viram nó**. |
| `PrimaryRest` | decide entre `Id` e `Call` | **(c) fatoração** do prefixo comum `ID`. `ε` produz `Id`; `( Args )` produz `Call`. |
| `Args` / `ArgTail` | atributo `args` de `Call` | **(c) fatoração**. Lista montada por laço. |

Os três casos exigidos:

- **(a) Não-terminais de precedência:** `Expr`, `AndExpr`, `EqExpr`, `RelExpr`, `AddExpr`, `MulExpr` (equivalentes ao `E`/`T`/`F` da aula).
- **(b) Não-terminais de eliminação de recursão à esquerda:** `OrTail`, `AndTail`, `EqTail`, `RelTail`, `AddTail`, `MulTail` (equivalentes ao `E'`/`T'`).
- **(c) Não-terminais criados por fatoração:** `DeclRest`, `VarRest`, `IdStmt`, `PrimaryRest`, `ElsePart`, `ElseBody`, `ReturnVal`, `ParamTail`, `ArgTail`.

**Terminais que não viram nó:** `(`, `)`, `{`, `}`, `,`, `;` e as palavras reservadas `if`, `else`, `while`, `return`, `int`, etc. As palavras reservadas só informam qual nó criar (ou viram o `TypeKind`).

---

## 3. Decisões de representação

1. **Procedimento e função são o mesmo nó (`FuncDecl`).** O procedimento é uma função com `returnType == VOID`. A gramática já aceita `void` em `Type` e a restrição de uso é semântica (decisão do CP2); dois nós seriam duplicação sem diferença estrutural.
2. **Declaração local e global compartilham `VarDecl`.** Por isso `VarDecl extends Stmt` (pode estar em um `Block`), e `Program.decls` é `List<Node>`. Alternativa descartada: um nó `DeclStmt` embrulhando `VarDecl`, que acrescentaria um nível sem informação nova.
3. **Um `Bin` para os 12 operadores binários e um `Unary` para os 2 unários.** O que muda entre eles é o atributo `op`, não a forma da árvore, e a análise semântica terá regras por categoria de operador de qualquer forma.
4. **`IntLit` e `DoubleLit` guardam o lexema (`String`), não o valor numérico.** A gramática tem um só `NUM`, mas o `Lexer` já separa `LIT_INT` e `LIT_DOUBLE`, e o tipo do token decide o nó. Guardar o texto evita `NumberFormatException` em literais muito grandes (o parser não pode lançar exceção em nenhuma entrada) e deixa a conversão e a checagem de faixa para a fase semântica.
5. **`CharLit` e `StringLit` também guardam o lexema como o `Lexer` o entrega**, com as aspas e os escapes ainda escritos (`"a\nb"` continua sendo barra e `n`). Decodificar os escapes no parser seria arriscado: o `Lexer` registra escape desconhecido como erro léxico mas ainda devolve o token (por exemplo `"a\q"`), e um decodificador ingênuo poderia lançar exceção. Guardar o texto faz o impressor e o reimpressor reproduzirem o literal sem reprocessar nada, e a decodificação fica para a fase semântica, se for preciso.
6. **`Assign.target` e `Call.callee` são nós `Id`**, não `String`, porque são usos de identificador e é neles que a tabela de símbolos vai pendurar a referência.
7. **`VarDecl`, `FuncDecl` e `Param` guardam o nome como `String`.** São declarações: criam o símbolo, e o próprio nó vai apontar para ele.
8. **`CallStmt` embrulha um `Call`** em vez de ser uma subclasse de `Expr`, porque a gramática só permite chamada como comando (`ID ( Args ) ;`), e a atribuição não é expressão. Assim a árvore não admite coisas como `1 + 2;`.
9. **`If.elseBranch` é `null`, `Block` ou `If`.** O `else if` fica como `If` aninhado, o que é exatamente o `ElseBody -> Block | IfStmt`.
10. **Nulos só em três campos:** `VarDecl.init`, `If.elseBranch` e `Return.value`. Todas as listas são sempre não-nulas (vazias quando não há elementos).

---

## 4. Posição

Todo nó guarda `line` e `col` do **primeiro token** que o originou (`Token.getLine()` e `Token.getCol()`, as mesmas coordenadas usadas nos erros léxicos).

| Nó | Token de posição |
| :--- | :--- |
| `Program` | primeiro token do arquivo (ou 1:1 se o programa é vazio) |
| `VarDecl`, `FuncDecl`, `Param` | o token de tipo |
| `Block` | `{` |
| `Assign` | o `ID` do alvo |
| `CallStmt`, `Call` | o `ID` do callee |
| `If`, `While`, `Return` | `if`, `while`, `return` |
| literais, `Id` | o próprio token |
| `Unary` | o operador (`!` ou `-`) |
| `Bin` | primeiro token do operando esquerdo (que é o primeiro token da expressão toda) |
| expressão entre parênteses | a posição do nó interno; o `(` é descartado junto com o parêntese |

Por isso `Node` recebe `line` e `col` no construtor, e nenhuma subclasse de nó pode ser criada sem posição.

---

## 5. Campos que as próximas fases vão acrescentar

Nenhum deles muda a forma da árvore; são campos novos, preenchidos depois pelo analisador:

| Fase | Campo | Onde |
| :--- | :--- | :--- |
| Tabela de símbolos | `Symbol symbol` | `Id`, `VarDecl`, `FuncDecl`, `Param` |
| Análise semântica | `Type type` (tipo da expressão) | `Expr` (raiz da hierarquia de expressões; herdado por todas as subclasses) |

O campo de tipo fica em `Expr` porque toda expressão carrega um tipo, e o de símbolo fica nos nós que declaram ou usam um nome.

---

## 6. Como o parser monta a árvore

Cada `parseX()` que corresponde a um nó devolve esse nó; `parse()` devolve o `Program`. Os auxiliares de recursão à esquerda recebem o operando esquerdo já pronto:

```java
// AddExpr -> MulExpr AddTail
private Expr parseAddExpr() {
  return parseAddTail(parseMulExpr());
}

// AddTail -> + MulExpr AddTail | - MulExpr AddTail | ε
private Expr parseAddTail(Expr left) {
  if (at(PLUS) || at(MINUS)) {
    Token op = token;
    advance();
    Expr right = parseMulExpr();
    return parseAddTail(new Bin(opOf(op), left, right));  // associa à esquerda
  } else if (inSet(L4)) {
    return left;                                  // ε, decidido por FOLLOW
  } else {
    fail(union(L4, PLUS, MINUS));                 // recuperação do CP2, igual
    synchronize(L4);
    return left;
  }
}
```

(O construtor de `Bin` copia a posição de `left`, que é o primeiro token da expressão; se `left` for `null`, usa 0:0, o que só acontece depois de um erro, quando a árvore será descartada de qualquer forma.)

Para `a - b - c`: `parseMulExpr` devolve `a`; `AddTail` lê `-`, `b`, monta `Bin(-, a, b)` e se chama de novo com ele como `left`; lê `-`, `c` e monta `Bin(-, Bin(-, a, b), c)`. A associatividade à esquerda sai da ordem em que as chamadas montam os nós.

### 6.1 Assinaturas: parser do CP2 → parser com AST

As decisões por FIRST/FOLLOW, os `fail(...)` e os `synchronize(...)` de cada método ficam como estão. Muda só o tipo de retorno e o que cada ramo devolve.

| Método atual (retorna `void`) | Passa a devolver | Observação |
| :--- | :--- | :--- |
| `parse()` (devolvia `boolean`) | `ParseResult` | Seção 6.2 |
| `parseProgram()` | `Program` | A recursão `Decl Program` continua; acumula em uma `List<Node>` passada por parâmetro. |
| `parseDecl()` | `Node` (`VarDecl` ou `FuncDecl`) | Guarda o token de tipo e o `ID` e os passa ao `parseDeclRest`. |
| `parseDeclRest(...)` | `Node` | Recebe `TypeKind`, token do tipo e token do nome; o ramo `(` cria `FuncDecl`, o ramo `=`/`;` cria `VarDecl`. |
| `parseVarRest()` | `Expr` | `null` no ramo `ε` (e em erro). |
| `parseParams()` / `parseParamTail()` | `List<Param>` / preenche a lista recebida | Nunca devolvem `null`: em erro devolvem a lista até ali. |
| `parseParam()` | `Param` | |
| `parseType()` | `TypeKind` | Converte `KW_INT` em `INT` etc.; `null` em erro. |
| `parseBlock()` | `Block` | |
| `parseStmtList()` | preenche `List<Stmt>` | |
| `parseStmt()` | `Stmt` | No ramo de tipo, captura o token inicial antes de `parseType()`. |
| `parseIdStmt(Token id)` | `Stmt` (`Assign` ou `CallStmt`) | Recebe o `ID` que `parseStmt` já consumiu. |
| `parseIfStmt()` | `If` | |
| `parseElsePart()` / `parseElseBody()` | `Stmt` | `null` no `ε`; `Block` ou `If` no `else`. |
| `parseReturnVal()` | `Expr` | `null` no `ε`. |
| `parseExpr()` ... `parseMulExpr()` | `Expr` | |
| `parseOrTail(Expr left)` ... `parseMulTail(Expr left)` | `Expr` | Recebem o operando esquerdo e devolvem a árvore maior. |
| `parseUnary()` | `Expr` | `Unary` se houver `!` ou `-`. |
| `parsePrimary()` | `Expr` | Literal, `Id`, `Call` ou a `Expr` interna dos parênteses. |
| `parsePrimaryRest(Token id)` | `Expr` | `ε` devolve `Id`; `( Args )` devolve `Call`. |
| `parseArgs()` / `parseArgTail()` | `List<Expr>` / preenche a lista | |

O operador de cada `Bin` e `Unary` é convertido do `TokenType` para o enum `Op` por um `switch` sobre o **token** (por exemplo `PLUS` → `ADD`), o que é permitido: a proibição de `switch` é só sobre o tipo do **nó**.

### 6.2 Resultado do `parse()` e erros

```java
public record ParseResult(Program tree, List<String> errors, List<LexicalError> lexicalErrors) {
  public boolean isValid() { return tree != null; }
}
```

- `tree` é `null` quando há **qualquer** erro, sintático ou léxico. Os erros léxicos entram porque o `Lexer` descarta o trecho inválido e segue (`@`, `&` isolado, número colado em letra etc.), então o parser pode ver uma sequência de tokens aparentemente correta e produzir uma árvore que não corresponde ao fonte. Hoje o `Main` já trata os dois tipos de erro juntos para decidir entre ACEITO e REJEITADO; a nova versão decide por `result.isValid()`. Isso substitui também o `parseComplete()`.
- Sem `NullPointerException` por causa dos nós: os ramos de falha devolvem `null` (ou um nó parcial); os construtores dos nós não desreferenciam os argumentos; os métodos de cauda repassam `left` sem usá-lo; as listas nunca são `null`; e `match()` devolve o token atual mesmo quando falha, então nenhum `Token` é `null` (por exemplo, o nome de um `Id` pode ficar com um lexema errado depois de um erro, o que é inofensivo porque a árvore é descartada).
- Sem laço infinito por causa dos nós: a construção da árvore não muda nenhum caminho de consumo de tokens; cada `advance()` e cada `synchronize()` continuam exatamente onde estavam no CP2.

---

## 7. Formato do impressor da árvore

Visitor `AstPrinter implements Visitor<Void>`. Regras:

- Um nó por linha. A indentação é de **2 espaços por nível de profundidade** (a raiz fica sem indentação).
- O formato da linha é `Nome` ou `Nome(atributos)`, conforme a tabela abaixo.
- Os filhos aparecem logo abaixo, na ordem dos atributos da Seção 1 (da esquerda para a direita). Atributos `null` e listas vazias não geram linha.
- Posição **não** é impressa (os testes ficam estáveis a mudanças de espaçamento no fonte).
- Cada linha termina com `\n`, inclusive a última.

| Nó | Linha | Filhos, em ordem |
| :--- | :--- | :--- |
| `Program` | `Program` | cada declaração |
| `VarDecl` | `VarDecl(int total)` (tipo e nome) | `init`, se houver |
| `FuncDecl` | `FuncDecl(int soma)` (tipo de retorno e nome) | cada `Param`, depois o `Block` |
| `Param` | `Param(int n)` | nenhum |
| `Block` | `Block` | cada comando |
| `Assign` | `Assign` | `Id` alvo, valor |
| `CallStmt` | `CallStmt` | o `Call` |
| `If` | `If` | condição, bloco do then, e o else (`Block` ou `If`) se houver |
| `While` | `While` | condição, bloco |
| `Return` | `Return` | valor, se houver |
| `IntLit` / `DoubleLit` | `IntLit(10)` / `DoubleLit(3.14)` (lexema) | nenhum |
| `BoolLit` | `BoolLit(True)` / `BoolLit(False)` | nenhum |
| `CharLit` | `CharLit('a')` (lexema com aspas, como escrito: `'\n'`, `'\''`) | nenhum |
| `StringLit` | `StringLit("ok")` (lexema com aspas, como escrito: `"a\nb"`, `"\""`) | nenhum |
| `Id` | `Id(x)` | nenhum |
| `Bin` | `Bin(+)` (símbolo do operador) | esquerda, direita |
| `Unary` | `Unary(-)` ou `Unary(!)` | operando |
| `Call` | `Call` | `Id` do callee, depois cada argumento |

Como `CharLit` e `StringLit` guardam o lexema como escrito (o `\n` do fonte continua sendo barra e `n`, e o `Lexer` não aceita quebra de linha real dentro de string ou char), nenhum valor quebra a regra de uma linha por nó. A posição dos filhos é fixa, então `If` com e sem `else` e `Return` com e sem valor são distinguíveis pelo número de filhos.

Exemplo, `a - b - c * d`:

```
Bin(-)
  Bin(-)
    Id(a)
    Id(b)
  Bin(*)
    Id(c)
    Id(d)
```

---

## 8. Formato do reimpressor de código-fonte

Visitor `SourcePrinter implements Visitor<String>`. Forma canônica:

- **Toda `Bin` e toda `Unary` é impressa entre parênteses**, mesmo a de nível mais alto: `a - b - c` vira `((a - b) - c)`; `-x` vira `(-x)`. Como o parser descarta parênteses, o texto relido gera a mesma árvore.
- Um comando por linha, **4 espaços por nível de indentação**; `{` na mesma linha do cabeçalho e `}` sozinho na linha.
- `else` e `else if` ficam na linha do `}` anterior: `} else {` e `} else if (...) {`.
- Literais: `IntLit`, `DoubleLit`, `CharLit` e `StringLit` com o lexema original, sem reprocessar escapes (zeros à esquerda ficam como estão: `007` continua `007`); `True` e `False` como no fonte.
- Declarações de topo separadas por uma linha em branco.

Exemplo: `s = s + i * 2;` sai como `s = (s + (i * 2));`.

---

## 9. Adicionar o segundo visitor não altera os nós

O `Node` só tem o `accept`, e cada nó concreto só chama o método do visitor com o seu nome (`visitBin`, `visitIf`, `visitWhile`, ...). O impressor da árvore e o reimpressor são classes independentes que implementam `Visitor<T>`; escrever o segundo não exigiu mexer em nenhuma classe de nó, e nenhum dos dois usa `instanceof` nem `switch` sobre o tipo do nó.

---

## 10. Árvore desenhada à mão

Programa de exemplo:

```
int soma(int n) {
    int s = 0;
    int i = 1;
    while (i <= n) {
        s = s + i * 2;
        i = i + 1;
    }
    if (s > 100) {
        return 100;
    }
    return s;
}
```

AST (34 nós, no formato do impressor da Seção 7):

```
Program
  FuncDecl(int soma)
    Param(int n)
    Block
      VarDecl(int s)
        IntLit(0)
      VarDecl(int i)
        IntLit(1)
      While
        Bin(<=)
          Id(i)
          Id(n)
        Block
          Assign
            Id(s)
            Bin(+)
              Id(s)
              Bin(*)
                Id(i)
                IntLit(2)
          Assign
            Id(i)
            Bin(+)
              Id(i)
              IntLit(1)
      If
        Bin(>)
          Id(s)
          IntLit(100)
        Block
          Return
            IntLit(100)
      Return
        Id(s)
```

### O que a AST descartou da árvore de derivação

1. **A cadeia de precedência até um identificador.** Para o `n` de `i <= n`, a derivação desce `Expr -> AndExpr -> EqExpr -> RelExpr -> AddExpr -> MulExpr -> Unary -> Primary -> ID PrimaryRest`, com `OrTail`, `AndTail`, `EqTail`, `AddTail` e `MulTail` derivando `ε` pelo caminho: cerca de 15 nós de derivação. Na AST é um único `Id(n)`.
2. **As caudas de `s + i * 2`.** `AddExpr -> MulExpr AddTail`, `AddTail -> + MulExpr AddTail`, `MulExpr -> Unary MulTail`, `MulTail -> * Unary MulTail`, mais os `ε` finais: viram só `Bin(+)` com um `Bin(*)` dentro. A precedência está na forma da árvore.
3. **A lista de comandos do bloco.** `{ StmtList }` com `StmtList -> Stmt StmtList -> Stmt StmtList -> ... -> ε` é uma cadeia aninhada que cresce a cada comando; na AST é um `Block` com uma lista de filhos no mesmo nível.
4. **`DeclRest` e `Params`/`ParamTail`.** `Decl -> Type ID DeclRest` e `DeclRest -> ( Params ) Block`, com `Params -> Param ParamTail` e `ParamTail -> ε`, viram um `FuncDecl` com uma lista `[Param]`. Os parênteses somem.
5. **`ElsePart -> ε`** do `if` sem `else` não deixa nada na árvore (`elseBranch == null`), e `VarRest -> = Expr` de `int s = 0;` vira o atributo `init`; `=`, `;`, `(`, `)`, `{`, `}` também somem.

---

## 11. Precedência, associatividade e parênteses

Árvores esperadas (formato da Seção 7) para os testes da Seção 5 do enunciado:

`a + b * c`

```
Bin(+)
  Id(a)
  Bin(*)
    Id(b)
    Id(c)
```

`a - b - c`

```
Bin(-)
  Bin(-)
    Id(a)
    Id(b)
  Id(c)
```

`a - (b - c)`

```
Bin(-)
  Id(a)
  Bin(-)
    Id(b)
    Id(c)
```

`((a))` produz a mesma árvore de `a`: um único `Id(a)`, sem nenhum nó de parênteses, porque `Primary -> ( Expr )` devolve a expressão interna.
