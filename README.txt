AboApp - gestion des abonnes Telecom Atlas Mobile
==================================================

Appli interne DSI : clients, abonnements, cartes SIM, facturation mensuelle, portabilite.
Utilisee par les boutiques (logiciel caisse -> service web) et par le back-office (ecrans).

-> service SOAP : /abo/services/AbonnementService
-> ecrans : recherche client, fiche client, catalogue des offres (mot de passe boutiques dans config.properties)

Installation poste de dev
-------------------------
1. mysql -u root -p < sql/tam_abo.sql      (cree la base tam_abo)
2. verifier src/main/resources/config.properties (url jdbc, port 3306)
3. mvn package, copier target/abo.war dans le tomcat (webapps)
4. http://localhost:8080/abo/   et la liste des WSDL sur http://localhost:8080/abo/services/

Mise en prod : deploy.sh (serveur 10.20.1.15) ou deploy.bat depuis le poste DSI. Serveur base : 192.168.1.20.

Contact : Adil Chraibi (DSI, poste 4410)

Problemes connus
----------------
- les accents des noms de clients sortent mal dans l'export CSV ouvert avec Excel
- la recherche par nom est lente quand on tape moins de 3 lettres
