#!/usr/bin/env bash
# Run the standalone local runner (Linux / macOS)
JAR=target/job-pulse-auto-apply-1.0.0.jar
if [ ! -f "$JAR" ]; then
  echo "Jar not found. Building local runner jar (profile: local-runner)"
  mvn -DskipTests -P local-runner package || { echo "Build failed"; exit 1; }
fi
if [ $# -ge 1 ]; then
  java -jar "$JAR" "$1" "$2"
else
  java -jar "$JAR"
fi


