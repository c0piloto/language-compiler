# Especificação Sintática

Construção de Compiladores · Checkpoint 2

Notação: `ε` = vazio; `$` = fim de entrada; terminais em minúsculas/símbolos ou MAIÚSCULAS (`ID`, `NUM`, `STR_LIT`, `CHAR_LIT`). Os terminais são os tokens do CP1: 12 palavras reservadas, 14 operadores, 6 delimitadores e os literais. Em cada regra, `|` separa produções distintas.

Decisões: atribuição é comando (não expressão); `-` unário é tratado aqui no parser (o léxico só tem o operador `-`); `void` está em `Type` e a restrição de uso é semântica; corpo de `if`/`else`/`while` é sempre `Block`.

## 1. Antes e depois

Versão inicial (trechos relevantes):

```
Decl   -> Type ID ( Params ) Block | Type ID ; | Type ID = Expr ;
Stmt   -> ID = Expr ; | ID ( Args ) ; | if ( Expr ) Stmt
        | if ( Expr ) Stmt else Stmt | return ; | return Expr ; | ...
Expr   -> Expr || AndExpr | AndExpr          (idem nos 6 níveis)
Primary-> ID | ID ( Args ) | ...
Params -> Param | Param , Params | ε         (idem Args)
```

| # | Problema | Correção |
| :--- | :--- | :--- |
| 1 | Recursão à esquerda direta nos 6 níveis de expressão | `A -> A α \| β` vira `A -> β A'`, `A' -> α A' \| ε` (`OrTail`, `AndTail`, ...) |
| 2 | Prefixo comum `Type ID` (`Decl`, `Stmt`) | `DeclRest`, `VarRest` |
| 3 | Prefixo comum `ID` (`Stmt`, `Primary`) | `IdStmt`, `PrimaryRest` |
| 4 | Prefixos comuns em `if`, `return`, `Params`, `Args` | `ElsePart`, `ReturnVal`, `ParamTail`, `ArgTail` |
| 5 | Dangling else: após fatorar, `ElsePart -> else Stmt \| ε` viola (c) | Seção 4 |

Não havia recursão à esquerda indireta.

## 2. Gramática final

```
Program     -> Decl Program | ε
Decl        -> Type ID DeclRest
DeclRest    -> ( Params ) Block | VarRest ;
VarRest     -> = Expr | ε
Params      -> Param ParamTail | ε
ParamTail   -> , Param ParamTail | ε
Param       -> Type ID
Type        -> int | double | bool | char | string | void

Block       -> { StmtList }
StmtList    -> Stmt StmtList | ε
Stmt        -> Type ID VarRest ; | ID IdStmt | IfStmt | Block
             | while ( Expr ) Block | return ReturnVal ;
IdStmt      -> = Expr ; | ( Args ) ;
IfStmt      -> if ( Expr ) Block ElsePart
ElsePart    -> else ElseBody | ε
ElseBody    -> Block | IfStmt
ReturnVal   -> Expr | ε

Expr        -> AndExpr OrTail
OrTail      -> || AndExpr OrTail | ε
AndExpr     -> EqExpr AndTail
AndTail     -> && EqExpr AndTail | ε
EqExpr      -> RelExpr EqTail
EqTail      -> == RelExpr EqTail | != RelExpr EqTail | ε
RelExpr     -> AddExpr RelTail
RelTail     -> < AddExpr RelTail | <= AddExpr RelTail
             | > AddExpr RelTail | >= AddExpr RelTail | ε
AddExpr     -> MulExpr AddTail
AddTail     -> + MulExpr AddTail | - MulExpr AddTail | ε
MulExpr     -> Unary MulTail
MulTail     -> * Unary MulTail | / Unary MulTail | ε
Unary       -> ! Unary | - Unary | Primary
Primary     -> NUM | STR_LIT | CHAR_LIT | True | False | ( Expr ) | ID PrimaryRest
PrimaryRest -> ( Args ) | ε
Args        -> Expr ArgTail | ε
ArgTail     -> , Expr ArgTail | ε
```

Início: `Program`.

## 3. FIRST e FOLLOW

Para não repetir os mesmos conjuntos de terminais na tabela, usam-se as abreviações abaixo.

**Conjuntos básicos**

| Abrev. | O que representa | Conteúdo |
| :--- | :--- | :--- |
| `T` | Os tipos básicos; é o que pode **começar** uma declaração | `{int, double, bool, char, string, void}` |
| `E` | O que pode **começar uma expressão** (inclui os unários `!` e `-`) | `{NUM, STR_LIT, CHAR_LIT, True, False, (, ID, !, -}` |
| `S` | O que pode **começar um comando** (tipo = declaração local; `ID` = atribuição ou chamada; `{` = bloco) | `T ∪ {if, while, return, {, ID}` |
| `F` | O que pode **vir depois de um comando**: o início de outro comando ou o `}` que fecha o bloco. Não contém `else`, o que resolve o dangling else (Seção 4) | `S ∪ {}}` |
| `X` | O que pode **vir depois de uma expressão completa**: `;` (atribuição, declaração, `return`), `)` (`if`, `while`, parênteses, chamada) e `,` (entre argumentos) | `{;, ), ,}` |

**Conjuntos de precedência (`L1` a `L6`)**

São os FOLLOW dos níveis de expressão. Cada nível herda o conjunto do nível acima e soma o seu próprio operador, porque depois de uma sub-expressão pode vir o operador do nível seguinte ou qualquer coisa que siga o nível atual. Assim, `L6` contém todos os operadores binários mais `X`.

| Abrev. | Definição | Operador somado | É o FOLLOW de |
| :--- | :--- | :--- | :--- |
| `L1` | `{\|\|} ∪ X` | `\|\|` | `AndExpr`, `AndTail` |
| `L2` | `{&&} ∪ L1` | `&&` | `EqExpr`, `EqTail` |
| `L3` | `{==, !=} ∪ L2` | `==`, `!=` | `RelExpr`, `RelTail` |
| `L4` | `{<, <=, >, >=} ∪ L3` | `<`, `<=`, `>`, `>=` | `AddExpr`, `AddTail` |
| `L5` | `{+, -} ∪ L4` | `+`, `-` | `MulExpr`, `MulTail` |
| `L6` | `{*, /} ∪ L5` | `*`, `/` | `Unary`, `Primary`, `PrimaryRest` |

| Não-terminal | FIRST | FOLLOW |
| :--- | :--- | :--- |
| `Program` | `T ∪ {ε}` | `{$}` |
| `Decl` | `T` | `T ∪ {$}` |
| `DeclRest` | `{(, =, ;}` | `T ∪ {$}` |
| `VarRest` | `{=, ε}` | `{;}` |
| `Params` | `T ∪ {ε}` | `{)}` |
| `ParamTail` | `{",", ε}` | `{)}` |
| `Param` | `T` | `{",", )}` |
| `Type` | `T` | `{ID}` |
| `Block` | `{ { }` | `F ∪ {$, else}` |
| `StmtList` | `S ∪ {ε}` | `{}}` |
| `Stmt` | `S` | `F` |
| `IdStmt` | `{=, (}` | `F` |
| `IfStmt` | `{if}` | `F` |
| `ElsePart` | `{else, ε}` | `F` |
| `ElseBody` | `{{, if}` | `F` |
| `ReturnVal` | `E ∪ {ε}` | `{;}` |
| `Expr` / `OrTail` | `E` / `{\|\|, ε}` | `X` |
| `AndExpr` / `AndTail` | `E` / `{&&, ε}` | `L1` |
| `EqExpr` / `EqTail` | `E` / `{==, !=, ε}` | `L2` |
| `RelExpr` / `RelTail` | `E` / `{<, <=, >, >=, ε}` | `L3` |
| `AddExpr` / `AddTail` | `E` / `{+, -, ε}` | `L4` |
| `MulExpr` / `MulTail` | `E` / `{*, /, ε}` | `L5` |
| `Unary` | `E` | `L6` |
| `Primary` | `E \ {!, -}` | `L6` |
| `PrimaryRest` | `{(, ε}` | `L6` |
| `Args` | `E ∪ {ε}` | `{)}` |
| `ArgTail` | `{",", ε}` | `{)}` |

Pontos-chave: `else ∈ FOLLOW(Block)` (por `IfStmt`), mas `else ∉ FOLLOW(Stmt)` porque o corpo de `if`/`while` é `Block`. `FOLLOW(Expr)` = `X` vem de `;` (`VarRest`, `IdStmt`, `ReturnVal`), `)` (`if`, `while`, parênteses, `Args`) e `,` (`ArgTail`).

## 4. Verificação LL(1)

(a) FIRST das alternativas disjuntos · (b) no máximo uma alternativa anulável · (c) com alternativa anulável, FIRST das demais ∩ FOLLOW = ∅.

Não-terminais com uma única produção (`Decl`, `Param`, `Block`, `IfStmt`, `Expr`, `AndExpr`, `EqExpr`, `RelExpr`, `AddExpr`, `MulExpr`) cumprem (a), (b) e (c) trivialmente. Os demais:

| Não-terminal | Alternativas (FIRST) | (a) | (b) | (c) |
| :--- | :--- | :--- | :--- | :--- |
| `Program` | `Decl Program` (`T`) · `ε` | ✔ | ✔ | `T ∩ {$} = ∅` ✔ |
| `DeclRest` | `( Params ) Block` (`{(}`) · `VarRest ;` (`{=, ;}`) | ✔ | ✔ nenhuma | — |
| `VarRest` | `= Expr` (`{=}`) · `ε` | ✔ | ✔ | `{=} ∩ {;} = ∅` ✔ |
| `Params` | `Param ParamTail` (`T`) · `ε` | ✔ | ✔ | `T ∩ {)} = ∅` ✔ |
| `ParamTail` | `, ...` (`{,}`) · `ε` | ✔ | ✔ | `{,} ∩ {)} = ∅` ✔ |
| `Type` | 6 terminais distintos | ✔ | ✔ nenhuma | — |
| `StmtList` | `Stmt StmtList` (`S`) · `ε` | ✔ | ✔ | `S ∩ {}} = ∅` ✔ |
| `Stmt` | `Type...` (`T`) · `ID IdStmt` · `IfStmt` (`if`) · `Block` (`{`) · `while` · `return` | ✔ | ✔ nenhuma | — |
| `IdStmt` | `= Expr ;` (`{=}`) · `( Args ) ;` (`{(}`) | ✔ | ✔ nenhuma | — |
| `ElsePart` | `else ElseBody` (`{else}`) · `ε` | ✔ | ✔ | `{else} ∩ F = ∅` ✔ |
| `ElseBody` | `Block` (`{{}`) · `IfStmt` (`{if}`) | ✔ | ✔ nenhuma | — |
| `ReturnVal` | `Expr` (`E`) · `ε` | ✔ | ✔ | `E ∩ {;} = ∅` ✔ |
| `OrTail` | `\|\| ...` · `ε` | ✔ | ✔ | `{\|\|} ∩ X = ∅` ✔ |
| `AndTail` | `&& ...` · `ε` | ✔ | ✔ | `{&&} ∩ L1 = ∅` ✔ |
| `EqTail` | `== ...` · `!= ...` · `ε` | ✔ | ✔ | `{==, !=} ∩ L2 = ∅` ✔ |
| `RelTail` | `<` · `<=` · `>` · `>=` · `ε` | ✔ | ✔ | `{<, <=, >, >=} ∩ L3 = ∅` ✔ |
| `AddTail` | `+ ...` · `- ...` · `ε` | ✔ | ✔ | `{+, -} ∩ L4 = ∅` ✔ |
| `MulTail` | `* ...` · `/ ...` · `ε` | ✔ | ✔ | `{*, /} ∩ L5 = ∅` ✔ |
| `Unary` | `!` · `-` · `Primary` (`E \ {!, -}`) | ✔ | ✔ nenhuma | — |
| `Primary` | `NUM` · `STR_LIT` · `CHAR_LIT` · `True` · `False` · `(` · `ID` | ✔ | ✔ nenhuma | — |
| `PrimaryRest` | `( Args )` (`{(}`) · `ε` | ✔ | ✔ | `{(} ∩ L6 = ∅` ✔ |
| `Args` | `Expr ArgTail` (`E`) · `ε` | ✔ | ✔ | `E ∩ {)} = ∅` ✔ |
| `ArgTail` | `, ...` (`{,}`) · `ε` | ✔ | ✔ | `{,} ∩ {)} = ∅` ✔ |

**Resultado: nenhuma violação; a gramática final é LL(1).**

### Violação encontrada: dangling else

Com `IfStmt -> if ( Expr ) Stmt ElsePart` e `ElsePart -> else Stmt | ε`, o `Stmt` do corpo pode ser outro `if`, então `else ∈ FOLLOW(ElsePart)` e a cláusula (c) falha:

```
FIRST(else Stmt) ∩ FOLLOW(ElsePart) = {else} ≠ ∅
```

A tabela LL(1) teria duas entradas em `M[ElsePart, else]`. **Correção:** o corpo de `if`/`else`/`while` passa a ser `Block`. Como o bloco sempre termina em `}`, `else` sai de `FOLLOW(Stmt)`, e `ElseBody -> Block | IfStmt` preserva o `else if`.
