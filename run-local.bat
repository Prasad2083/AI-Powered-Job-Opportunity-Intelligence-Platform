@echo off
REM Run the standalone local runner (Windows)
set JAR=target\job-pulse-auto-apply-1.0.0.jar
if not exist %JAR% (
  echo Jar not found. Building local runner jar (profile: local-runner)
  mvn -DskipTests -P local-runner package || (
    echo Build failed
    exit /b 1
  )
)
if "%1"=="" (
  java -jar %JAR%
) else (
  java -jar %JAR% %1 %2
)

