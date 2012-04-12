@echo off
rem copie du war sur le tomcat du poste DSI (test avant prod)
set TOMCAT=C:\tomcat7
if not exist target\abo.war goto erreur
call %TOMCAT%\bin\shutdown.bat
del /Q %TOMCAT%\webapps\abo.war
rmdir /S /Q %TOMCAT%\webapps\abo
copy target\abo.war %TOMCAT%\webapps\
call %TOMCAT%\bin\startup.bat
goto fin
:erreur
echo lancer mvn package avant
:fin
pause
