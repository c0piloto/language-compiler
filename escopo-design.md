# Checkpoint 4 — Tabela de Símbolos e Passada de Identificação

**Grupo:** <nome-do-grupo>
**Integrantes:** Moisés, João Pedro Teixeira

Este documento foi escrito antes da implementação. O desenho da Seção 1 foi feito à mão e a saída da passada é conferida contra ele.

---

## 1. Ambiente em cada ponto de um programa

### 1.1 Programa de referência

O programa tem dois níveis de blocos aninhados dentro de uma função com parâmetro, um nome sombreado (`x`) e um uso depois do fechamento do bloco (linha 11). É o arquivo `test/programas/valido_sombreamento.txt`.

```
 1  int x = 10;
 2  void f(int a) {
 3      int y = a + x;
 4      {
 5          int x = y;
 6          {
 7              int z = x + a;
 8          }
 9          x = x + y;
10      }
11      y = x;
12  }
13  void main() {
14      f(x);
15  }
```

### 1.2 Ambiente depois de cada declaração e de cada fechamento

Notação: `N<k>` é o escopo de nível `k` e `nome@L` é o símbolo declarado na linha `L`. O escopo mais à direita é o topo da pilha.

| Após a linha | Evento | Ambiente (global → topo) |
|---|---|---|
| 1 | declara `x` | `N0{x@1}` |
| 2 | declara `f` em N0, abre escopo, declara o parâmetro `a` | `N0{x@1, f@2}` `N1{a@2}` |
| 3 | declara `y` | `N0{x@1, f@2}` `N1{a@2, y@3}` |
| 4 | abre bloco | `N0{x@1, f@2}` `N1{a@2, y@3}` `N2{}` |
| 5 | declara `x` (sombreia `x@1`) | `N0{x@1, f@2}` `N1{a@2, y@3}` `N2{x@5}` |
| 6 | abre bloco | `... N2{x@5}` `N3{}` |
| 7 | declara `z` | `... N2{x@5}` `N3{z@7}` |
| 8 | fecha bloco (`z@7` some) | `N0{x@1, f@2}` `N1{a@2, y@3}` `N2{x@5}` |
| 10 | fecha bloco (`x@5` some) | `N0{x@1, f@2}` `N1{a@2, y@3}` |
| 12 | fecha o escopo da função (`a@2` e `y@3` somem) | `N0{x@1, f@2}` |
| 13 | declara `main` em N0, abre escopo | `N0{x@1, f@2, main@13}` `N1{}` |
| 15 | fecha o escopo de `main` | `N0{x@1, f@2, main@13}` |

### 1.3 Resolução de cada uso

| Linha | Uso | Resolve para | Observação |
|---|---|---|---|
| 3 | `a` | `a@2` | parâmetro, escopo N1 |
| 3 | `x` | `x@1` | ainda não existe `x@5` |
| 5 | `y` | `y@3` | o inicializador usa o `y` do escopo externo |
| 7 | `x` | `x@5` | o `x` interno oculta o global |
| 7 | `a` | `a@2` | sobe dois níveis |
| 9 | `x` (alvo da atribuição) | `x@5` | sombreado |
| 9 | `x` (operando esquerdo) | `x@5` | sombreado |
| 9 | `y` | `y@3` | |
| 11 | `y` | `y@3` | |
| 11 | `x` | `x@1` | uso depois do fechamento do bloco: `x@5` não existe mais, volta o global |
| 14 | `f` | `f@2` | |
| 14 | `x` | `x@1` | |

Saída esperada da passada: nenhum erro e todos os usos resolvidos como na tabela acima. A ordem das linhas é a ordem em que a passada visita os nós (ver Seção 5).

---

## 2. O que o `Symbol` guarda e por quê

| Campo | Tipo | Quem pergunta por ele |
|---|---|---|
| `name` | `String` | a tabela, como chave de `declare`/`lookup`; as mensagens de erro |
| `category` | `enum Category` | CP5, para saber se o nome pode ser chamado, atribuído ou usado como valor (não se chama variável, não se atribui a procedimento) |
| `declaration` | `Node` (o nó da AST da declaração) | a passada, para pegar a linha da primeira declaração na mensagem de redeclaração (`line()`); CP5, para chegar nos filhos da declaração |
| `level` | `int` | a passada (o símbolo é criado com `table.level()`); o gerador de código, para distinguir global de local |
| `type` | `Type` (nasce `null`) | reservado para o CP5 (Aula 12); o `getType()` fica comentado até lá |
| `displace` | `int` (nasce `-1`) | reservado para a geração de código (Aulas 13 a 19) |

Os campos `type` e `displace` já existem na classe, com valores neutros. Nas próximas etapas eles só são preenchidos, então a forma das classes e dos construtores do CP4 não muda. Não acrescentamos nenhum campo específico da linguagem. A categoria diz de que tipo de nome se trata (variável, parâmetro, procedimento ou função). A AST atual não tem nó nem marca de constante (o `VarDecl` não diferencia), então nesta etapa toda declaração de variável entra como `VARIABLE`. Se a linguagem ganhar `const`, a diferença fica na categoria e o valor fica no nó apontado por `declaration`.

O nó de identificador de uso (`Id`) ganha o campo `decl` (do tipo `Symbol`), com `getDecl()` e `setDecl()`. Ele nasce `null` e a passada o preenche. É a única mudança na hierarquia da AST do CP3. O CP5 vai ler o tipo por `i.getDecl().getType()` na árvore decorada.

---

## 3. Escolha da implementação da tabela

**Escolha: pilha de mapas** (`ArrayDeque<Map<String, Symbol>>`, com um `HashMap` por escopo).

**Motivo:**
- `openScope` empilha um mapa novo e `closeScope` desempilha. Tudo o que foi declarado no bloco some junto, sem nenhum código de limpeza.
- `lookupLocal` é um `get` no mapa do topo, e `lookup` percorre a pilha do topo para a base.
- `level()` é `pilha.size() - 1`. A tabela já nasce com o escopo global aberto (nível 0).

**Alternativas descartadas:**
- *Escopos encadeados:* funciona, mas exige uma classe `Escopo` com ponteiro para o pai e uma variável "escopo atual", ou seja, mais código para o mesmo resultado.
- *Tabela única com registro:* é mais eficiente na busca, mas o `closeScope` precisa desfazer as entradas uma a uma, e é o desenho mais fácil de errar (sombreamento, restauração do símbolo externo). Não compensa para o tamanho dos nossos programas.

**Contrato:**
- `declare(s)`: se o nome já existe no escopo do topo, devolve `false` e não altera nada (a primeira declaração fica). Caso contrário, grava e devolve `true`.
- `lookup` e `lookupLocal`: devolvem `null` quando não acham e nunca lançam exceção.
- `closeScope` com só o escopo global aberto: lança `IllegalStateException` com mensagem explicativa. Isso indica bug do chamador, não erro do programa-fonte. O Visitor sempre abre e fecha em pares, então nunca chega a acontecer. Preferimos falhar com mensagem clara a engolir o erro em silêncio.
- O Visitor só enxerga a interface `SymbolTable`. A classe concreta (`StackSymbolTable`) aparece uma única vez, na linha que cria a tabela.

---

## 4. As cinco decisões de projeto

Todos os programas de teste ficam em `test/programas/NOME.txt`, e a saída esperada de cada um, escrita à mão, em `test/esperado/NOME.txt`. Os números de linha abaixo são os dos arquivos.

### 4.1 Sombreamento

- **Escolha:** um nome interno **pode** ocultar um externo. Redeclarar o mesmo nome **no mesmo bloco** é sempre erro.
- **Descartada:** proibir sombreamento (erro se o nome já existe em qualquer escopo externo).
- **Motivo:** é o comportamento de C e Java para variáveis locais, e é exatamente o que `declare` + `lookupLocal` implementam: a duplicata só é detectada no escopo do topo. Proibir sombreamento obrigaria a usar `lookup` na declaração e faria blocos internos dependerem de nomes de fora que o programador nem precisa conhecer.
- **Testes:** `d1_sombreamento.txt` e `d1_redeclaracao.txt`.

`d1_sombreamento.txt`:
```
1  void main() {
2      int a = 1;
3      {
4          int a = 2;
5          a = 3;
6      }
7      a = 4;
8  }
```
Esperado: nenhum erro. `a` da linha 5 → `a@4`; `a` da linha 7 → `a@2`.

`d1_redeclaracao.txt`:
```
1  int b = 1;
2  int b = 2;
3  void main() {
4      b = 3;
5  }
```
Esperado: `linha 2: 'b' já declarado neste escopo (linha 1)`. O `b` da linha 4 resolve para `b@1`, e a segunda declaração é descartada.

### 4.2 Declarar antes de usar

- **Escolha:** o uso precisa vir **depois** da declaração. Não há passada prévia. Em `int d = d + 1;` o inicializador é visitado **antes** de declarar `d`, então o `d` do inicializador ainda não existe.
- **Descartada:** passada prévia que declara todos os nomes do bloco antes de visitar os usos.
- **Motivo:** a passada única é a mais simples e deixa a ordem das declarações coincidir com a ordem do texto, o que também ordena as mensagens de erro. A passada prévia só se justificaria para funções mutuamente recursivas (ver 4.3), e decidimos não permiti-las.
- **Teste:** `d2_declarar_antes.txt`.

```
1  void main() {
2      c = 1;
3      int c = 0;
4      int d = d + 1;
5  }
```
Esperado:
```
linha 2: 'c' não declarado
linha 4: 'd' não declarado
```

### 4.3 Funções mutuamente recursivas

- **Escolha:** **não permitidas.** A recursão direta funciona, porque o nome da função é declarado no escopo externo antes do `openScope` (ver 4.4 e Seção 5).
- **Descartada:** permitir via passada prévia que declara os nomes de todas as funções antes de visitar seus corpos (ou via declaração antecipada na linguagem).
- **Motivo:** a linguagem não tem declaração antecipada, e a passada prévia adicionaria uma segunda travessia e o cuidado de não declarar o mesmo nome duas vezes. Para o escopo do trabalho, a recursão direta cobre o caso importante. Uma função só enxerga a si mesma e as que vieram antes dela no texto.
- **Teste:** `d3_mutua.txt`.

```
1  void par(int n) {
2      impar(n);
3  }
4  void impar(int n) {
5      par(n);
6  }
```
Esperado:
```
linha 2: 'impar' não declarado
linha 2: n@1
linha 5: par@1
linha 5: n@4
```
A chamada de `par` na linha 5 resolve para `par@1`, e a de `impar` na linha 2 falha.

### 4.4 Parâmetros e corpo

- **Escolha:** parâmetros e corpo ficam **no mesmo escopo**. Então `void f(int a) { int a; }` é redeclaração.
- **Descartada:** o corpo abrir um escopo próprio dentro do escopo dos parâmetros.
- **Motivo:** é o comportamento de C e evita a situação confusa de uma variável local ocultar um parâmetro sem aviso. Na implementação, a passada declara a função no escopo externo, faz `openScope`, declara os parâmetros e percorre os comandos do corpo **diretamente** (`getBody().getStmts()`), sem chamar o `visitBlock` do corpo, que abriria um segundo escopo. O `closeScope` vem depois do último comando.
- **Teste:** `d4_param_corpo.txt`.

```
1  void f(int a) {
2      int a = 0;
3      a = 1;
4  }
```
Esperado:
```
linha 2: 'a' já declarado neste escopo (linha 1)
linha 3: a@1
```

### 4.5 Espaços de nomes

- **Escolha:** variáveis e funções compartilham **um único** espaço de nomes, como em C.
- **Descartada:** um espaço para variáveis e outro para funções.
- **Motivo:** a tabela fica com um único mapa por escopo e o `Symbol` já carrega a categoria para distinguir os casos. Dois espaços exigiriam duas tabelas ou chaves compostas, sem ganho para a linguagem.
- **Teste:** `d5_namespace.txt`.

```
1  int f = 1;
2  void f(int a) {
3  }
```
Esperado: `linha 2: 'f' já declarado neste escopo (linha 1)`.

---

## 5. Como a passada de identificação trata cada nó

| Nó | Ação |
|---|---|
| `Program` | visita as declarações globais em ordem, sem `openScope` (ficam no nível 0) |
| `Block` | `openScope`, visita os filhos na ordem, `closeScope` |
| `VarDecl` | visita o inicializador (se houver) **antes** de declarar; monta o `Symbol` com `table.level()` e chama `declare` |
| `Param` | igual, com categoria `PARAMETER`, dentro do escopo novo da função |
| `FuncDecl` | declara o nome no escopo externo (`PROCEDURE` se o retorno é `void`, senão `FUNCTION`); `openScope`; declara os parâmetros; visita os comandos do corpo direto; `closeScope` |
| `Id` (uso) | `lookup`; se achou, grava em `decl`; se não, reporta `não declarado` e deixa `decl` nulo |
| `Assign`, `Call`, `CallStmt`, `If`, `While`, `Return`, `Bin`, `Unary` | só percorrem os filhos, na ordem do texto (o alvo da atribuição antes do valor; o nome chamado antes dos argumentos) |
| literais | nada a fazer |

Se `declare` devolve `false`, a passada reporta a redeclaração e mantém a primeira declaração. A segunda é descartada.

### Formato das mensagens de erro

Os erros são acumulados em uma `List<String>` e a passada continua depois de cada um. O formato é fixo:

```
linha <L>: '<nome>' já declarado neste escopo (linha <L0>)
linha <L>: '<nome>' não declarado
```

A ordem das mensagens é a ordem do texto do programa, porque a travessia é em ordem e não há passada prévia.

### Formato da saída usada nos testes

Para provar que cada uso resolve à declaração certa, a passada também guarda, para cada uso resolvido, uma linha no formato do enunciado:

```
linha <L>: <nome>@<L0>
```

onde `L` é a linha do uso e `L0` a da declaração. O arquivo esperado de cada teste traz primeiro os erros, na ordem do texto, e depois os usos resolvidos, na ordem em que a passada os visita. Um uso que não resolve não gera linha de uso, só o erro.

---

## 6. Plano de testes

### 6.1 A tabela, isolada

`SymbolTableTest` (classe `main`, com `AssertionError` em vez de `assert`, que depende da flag `-ea`):

- declarar e buscar um nome
- sombreamento: o interno oculta o externo e, depois do `closeScope`, o externo volta
- redeclaração: `declare` devolve `false` e o primeiro símbolo permanece
- `lookupLocal` não enxerga o escopo externo
- `lookup` de nome inexistente devolve `null`
- `level()` começa em 0, sobe com `openScope` e desce com `closeScope`

### 6.2 Programas obrigatórios

Saídas esperadas escritas à mão, antes de rodar a passada, em `test/esperado/`.

| Programa | Conteúdo | Esperado |
|---|---|---|
| `valido_sombreamento.txt` | o programa da Seção 1.1 | nenhum erro; os 12 usos da Seção 1.3 |
| `redeclaracao.txt` | `int a` repetido no mesmo bloco | `linha 3: 'a' já declarado neste escopo (linha 2)` e `linha 4: a@2` |
| `nao_declarado.txt` | uso de nome sem declaração | `linha 3: 'b' não declarado` e `linha 3: a@2` |

`redeclaracao.txt`:
```
1  void main() {
2      int a = 1;
3      int a = 2;
4      a = 3;
5  }
```

`nao_declarado.txt`:
```
1  void main() {
2      int a = 1;
3      a = b;
4  }
```

### 6.3 Um programa por decisão e casos extras

| Programa | O que cobre |
|---|---|
| `d1_sombreamento.txt`, `d1_redeclaracao.txt` | decisão 1 |
| `d2_declarar_antes.txt` | decisão 2 |
| `d3_mutua.txt` | decisão 3 |
| `d4_param_corpo.txt` | decisão 4 |
| `d5_namespace.txt` | decisão 5 |
| `tres_erros.txt` | três erros numa rodada, reportados na ordem do texto |
| `recursivo.txt` | procedimento recursivo: o nome resolve para a própria declaração, sem erro |
| `uso_apos_bloco.txt` | nome declarado dentro de um bloco e usado depois do `}` → `não declarado` |

`tres_erros.txt`:
```
1  void main() {
2      int a = 1;
3      int a = 2;
4      a = b;
5      {
6          int c = 0;
7      }
8      c = 1;
9  }
```
Esperado:
```
linha 3: 'a' já declarado neste escopo (linha 2)
linha 4: 'b' não declarado
linha 8: 'c' não declarado
linha 4: a@2
```

`recursivo.txt`:
```
1  void fat(int n) {
2      if (n > 0) {
3          fat(n - 1);
4      }
5  }
```
Esperado:
```
linha 2: n@1
linha 3: fat@1
linha 3: n@1
```

`uso_apos_bloco.txt`:
```
1  void main() {
2      {
3          int z = 1;
4      }
5      z = 2;
6  }
```
Esperado: `linha 5: 'z' não declarado`.

### 6.4 Regressão

A suíte do CP3 (`TestRunner`: árvores, precedência, ida e volta, programas inválidos e múltiplos erros) continua passando sem alterações. Os testes do CP4 rodam em um executor próprio (`IdentificationTestRunner`), e os dois são executados antes de cada entrega. Os nomes dos arquivos do CP4 não colidem com os do CP3 em `test/` e `test/esperado/`.

### 6.5 Robustez

A passada não lança `NullPointerException` nem entra em laço infinito em nenhuma entrada, válida ou inválida. Quando o uso não resolve, `decl` fica `null`, e quem lê `decl` (CP5) precisa checar isso. A passada só roda sobre AST válida: programas com erro sintático não chegam até ela.

---

## 7. Estrutura da entrega

```
cp4-<nome-do-grupo>/
    escopo-design.md
    log-uso-ia.md
    src/          (lexer, parser, ast/, visitor/, symbols/)
    test/
        esperado/   (saídas esperadas, escritas à mão)
        programas/  (os programas de teste)
```
