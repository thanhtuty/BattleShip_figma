@echo off
mvn -q exec:java -Dexec.mainClass=battleship.client.ClientApp
pause
