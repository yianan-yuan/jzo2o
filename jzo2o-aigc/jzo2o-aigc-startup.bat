@echo off
chcp 65001
title jzo2o-aigc.jar
echo.
echo [INFO] Packaging jzo2o-aigc.
echo.
call mvn package -DskipTests=true
echo.
echo [INFO] Starting jzo2o-aigc.
echo.
java -Dfile.encoding=utf-8 -Xmx256m -jar target/jzo2o-aigc.jar
