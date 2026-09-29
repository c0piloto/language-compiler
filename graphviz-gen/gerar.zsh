#!/usr/bin/env zsh
# Gera PNG e SVG de todos os .dot desta pasta
for f in *.dot; do
  dot -Tpng $f -o ${f:r}.png && dot -Tsvg $f -o ${f:r}.svg && echo "ok: $f"
done
