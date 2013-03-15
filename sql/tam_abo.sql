/*
 * tam_abo : base AboApp (Telecom Atlas Mobile)
 *
 * A. Chraibi     04/2012  creation (clients, offres, abonnements, sim)
 * N. Fassi Fihri 10/2012  facturation (consommation, facture, ligne_facture)
 * K. Saidi       06/2013  offre_tarif (tarifs du service facturation)
 *
 * mysql -u root -p < sql/tam_abo.sql
 */

DROP DATABASE IF EXISTS tam_abo;
CREATE DATABASE tam_abo CHARACTER SET utf8 COLLATE utf8_general_ci;

USE tam_abo;
SET NAMES utf8;

-- ---------------------------------------------------------------
create table BOUTIQUE (
	ID_BOUTIQUE int not null auto_increment,
	CODE_BOUTIQUE varchar(10) not null,
	NOM varchar(80) not null,
	VILLE varchar(40),
	ADRESSE varchar(150),
	TEL varchar(15),
	RESPONSABLE varchar(80),
	OUVERTE char(1) default 'O',
	primary key (ID_BOUTIQUE),
	unique key UK_BOUTIQUE_CODE (CODE_BOUTIQUE)
) engine=InnoDB;

create table CLIENT (
	ID_CLIENT int not null auto_increment,
	CODE_CLIENT varchar(12) not null,
	TYPE_CLIENT varchar(4) not null default 'PART',
	CIVILITE varchar(5),
	NOM varchar(60),
	PRENOM varchar(60),
	RAISON_SOCIALE varchar(120),
	CIN varchar(12),
	ICE varchar(20),
	DATE_NAISSANCE date,
	ADRESSE varchar(200),
	VILLE varchar(40),
	TEL_CONTACT varchar(15),
	EMAIL varchar(100),
	ID_BOUTIQUE int,
	DATE_CREATION date,
	STATUT varchar(10) default 'ACTIF',
	CLIENT_DEPUIS date,
	primary key (ID_CLIENT),
	unique key UK_CLIENT_CODE (CODE_CLIENT),
	key IDX_CLIENT_NOM (NOM),
	key IDX_CLIENT_CIN (CIN)
) engine=InnoDB;

-- referentiel des offres (marketing)
create table OFFRE (
	CODE_OFFRE varchar(15) not null,
	LIBELLE varchar(80) not null,
	TYPE_OFFRE varchar(10) not null,
	PRIX_MENSUEL decimal(10,2),
	DATA_MO int,
	MINUTES int,
	NB_SMS int,
	ENGAGEMENT_MOIS int default 0,
	ACTIF tinyint default 1,
	DATE_LANCEMENT date,
	COMMENTAIRE varchar(200),
	primary key (CODE_OFFRE)
) engine=InnoDB;

-- tarifs utilises par la facturation
create table OFFRE_TARIF (
	ID_TARIF int not null auto_increment,
	CODE_OFFRE varchar(15) not null,
	LIBELLE_OFFRE varchar(80),
	TYPE_OFFRE varchar(10),
	PRIX_HT decimal(10,2),
	PRIX_TTC decimal(10,2),
	PRIX_MIN_NAT decimal(8,2),
	PRIX_MIN_INTER decimal(8,2),
	PRIX_SMS decimal(8,2),
	PRIX_MO decimal(8,2),
	FORFAIT_MINUTES int,
	FORFAIT_SMS int,
	FORFAIT_MO int,
	DATE_DEBUT date,
	DATE_FIN date,
	primary key (ID_TARIF),
	key IDX_TARIF_OFFRE (CODE_OFFRE)
) engine=InnoDB;

create table SIM (
	ID_SIM int not null auto_increment,
	ICCID varchar(20) not null,
	IMSI varchar(15) not null,
	PUK varchar(8),
	MSISDN varchar(10),
	STATUT varchar(12) not null default 'STOCK',
	ID_BOUTIQUE int,
	DATE_LIVRAISON date,
	DATE_ACTIVATION datetime,
	LOT_RESEAU varchar(40),
	primary key (ID_SIM),
	unique key UK_SIM_ICCID (ICCID),
	key IDX_SIM_STATUT (STATUT),
	key IDX_SIM_MSISDN (MSISDN)
) engine=InnoDB;

create table ABONNEMENT (
	ID_ABONNEMENT int not null auto_increment,
	NUM_CONTRAT varchar(14) not null,
	ID_CLIENT int not null,
	CODE_OFFRE varchar(15) not null,
	ANCIENNE_OFFRE varchar(15),
	DATE_CHGT_OFFRE date,
	MSISDN varchar(10),
	ID_SIM int,
	DATE_DEBUT date,
	DATE_FIN_ENGAGEMENT date,
	STATUT varchar(12) default 'EN_ATTENTE',
	MODE_PAIEMENT varchar(12) default 'ESPECES',
	CODE_PROMO varchar(20),
	ID_BOUTIQUE int,
	DATE_RESILIATION date,
	MOTIF_RESILIATION varchar(150),
	primary key (ID_ABONNEMENT),
	unique key UK_ABO_CONTRAT (NUM_CONTRAT),
	key IDX_ABO_MSISDN (MSISDN),
	constraint FK_ABO_CLIENT foreign key (ID_CLIENT) references CLIENT (ID_CLIENT)
) engine=InnoDB;

-- releves mensuels de la plateforme reseau (fichiers CDR agreges)
create table CONSOMMATION (
	ID_CONSO int not null auto_increment,
	MSISDN varchar(10) not null,
	ID_ABONNEMENT int,
	MOIS varchar(7) not null,
	TYPE_CONSO varchar(12) not null,
	QUANTITE decimal(12,2),
	UNITE varchar(5),
	ZONE varchar(10),
	DATE_RELEVE date,
	FACTURE char(1) default 'N',
	primary key (ID_CONSO),
	key IDX_CONSO_MSISDN_MOIS (MSISDN, MOIS)
) engine=InnoDB;

create table FACTURE (
	ID_FACTURE int not null auto_increment,
	NUM_FACTURE varchar(20) not null,
	TYPE_DOC varchar(8) default 'FACTURE',
	ID_CLIENT int not null,
	ID_ABONNEMENT int,
	MOIS varchar(7),
	DATE_EMISSION date,
	DATE_ECHEANCE date,
	MONTANT_HT decimal(12,2),
	MONTANT_REMISE decimal(12,2) default 0,
	MONTANT_TVA decimal(12,2),
	DROIT_TIMBRE decimal(10,2) default 0,
	MONTANT_TTC decimal(12,2),
	STATUT varchar(10) default 'EMISE',
	LOT varchar(30),
	primary key (ID_FACTURE),
	unique key UK_FACTURE_NUM (NUM_FACTURE),
	key IDX_FACT_CLIENT (ID_CLIENT),
	key IDX_FACT_ABO_MOIS (ID_ABONNEMENT, MOIS)
) engine=InnoDB;

create table LIGNE_FACTURE (
	ID_LIGNE int not null auto_increment,
	ID_FACTURE int not null,
	NUM_ORDRE int,
	CODE_LIGNE varchar(15),
	LIBELLE varchar(150),
	QUANTITE decimal(12,2),
	PU_HT decimal(10,4),
	MONTANT_HT decimal(12,2),
	TAUX_TVA decimal(5,2),
	primary key (ID_LIGNE),
	constraint FK_LIGNE_FACTURE foreign key (ID_FACTURE) references FACTURE (ID_FACTURE)
) engine=InnoDB;

create table PARAMETRE (
	CLE varchar(40) not null,
	VALEUR varchar(200),
	COMMENTAIRE varchar(200),
	primary key (CLE)
) engine=InnoDB;

-- boutiques
INSERT INTO BOUTIQUE (CODE_BOUTIQUE, NOM, VILLE, ADRESSE, TEL, RESPONSABLE) VALUES
('RB01', 'Agora Rabat Agdal', 'Rabat', '45, Av. de France, Agdal', '0537771020', 'Mounir Kettani'),
('RB02', 'Rabat Hassan', 'Rabat', '12, Av. Mohammed V', '0537202233', 'Laila Berrada'),
('RB03', 'Rabat Hay Riad', 'Rabat', 'Centre commercial Hay Riad, Mahaj Riad', '0537571144', 'Hamid Sqalli'),
('SL01', 'Sale Tabriquet', 'Sale', 'Bd Abdelkrim El Khattabi', '0537845566', 'Fouad Alami'),
('TM01', 'Temara centre', 'Temara', 'Av. Hassan II', '0537643310', 'Samira Ouazzani'),
('CA01', 'Casa Maarif', 'Casablanca', '88, Bd Zerktouni, Maarif', '0522254010', 'Rachid Belhaj'),
('CA02', 'Casa Anfa Place', 'Casablanca', 'Bd de la Corniche, Anfa Place', '0522798855', 'Siham Lahlou'),
('CA03', 'Casa Ain Sebaa', 'Casablanca', 'Route de Rabat, Ain Sebaa', '0522351278', 'Abdellah Tazi'),
('CA04', 'Casa Derb Omar', 'Casablanca', 'Rue Chaouia, Derb Omar', '0522441190', 'Nawal Saidi'),
('CA05', 'Casa Sidi Maarouf', 'Casablanca', 'Casanearshore, Sidi Maarouf', '0522875500', 'Yassine Naciri'),
('MO01', 'Mohammedia', 'Mohammedia', 'Av. des FAR', '0523315020', 'Khadija Amrani'),
('FE01', 'Fes Ville Nouvelle', 'Fes', 'Av. Hassan II, ville nouvelle', '0535654321', 'Driss Fassi Fihri'),
('FE02', 'Fes Borj Fez', 'Fes', 'Centre commercial Borj Fez', '0535731288', 'Ilham Bennani'),
('MK01', 'Meknes Hamria', 'Meknes', 'Av. des FAR, Hamria', '0535521677', 'Said Chraibi'),
('TG01', 'Tanger Bd Pasteur', 'Tanger', 'Bd Pasteur', '0539943377', 'Karima El Ouafi'),
('TG02', 'Tanger City Mall', 'Tanger', 'Tanger City Mall, route de Rabat', '0539340012', 'Omar Zniber'),
('TT01', 'Tetouan', 'Tetouan', 'Av. Mohammed V', '0539712245', 'Zineb Alaoui'),
('OU01', 'Oujda centre', 'Oujda', 'Bd Mohammed V', '0536682210', 'Mustapha Belhaj'),
('NA01', 'Nador', 'Nador', 'Bd Youssef Ben Tachfine', '0536607788', 'Hicham Amrani'),
('MR01', 'Marrakech Gueliz', 'Marrakech', 'Rue de la Liberte, Gueliz', '0524437799', 'Asmaa Naciri'),
('MR02', 'Marrakech Menara Mall', 'Marrakech', 'Menara Mall, Av. Mohammed VI', '0524339011', 'Tarik Lahlou'),
('AG01', 'Agadir Talborjt', 'Agadir', 'Av. du Prince Moulay Abdallah', '0528841230', 'Hafsa Kettani'),
('KE01', 'Kenitra', 'Kenitra', 'Av. Mohammed Diouri', '0537379900', 'Jamal Sqalli'),
('EJ01', 'El Jadida', 'El Jadida', 'Av. Mohammed VI', '0523342211', 'Loubna Tazi');

update BOUTIQUE set OUVERTE = 'N' where CODE_BOUTIQUE = 'TM01';

