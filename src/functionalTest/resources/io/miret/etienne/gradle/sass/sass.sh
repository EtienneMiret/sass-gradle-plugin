#!/bin/sh

for LAST_ARG; do true; done
DIR="${LAST_ARG#*:}"

mkdir -p "$DIR"
touch "$DIR"/style.css
