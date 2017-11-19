#! /bin/bash
set -x

for a in $@; do
  targets="$targets$a "
done

gradle run -Pargs="$targets"
