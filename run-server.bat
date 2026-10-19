@echo off
mvn -q exec:java -Dexec.mainClass=battleship.server.BattleshipServer
pause
