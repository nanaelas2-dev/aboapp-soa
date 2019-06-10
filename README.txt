AboApp - gestion des abonnes Telecom Atlas Mobile
==================================================

Appli interne DSI : clients, abonnements, cartes SIM, facturation mensuelle, portabilite.
Utilisee par les boutiques (logiciel caisse -> services web) et par le back-office (ecrans).

-> services SOAP : /abo/services/AbonnementService et /abo/services/PortabiliteService
-> ecrans : recherche client, fiche client, catalogue des offres (mot de passe boutiques dans config.properties)
-> batch : activation des SIM toutes les 30 s (fichier ACT_*.dat pour la plateforme reseau
   dans echanges/reseau/sortie), comptes rendus a deposer dans echanges/reseau/entree,
   facturation du mois quand FACTURATION_LANCER = O dans la table PARAMETRE
   (ou operation lancerFacturationMensuelle)

Installation poste de dev
-------------------------
1. mysql -u root -p < sql/tam_abo.sql      (cree la base tam_abo et l'utilisateur abo_app)
2. verifier src/main/resources/config.properties (url jdbc, port 3306)
3. mvn package cargo:run
4. http://localhost:8080/abo/   et la liste des WSDL sur http://localhost:8080/abo/services/

Les dossiers echanges/... sont crees au lancement, relatifs au repertoire courant.
Exemples de fichiers dans echanges/exemples (voir LISEZMOI.txt).

Mise en prod : deploy.sh (serveur 10.20.1.15) ou deploy.bat depuis le poste DSI.

Contact : Ghita El Ouafi (DSI, poste 4417). Avant : Adil Chraibi, Nadia Fassi Fihri.

Problemes connus
----------------
- la facturation du mois prend plusieurs minutes en prod, la caisse attend la reponse si on passe par le WS
- les accents des noms de clients sortent mal dans l'export CSV ouvert avec Excel
- le batch activation peut prendre un cycle de retard si la plateforme n'a pas encore ramasse le fichier
