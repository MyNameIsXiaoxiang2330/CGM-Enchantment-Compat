@echo off
set JAVA_HOME=E:\mod-workspace\Java8NEW
set GRADLE_HOME=%~dp0.gradle\gradle-4.10
"%GRADLE_HOME%\bin\gradle" %*
