#!/bin/bash

BASE_DIR=$(dirname -- "$( readlink -f -- "$0"; )")

NUM_IMAGES=${1:-10}
NUM_WORKERS=${2:-$(nproc)}

echo "Executando pipeline: $NUM_IMAGES imagens, $NUM_WORKERS workers..."
time java -cp "$BASE_DIR/bin" ImagePipelineMain "$NUM_IMAGES" "$NUM_WORKERS"
