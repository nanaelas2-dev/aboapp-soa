#!/bin/sh
# deploiement AboApp sur le serveur de prod (a lancer sur 10.20.1.15)
TOMCAT=/opt/tomcat
WAR=target/abo.war

if [ ! -f $WAR ]; then
  echo "pas de $WAR, faire mvn package"
  exit 1
fi
$TOMCAT/bin/shutdown.sh
sleep 10
rm -rf $TOMCAT/webapps/abo $TOMCAT/webapps/abo.war
cp $WAR $TOMCAT/webapps/
cd $TOMCAT
./bin/startup.sh
echo "ok - verifier http://10.20.1.15:8080/abo/"
