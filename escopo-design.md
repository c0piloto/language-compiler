# Checkpoint 4 — Tabela de Símbolos e Passada de Identificação

**Integrantes:** Moisés Marques de Oliveira, João Pedro Teixeira

Este documento foi escrito antes da implementação. O desenho da Seção 1 foi feito à mão e a saída da passada é conferida contra ele.

---

## 1. Ambiente em cada ponto de um programa

### 1.1 Programa de referência

O programa tem dois níveis de blocos aninhados dentro de uma função com parâmetro, um nome sombreado (`x`) e um uso depois do fechamento do bloco (linha 11).

```
 1  var x = 10;
 2  void f(int a) {
 3      var y = a + x;
 4      {
 5          var x = y;
 6          {
 7              var z = x + a;
 8          }
 9          x = x + y;
10      }
11      y = x;
12  }
13  f(x);
```

### 1.2 Ambiente depois de cada declaração e de cada fechamento

Notação: `N<k>` é o escopo de nível `k` e `nome@L` é o símbolo declarado na linha `L`. O escopo mais à direita é o topo da pilha.

| Após a linha | Evento | Ambiente (global → topo) |
|---|---|---|
| 1 | declara `x` | `N0{x@1}` |
| 2 | declara `f` em N0, abre escopo, declara parâmetro `a` | `N0{x@1, f@2}` `N1{a@2}` |
| 3 | declara `y` | `N0{x@1, f@2}` `N1{a@2, y@3}` |
| 4 | abre bloco | `N0{x@1, f@2}` `N1{a@2, y@3}` `N2{}` |
| 5 | declara `x` (sombreia `x@1`) | `N0{x@1, f@2}` `N1{a@2, y@3}` `N2{x@5}` |
| 6 | abre bloco | `... N2{x@5}` `N3{}` |
| 7 | declara `z` | `... N2{x@5}` `N3{z@7}` |
| 8 | fecha bloco (`z@7` some) | `N0{x@1, f@2}` `N1{a@2, y@3}` `N2{x@5}` |
| 10 | fecha bloco (`x@5` some) | `N0{x@1, f@2}` `N1{a@2, y@3}` |
| 12 | fecha o escopo da função (`a@2` e `y@3` somem) | `N0{x@1, f@2}` |

### 1.3 Resolução de cada uso

| Linha | Uso | Resolve para | Observação |
|---|---|---|---|
| 3 | `a` | `a@2` | parâmetro, escopo N1 |
| 3 | `x` | `x@1` | ainda não existe `x@5` |
| 5 | `y` | `y@3` | inicializador usa o `y` do escopo externo |
| 7 | `x` | `x@5` | o `x` interno oculta o global |
| 7 | `a` | `a@2` | sobe dois níveis |
| 9 | `x` (esquerda) | `x@5` | sombreado |
| 9 | `x` (direita) | `x@5` | sombreado |
| 9 | `y` | `y@3` | |
| 11 | `y` | `y@3` | |
| 11 | `x` | `x@1` | uso depois do fechamento do bloco: `x@5` não existe mais, volta o global |
| 13 | `f` | `f@2` | |
| 13 | `x` | `x@1` | |

Saída esperada da passada: nenhum erro e todos os usos resolvidos como na tabela acima.

---

## 2. O que o `Symbol` guarda e por quê

| Campo | Tipo | Quem pergunta por ele |
|---|---|---|
| `nome` | `String` | a tabela, como chave de `declare`/`lookup`; as mensagens de erro |
| `categoria` | `enum {VARIAVEL, CONSTANTE, PARAMETRO, PROCEDIMENTO, FUNCAO}` | CP5, para saber se o nome pode ser chamado, atribuído ou usado como valor (não se atribui a constante, não se chama variável) |
| `decl` | nó da AST da declaração | a passada, para pegar a linha da declaração original na mensagem de redeclaração; CP5, para chegar nos filhos da declaração |
| `nivel` | `int` | a passada (confere se o símbolo foi criado com `table.level()`); o gerador de código, para distinguir global de local |
| `tipo` | `Tipo` (nasce `null`) | reservado para o CP5 (Aula 12) |
| `deslocamento` | `int` (nasce `-1`) | reservado para a geração de código (Aulas 13 a 19) |

Os campos `tipo` e `deslocamento` já existem na classe, com valores neutros. Nas próximas etapas eles só são preenchidos, então as classes e os construtores do CP4 não mudam de forma. Também não acrescentamos nenhum campo específico da linguagem. A informação de `const` com valor e a de parâmetro por referência já estão no nó apontado por `decl`, e a categoria diz de qual caso se trata.

O nó de identificador de uso ganha o campo `decl` (do tipo `Symbol`). Ele nasce `null` e a passada o preenche. É a única mudança na hierarquia da AST do CP3. O CP5 vai ler `i.decl.tipo` na árvore decorada.

---

## 3. Escolha da implementação da tabela

**Escolha: pilha de mapas** (`ArrayDeque<Map<String, Symbol>>`, com um `HashMap` por escopo).

**Motivo:**
- `openScope` empilha um mapa novo e `closeScope` desempilha. Tudo o que foi declarado no bloco some junto, sem precisar de nenhum código de limpeza.
- `lookupLocal` é um `get` no mapa do topo, e `lookup` percorre a pilha do topo para a base.
- `level()` é `pilha.size() - 1`. A tabela já nasce com o escopo global aberto (nível 0).

**Alternativas descartadas:**
- *Escopos encadeados:* funciona, mas exige uma classe `Escopo` com ponteiro para o pai e uma variável "escopo atual", ou seja, mais código para o mesmo resultado.
- *Tabela única com registro:* é mais eficiente em tempo de busca, mas o `closeScope` precisa desfazer as entradas uma a uma e é o desenho mais fácil de errar (sombreamento, restauração do símbolo externo). Não compensa para o tamanho dos nossos programas.

**Contrato:**
- `declare(s)`: se o nome já existe no escopo do topo, devolve `false` e não altera nada (a primeira declaração fica). Caso contrário, grava e devolve `true`.
- `lookup` e `lookupLocal`: devolvem `null` quando não acham e nunca lançam exceção.
- `closeScope` com só o escopo global aberto: lança `IllegalStateException("closeScope sem escopo aberto")`. Isso indica bug do chamador, não erro do programa-fonte, e o Visitor sempre abre e fecha em pares, então nunca chega a acontecer. Preferimos falhar com mensagem clara a engolir o erro em silêncio.
- O Visitor só enxerga a interface `SymbolTable`. A classe concreta aparece uma única vez, na linha que cria a tabela.

---

## 4. As cinco decisões de projeto

### 4.1 Sombreamento

- **Escolha:** um nome interno **pode** ocultar um externo. Redeclarar o mesmo nome **no mesmo bloco** é sempre erro.
- **Descartada:** proibir sombreamento (erro se o nome já existe em qualquer escopo externo).
- **Motivo:** é o comportamento de C e Java para variáveis locais, e é exatamente o que `declare` + `lookupLocal` implementam: a duplicata só é detectada no escopo do topo. Proibir sombreamento obrigaria a usar `lookup` na declaração e faria blocos internos dependerem de nomes de fora que o programador nem precisa conhecer.
- **Teste:** `test/programas/d1_sombreamento.prog` e `d1_redeclaracao.prog`.

```
1  var a = 1;
2  {
3      var a = 2;
4      a = 3;
5  }
6  a = 4;
```
Esperado: nenhum erro. `a` da linha 4 → `a@3`; `a` da linha 6 → `a@1`.

```
1  var b = 1;
2  var b = 2;
3  b = 3;
```
Esperado: `linha 2: 'b' já declarado neste escopo (linha 1)`. O `b` da linha 3 resolve para `b@1` e a segunda declaração é descartada.

### 4.2 Declarar antes de usar

- **Escolha:** o uso precisa vir **depois** da declaração. Não há passada prévia. Em `var d = d + 1;` o inicializador é visitado **antes** de declarar `d`, então o `d` do inicializador ainda não existe.
- **Descartada:** passada prévia que declara todos os nomes do bloco antes de visitar os usos.
- **Motivo:** a passada única é a mais simples e deixa a ordem das declarações coincidir com a ordem do texto, o que também ordena as mensagens de erro. A passada prévia só se justifica se quiséssemos funções mutuamente recursivas (ver 4.3), e decidimos não permiti-las.
- **Teste:** `test/programas/d2_declarar_antes.prog`.

```
1  c = 1;
2  var c = 0;
3  var d = d + 1;
```
Esperado:
```
linha 1: 'c' não declarado
linha 3: 'd' não declarado
```

### 4.3 Funções mutuamente recursivas

- **Escolha:** **não permitidas.** A recursão direta funciona, porque o nome da função é declarado no escopo externo antes do `openScope` (ver 4.4 e Seção 5).
- **Descartada:** permitir via passada prévia que declara os nomes de todas as funções do bloco antes de visitar seus corpos (ou via declaração antecipada na linguagem).
- **Motivo:** a linguagem não tem declaração antecipada, e a passada prévia adicionaria uma segunda travessia e o cuidado de não declarar o mesmo nome duas vezes. Para o escopo do trabalho, a recursão direta cobre o caso importante. Uma função só enxerga a si mesma e as que vieram antes dela no texto.
- **Teste:** `test/programas/d3_mutua.prog`.

```
1  void par(int n) { impar(n); }
2  void impar(int n) { par(n); }
```
Esperado: `linha 1: 'impar' não declarado`. A chamada de `par` na linha 2 resolve para `par@1`.

### 4.4 Parâmetros e corpo

- **Escolha:** parâmetros e corpo ficam **no mesmo escopo**. Então `void f(int a) { int a; }` é redeclaração.
- **Descartada:** o corpo abrir um escopo próprio dentro do escopo dos parâmetros.
- **Motivo:** é o comportamento de C e evita a situação confusa de uma variável local ocultar um parâmetro sem aviso. Na implementação, a passada declara a função no escopo externo, faz `openScope`, declara os parâmetros e percorre os comandos do corpo **diretamente**, sem chamar o `visit` do bloco do corpo (que abriria um segundo escopo). O `closeScope` vem depois do último comando.
- **Teste:** `test/programas/d4_param_corpo.prog`.

```
1  void f(int a) {
2      var a = 0;
3  }
```
Esperado: `linha 2: 'a' já declarado neste escopo (linha 1)`.

### 4.5 Espaços de nomes

- **Escolha:** variáveis e funções compartilham **um único** espaço de nomes, como em C.
- **Descartada:** um espaço para variáveis e outro para funções.
- **Motivo:** a tabela fica com um único mapa por escopo e o `Symbol` já carrega a categoria para distinguir os casos. Dois espaços exigiriam duas tabelas ou chaves compostas, sem ganho para a linguagem.
- **Teste:** `test/programas/d5_namespace.prog`.

```
1  var f = 1;
2  void f(int a) { }
```
Esperado: `linha 2: 'f' já declarado neste escopo (linha 1)`.

---

## 5. Como a passada de identificação trata cada nó

| Nó | Ação |
|---|---|
| Bloco | `openScope`, visita os filhos na ordem, `closeScope` |
| Declaração de variável, constante | visita o inicializador (se houver) **antes** de declarar; monta o `Symbol` com `table.level()` e chama `declare` |
| Parâmetro | igual, com categoria `PARAMETRO`, dentro do escopo novo da função |
| Procedimento / função | declara o nome no escopo externo; `openScope`; declara os parâmetros; visita os comandos do corpo; `closeScope` (funções aninhadas usam o mesmo código) |
| Identificador de uso | `lookup`; se achou, grava em `decl`; se não, reporta `não declarado` e deixa `decl` nulo |
| Atribuição, expressões, controle, chamadas | só percorrem os filhos |

Se `declare` devolve `false`, a passada reporta a redeclaração e mantém a primeira declaração. A segunda é descartada.

### Formato das mensagens de erro

Os erros são acumulados em uma `List<String>` e a passada continua depois de cada um. O formato é fixo:

```
linha <L>: '<nome>' já declarado neste escopo (linha <L0>)
linha <L>: '<nome>' não declarado
```

A ordem das mensagens é a ordem do texto do programa, porque a travessia é em ordem e não há passada prévia.

---

## 6. Plano de testes

### 6.1 A tabela, isolada

- declarar e buscar um nome
- sombreamento: o interno oculta o externo e, depois do `closeScope`, o externo volta
- redeclaração: `declare` devolve `false` e o primeiro símbolo permanece
- `lookupLocal` não enxerga o escopo externo
- `lookup` de nome inexistente devolve `null`
- `level()` começa em 0, sobe com `openScope` e desce com `closeScope`
- `closeScope` sem escopo aberto lança `IllegalStateException` com mensagem

### 6.2 Programas obrigatórios

Saídas esperadas escritas à mão, antes de rodar a passada, em `test/esperado/`.

| Programa | Conteúdo | Esperado |
|---|---|---|
| `valido_sombreamento.prog` | o programa da Seção 1.1 | nenhum erro; cada uso resolve como na Seção 1.3 |
| `redeclaracao.prog` | `var` repetida no mesmo bloco | `linha 2: 'a' já declarado neste escopo (linha 1)` |
| `nao_declarado.prog` | uso de nome sem declaração | `linha 4: 'b' não declarado` |

### 6.3 Um programa por decisão e casos extras

| Programa | O que cobre |
|---|---|
| `d1_sombreamento.prog`, `d1_redeclaracao.prog` | decisão 1 |
| `d2_declarar_antes.prog` | decisão 2 |
| `d3_mutua.prog` | decisão 3 |
| `d4_param_corpo.prog` | decisão 4 |
| `d5_namespace.prog` | decisão 5 |
| `tres_erros.prog` | pelo menos três erros numa rodada, reportados na ordem do texto |
| `recursivo.prog` | procedimento recursivo (`fat` chamando `fat`): resolve para a própria declaração, sem erro |
| `uso_apos_bloco.prog` | nome declarado dentro de um bloco e usado depois do `}` → `não declarado` |

Exemplo de `tres_erros.prog` e da saída esperada:

```
1  var a = 1;
2  var a = 2;
3  b = a;
4  { var c = 0; }
5  c = 1;
```
```
linha 2: 'a' já declarado neste escopo (linha 1)
linha 3: 'b' não declarado
linha 5: 'c' não declarado
```

### 6.4 Regressão

A suíte do CP3 (construção da AST e Visitor) continua passando sem alterações.

### 6.5 Robustez

A passada não lança `NullPointerException` nem entra em laço infinito em nenhuma entrada válida ou inválida. Quando o uso não resolve, `decl` fica `null` e quem lê `decl` (CP5) precisa checar isso. A passada só roda sobre AST válida: programas com erro sintático não chegam até ela.

---

## 7. Estrutura da entrega

```
cp4-<nome-do-grupo>/
    escopo-design.md
    log-uso-ia.md
    src/          (scanner, parser, ast/, visitor/, symbols/)
    test/
        esperado/   (saídas esperadas, escritas à mão)
        programas/  (os programas de teste)
```
