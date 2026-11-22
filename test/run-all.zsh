#!/usr/bin/env zsh
cd "$(dirname "$0")/.." || exit 2

rm -rf out && mkdir out
javac -d out $(find src -name '*.java') || exit 2
javac -cp out -d out test/*.java test/language/symbols/SymbolTableTest.java || exit 2

falhou=0
for t in language.symbols.SymbolTableTest OperatorTableTest TypeTestRunner IdentificationTestRunner TestRunner; do
  echo "=== $t ==="
  java -cp out $t || falhou=1
done

echo
[[ $falhou -eq 0 ]] && echo "TUDO PASSOU" || echo "HOUVE FALHAS"
exit $falhou
