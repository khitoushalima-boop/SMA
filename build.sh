#!/bin/bash
# SRUU Build Script
# Requires: jade.jar in ./lib/

echo "=== SRUU Build Script ==="

# Check jade.jar
if [ ! -f "lib/jade.jar" ]; then
  echo "ERROR: lib/jade.jar not found!"
  echo "Download JADE from https://jade.tilab.com/ and place jade.jar in lib/"
  exit 1
fi

mkdir -p out

echo "Compiling sources..."
javac -cp lib/jade.jar \
  -sourcepath src \
  -d out \
  src/sruu/ontology/*.java \
  src/sruu/utils/*.java \
  src/sruu/agents/*.java \
  src/sruu/MainLauncher.java

if [ $? -eq 0 ]; then
  echo "Build successful!"
  echo ""
  echo "=== Starting SRUU Simulation ==="
  java -cp out:lib/jade.jar sruu.MainLauncher
else
  echo "Build FAILED. Check errors above."
  exit 1
fi
