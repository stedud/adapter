@echo off
setlocal

set cp=.
set cp=%cp%;..\dist\kwsoft-otf-converter-1.0.jar
set cp=%cp%;"..\lib\*"

set vmargs=-Dkwsoft.env.globalcontext=converter.ini

javac -cp %cp% ConvertFile.java
java %vmargs% -cp %cp% ConvertFile
