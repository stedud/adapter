@echo off
setlocal

set cp=..\dist\kwsoft-otf-converter-1.0.jar
set cp=%cp%;"..\lib\*"

set vmargs=-Dkwsoft.env.globalcontext=converter.ini

set inputfile=CandidateLetter.gof
set outputdir=.

java %vmargs% -cp %cp% de.kwsoft.mtext.converter.gof.OtfConverter -if %inputfile% -od %outputdir% -f mfd -wh
