# CP5 — Verificador de tipos

## Como os tipos estão representados

- `language.symbols.Type` é um enum: `INT`, `DOUBLE`, `BOOL`, `CHAR`, `STRING`, `VOID` e `ERROR`.
- `ERROR` é o tipo de erro: compatível com tudo e nunca gera mensagem nova (sem cascata).
- `Type.of(TypeKind)` converte o tipo escrito no programa para o tipo do verificador.
- `Type.isAssignableTo` implementa a relação `T1 ≤ T2` do papel (mesmo tipo, ou `int → double`).
- Cada expressão guarda o seu tipo no nó (`Expr.getType()` / `setType`). Essa é a única mudança na AST do CP4.
- Cada `Symbol` guarda o tipo declarado. Para função, `type` é o tipo de retorno e `paramTypes` é a lista dos tipos dos parâmetros (procedimento = retorno `VOID`). Quem preenche é `IdentificationPass`, ao declarar o nome.

## Onde ficam as regras (`src/language/visitor/TypeChecker.java`)

| Regra do papel | Onde |
|---|---|
| `[LIT-INT]` `[LIT-DBL]` `[LIT-BOOL]` `[LIT-CHAR]` `[LIT-STR]` | `visitIntLit`, `visitDoubleLit`, `visitBoolLit`, `visitCharLit`, `visitStringLit` |
| `[ID]` `[ID-FUNC]` `[ID-NULO]` | `visitId` |
| `[ARIT-INT]` `[ARIT-DBL]` `[REL]` `[EQ-IGUAL]` `[EQ-NUM]` `[LOG]` | `visitBin`, tabela em `binaryResult` |
| `[NOT]` `[NEG-INT]` `[NEG-DBL]` | `visitUnary` |
| `[ASSIGN]` | `visitAssign` |
| `[VAR-INIT]` | `visitVarDecl` |
| `[IF]` `[WHILE]` `[BLOCK]` | `visitIf`, `visitWhile`, `visitBlock` |
| `[RET-VAL]` `[RET-VOID]` | `visitReturn` |
| `[CALL]` | `checkCall` (usado por `visitCall`) |
| `[CALL-STMT]` | `visitCallStmt` |

Os comentários `[NOME-DA-REGRA]` no código usam exatamente os nomes do papel.

## Onde ficam as decisões

| # | Decisão | Onde |
|---|---|---|
| 1 | `int → double` implícito, inverso é erro | `Type.isAssignableTo`; `[ARIT-DBL]`, `[EQ-NUM]` em `binaryResult` |
| 2 | `char` só com `char` | `binaryResult`: `char` não entra em nenhuma regra aritmética ou relacional |
| 3 | só `bool` é condição; `bool` fora da aritmética | `visitIf`, `visitWhile`, `binaryResult` |
| 4 | `string` sem operadores | `binaryResult`: nenhum caso aceita `STRING` |
| 5 | `==` só entre tipos iguais ou `int` com `double` | `binaryResult`, casos `EQ`/`NE` |
| 6 | função como comando descarta o valor | `visitCallStmt` (aceita) e `visitCall` (recusa `void` em expressão) |
| 7 | recursão mútua proibida | `IdentificationPass.visitFuncDecl` (a função só existe a partir da sua declaração) |
| 8 | argumentos com tipo exato | `checkCall` |
| 9 | `return` sem análise de fluxo | `visitReturn` |
| 10 | sem sobrecarga | `IdentificationPass` (redeclaração no espaço de nomes único) |
| 11 | nome de função sem parênteses é erro | `visitId` (`[ID-FUNC]`) |

## Mensagens

Cada erro traz `linha L:` e o operador, o nome e os tipos envolvidos. Os erros são acumulados, e a verificação continua depois de cada um. Depois do primeiro erro numa expressão, o tipo `ERROR` sobe sem novas mensagens. Exemplos:

- `linha 6: condição de 'if' deve ser bool, mas é int`
- `linha 9: operador '+' não se aplica a bool e int`
- `linha 13: 'f' espera 1 argumento(s), recebeu 2`
- `linha 14: argumento 1 de 'f': esperado int, veio double`

## Avisos

Variável local declarada sem valor e lida antes de qualquer atribuição gera `linha L: aviso: 'x' pode ser usado sem valor`. A análise é conservadora: atribuição dentro de `if` só vale depois dele se os dois ramos atribuem, e o corpo de `while` não conta. Avisos não impedem a compilação.

## Limitações

- A AST não tem comandos `print`/`read`, nem `break`, nem função declarada dentro de bloco, então o verificador não os trata.
- Com erro de escopo ou de tipo, a `Main` encerra com código 1 e não segue para a geração de código.

## Como rodar

Compilar: `javac -d out $(find src -name '*.java')`
Verificar um programa: `java -cp out language.Main <arquivo.txt> --usos`
