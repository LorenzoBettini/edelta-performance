#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
REAL_EXAMPLE_RESULT_FILE=results/jmh-all-real-examples.json \
  ./scripts/run-jmh-real-example.sh \
  -p example=library-v1-v2,library-v2-v3,library-v3-v4,personlist-v1-v2,personlist-v2-v3,personlist-v3-v4,catalog-walkthrough,catalog-personlist,extract-class,extract-superclass,refactorings-personlist,statecharts,statecharts-enum,parallel-cleanup,parallel-inline,persons-inline \
  "$@"
