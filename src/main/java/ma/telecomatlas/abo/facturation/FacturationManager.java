/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 * Tous droits reserves.
 */
package ma.telecomatlas.abo.facturation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ma.telecomatlas.abo.dao.AbonnementDao;
import ma.telecomatlas.abo.dao.ClientDao;
import ma.telecomatlas.abo.dao.ConsoDao;
import ma.telecomatlas.abo.dao.FactureDAO;
import ma.telecomatlas.abo.dao.OffreDao;
import ma.telecomatlas.abo.dao.ParamDao;
import ma.telecomatlas.abo.domaine.Abonnement;
import ma.telecomatlas.abo.domaine.Client;
import ma.telecomatlas.abo.domaine.Consommation;
import ma.telecomatlas.abo.domaine.Facture;
import ma.telecomatlas.abo.domaine.LigneFacture;
import ma.telecomatlas.abo.domaine.Offre;
import ma.telecomatlas.abo.domaine.OffreTarif;
import ma.telecomatlas.abo.outils.DateUtil;
import ma.telecomatlas.abo.outils.MontantUtil;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Calcul des factures mensuelles.
 *
 * Une facture = un abonnement x un mois. Le calcul part du tarif OFFRE_TARIF du mois,
 * des consommations remontees par la plateforme reseau (table CONSOMMATION), puis
 * applique promotions, remises, taxes et arrondis.
 *
 * Prepaye : on produit un RELEVE (pas de montant du par le client, information seulement).
 *
 * @author achraibi
 * @author nfassi
 */
public class FacturationManager {

	private static Logger log = Logger.getLogger(FacturationManager.class);

	public static final String TYPE_PREPAYE = "PREPAYE";
	public static final String TYPE_POSTPAYE = "POSTPAYE";
	public static final String TYPE_ENTREPRISE = "ENTREPRISE";

	public static final String DOC_FACTURE = "FACTURE";
	public static final String DOC_RELEVE = "RELEVE";
	public static final String DOC_AVOIR = "AVOIR";

	public static final double TAUX_TVA = 20.0;
	public static final double TAUX_TVA_2012 = 20.0;
	public static final double TAUX_TIMBRE = 0.25;
	public static final double TIMBRE_MINIMUM = 0.0;

	// codes de lignes
	public static final String L_FORFAIT = "FORFAIT";
	public static final String L_PASS = "PASS";
	public static final String L_HF_VOIX = "HF_VOIX";
	public static final String L_HF_SMS = "HF_SMS";
	public static final String L_HF_DATA = "HF_DATA";
	public static final String L_INTER = "INTER";
	public static final String L_ROAMING = "ROAM";
	public static final String L_PROMO = "PROMO";
	public static final String L_REMISE = "REMISE";
	public static final String L_PRORATA = "PRORATA";
	public static final String L_BONUS = "BONUS";
	public static final String L_FRAIS = "FRAIS";
	public static final String L_PENALITE = "PENALITE";

	// coefficients zones internationales (sur PRIX_MIN_INTER)
	private static final String[] ZONES = { "EUROPE", "AFRIQUE", "GOLFE", "AMERIQUE", "ASIE", "AUTRE" };
	private static final double[] COEF_ZONES = { 1.0, 1.5, 1.8, 1.2, 2.0, 2.5 };

	// roaming data, prix au Mo HT par zone (grille 2017)
	private static final double ROAMING_EUROPE = 2.50;
	private static final double ROAMING_GOLFE = 4.00;
	private static final double ROAMING_AFRIQUE = 5.00;
	private static final double ROAMING_AUTRE = 8.00;
	private static final double PLAFOND_ROAMING = 500.0;

	private static final double PLAFOND_DATA_POSTPAYE = 200.0;
	private static final double PLAFOND_DATA_ENTREPRISE = 350.0;

	private static final int SEUIL_FIDELITE_1 = 24;
	private static final int SEUIL_FIDELITE_2 = 60;

	private OffreDao offreDao;
	private ConsoDao consoDao;
	private ClientDao clientDao;
	private AbonnementDao abonnementDao;
	private FactureDAO factureDao;
	private ParamDao paramDao;
	private JdbcTemplate jdbcTemplate;

	private int delaiEcheance = 15;
	private boolean modeTest = false;

	// cache des tarifs par mois (vide a chaque lot)
	private Map<String, OffreTarif> cacheTarifs = new HashMap<String, OffreTarif>();

	/**
	 * Donnees de travail d'un calcul.
	 */
	static class Contexte {
		Abonnement abo;
		Client client;
		Offre offre;
		OffreTarif tarif;
		String mois;
		Date debutMois;
		Date finMois;
		int nbJours;
		int annee;
		int moisNum;
		List<Consommation> consos = new ArrayList<Consommation>();
		double minutesNat;
		double sms;
		double dataMo;
		Map<String, Double> inter = new HashMap<String, Double>();
		Map<String, Double> roaming = new HashMap<String, Double>();
		double prorata = 1.0;
		int ancienneteMois;
		int nbLignes = 1;
		boolean simulation;
		Facture facture;
		StringBuffer trace = new StringBuffer();
	}

	// =====================================================================
	// points d'entree
	// =====================================================================

	/**
	 * Calcule (sans l'enregistrer) la facture d'un abonnement pour un mois yyyy-MM.
	 * Renvoie null s'il n'y a rien a facturer.
	 */
	public Facture calculerFacture(Abonnement abo, String mois) {
		return calculer(abo, mois, false);
	}

	/**
	 * Simulation depuis les boutiques (operation simulerFacture) : prend aussi les
	 * consommations deja facturees.
	 */
	public Facture simulerFacture(Abonnement abo, String mois) {
		return calculer(abo, mois, true);
	}

	private Facture calculer(Abonnement abo, String mois, boolean simulation) {
		long t0 = System.currentTimeMillis();
		Contexte ctx = preparer(abo, mois, simulation);
		if (ctx == null) {
			return null;
		}
		String type = typeOffre(ctx);
		Facture f;
		String code = abo.getCodeOffre();
		if ("PP-SALAM".equals(code)) {
			f = calculerPrepayeSalam(ctx);
		} else if (TYPE_PREPAYE.equals(type)) {
			f = calculerPrepaye(ctx);
		} else if ("EN-M2M10".equals(code)) {
			f = calculerM2M(ctx);
		} else if (TYPE_ENTREPRISE.equals(type)) {
			f = calculerEntreprise(ctx);
		} else if ("PO-CTRL79".equals(code)) {
			f = calculerPostpayeBloque(ctx);
		} else {
			f = calculerPostpaye(ctx);
		}
		if (f != null) {
			controlerFacture(ctx, f);
			if (log.isDebugEnabled()) {
				log.debug("facture " + abo.getNumContrat() + " " + mois + " : " + f.getMontantTtc() + " TTC (" + (System.currentTimeMillis() - t0) + " ms) " + ctx.trace);
			}
		}
		return f;
	}

	private String typeOffre(Contexte ctx) {
		if (ctx.tarif != null && ctx.tarif.getTypeOffre() != null) {
			return ctx.tarif.getTypeOffre();
		}
		if (ctx.offre != null) {
			return ctx.offre.getTypeOffre();
		}
		if (ctx.abo.getTypeOffre() != null) {
			return ctx.abo.getTypeOffre();
		}
		// par defaut on considere postpaye (cas des vieux contrats 2012 sans offre)
		return TYPE_POSTPAYE;
	}

	// =====================================================================
	// preparation
	// =====================================================================

	Contexte preparer(Abonnement abo, String mois, boolean simulation) {
		Contexte ctx = new Contexte();
		ctx.abo = abo;
		ctx.mois = mois;
		ctx.simulation = simulation;
		ctx.debutMois = DateUtil.premierJourMois(mois);
		ctx.finMois = DateUtil.dernierJourMois(mois);
		if (ctx.debutMois == null) {
			log.error("mois invalide : " + mois);
			return null;
		}
		ctx.nbJours = DateUtil.nbJoursMois(mois);
		ctx.annee = Integer.parseInt(mois.substring(0, 4));
		ctx.moisNum = Integer.parseInt(mois.substring(5, 7));

		// abonnement pas encore commence
		if (abo.getDateDebut() != null && abo.getDateDebut().after(ctx.finMois)) {
			return null;
		}
		// resilie avant le mois
		if (abo.getDateResiliation() != null && abo.getDateResiliation().before(ctx.debutMois)) {
			return null;
		}
		if ("EN_ATTENTE".equals(abo.getStatut())) {
			return null;
		}

		ctx.client = clientDao.trouver(abo.getIdClient());
		if (ctx.client == null) {
			log.warn("client introuvable " + abo.getIdClient() + " pour contrat " + abo.getNumContrat());
			return null;
		}
		ctx.offre = offreDao.trouver(abo.getCodeOffre());
		ctx.tarif = tarif(abo.getCodeOffre(), mois);
		if (ctx.tarif == null) {
			log.warn("pas de tarif " + abo.getCodeOffre() + " pour " + mois + ", on prend le prix de l'offre");
			ctx.tarif = tarifParDefaut(ctx.offre, abo.getCodeOffre());
			if (ctx.tarif == null) {
				return null;
			}
		}
		if (simulation) {
			ctx.consos = consoDao.lister(abo.getMsisdn(), mois);
		} else {
			ctx.consos = consoDao.listerNonFacturees(abo.getMsisdn(), mois);
		}
		totaliser(ctx);
		ctx.prorata = calculerProrata(ctx);
		ctx.ancienneteMois = anciennete(ctx);
		if (ctx.client.isEntreprise()) {
			ctx.nbLignes = clientDao.nbLignesActives(ctx.client.getIdClient());
		}
		return ctx;
	}

	private OffreTarif tarif(String code, String mois) {
		String cle = code + "|" + mois;
		if (cacheTarifs.containsKey(cle)) {
			return cacheTarifs.get(cle);
		}
		OffreTarif t = offreDao.tarif(code, mois);
		cacheTarifs.put(cle, t);
		return t;
	}

	public void viderCache() {
		cacheTarifs.clear();
	}

	/**
	 * Offre sans ligne dans OFFRE_TARIF : on reconstruit un tarif avec les prix standard.
	 */
	private OffreTarif tarifParDefaut(Offre o, String code) {
		if (o == null) {
			log.error("offre " + code + " absente de OFFRE et de OFFRE_TARIF");
			return null;
		}
		OffreTarif t = new OffreTarif();
		t.setCodeOffre(o.getCodeOffre());
		t.setLibelleOffre(o.getLibelle());
		t.setTypeOffre(o.getTypeOffre());
		t.setPrixTtc(o.getPrixMensuel());
		t.setPrixHt(MontantUtil.ht(o.getPrixMensuel(), TAUX_TVA));
		t.setPrixMinNat(TYPE_ENTREPRISE.equals(o.getTypeOffre()) ? 0.90 : 1.20);
		t.setPrixMinInter(4.80);
		t.setPrixSms(0.50);
		t.setPrixMo(0.10);
		t.setForfaitMinutes(o.getMinutes());
		t.setForfaitSms(o.getNbSms());
		t.setForfaitMo(o.getDataMo());
		return t;
	}

	private void totaliser(Contexte ctx) {
		for (int i = 0; i < ctx.consos.size(); i++) {
			Consommation c = ctx.consos.get(i);
			String t = c.getTypeConso();
			if ("VOIX_NAT".equals(t)) {
				ctx.minutesNat += c.getQuantite();
			} else if ("SMS".equals(t)) {
				ctx.sms += c.getQuantite();
			} else if ("DATA".equals(t)) {
				ctx.dataMo += c.getQuantite();
			} else if ("VOIX_INTER".equals(t)) {
				cumuler(ctx.inter, zone(c.getZone()), c.getQuantite());
			} else if ("ROAMING".equals(t)) {
				cumuler(ctx.roaming, zone(c.getZone()), c.getQuantite());
			} else {
				log.warn("type de consommation inconnu " + t + " (" + c.getMsisdn() + ")");
			}
		}
	}

	private void cumuler(Map<String, Double> m, String cle, double q) {
		Double d = m.get(cle);
		m.put(cle, new Double((d == null ? 0 : d.doubleValue()) + q));
	}

	private String zone(String z) {
		if (z == null) {
			return "AUTRE";
		}
		for (int i = 0; i < ZONES.length; i++) {
			if (ZONES[i].equalsIgnoreCase(z.trim())) {
				return ZONES[i];
			}
		}
		return "AUTRE";
	}

	private double coefZone(String z) {
		for (int i = 0; i < ZONES.length; i++) {
			if (ZONES[i].equals(z)) {
				return COEF_ZONES[i];
			}
		}
		return 2.5;
	}

	private int anciennete(Contexte ctx) {
		Date depuis = ctx.client.getClientDepuis();
		if (depuis == null) {
			depuis = ctx.client.getDateCreation();
		}
		if (depuis == null) {
			depuis = ctx.abo.getDateDebut();
		}
		return DateUtil.moisEntre(depuis, ctx.finMois);
	}

	private Facture nouvelleFacture(Contexte ctx, String typeDoc) {
		Facture f = new Facture();
		f.setTypeDoc(typeDoc);
		f.setIdClient(ctx.abo.getIdClient());
		f.setIdAbonnement(ctx.abo.getIdAbonnement());
		f.setMois(ctx.mois);
		Date emission = new Date();
		f.setDateEmission(emission);
		f.setDateEcheance(DateUtil.ajouterJours(emission, delaiEcheance));
		f.setStatut(DOC_RELEVE.equals(typeDoc) ? "INFO" : "EMISE");
		ctx.facture = f;
		return f;
	}

	private LigneFacture ligne(Facture f, String code, String libelle, double qte, double pu, double montant) {
		LigneFacture l = new LigneFacture(code, libelle, qte, MontantUtil.arrondi(pu, 4), MontantUtil.arrondi(montant));
		f.ajouterLigne(l);
		return l;
	}

	// =====================================================================
	// postpaye (particuliers)
	// =====================================================================

	private Facture calculerPostpaye(Contexte ctx) {
		Facture f = nouvelleFacture(ctx, DOC_FACTURE);
		String libOffre = ctx.tarif.getLibelleOffre() != null ? ctx.tarif.getLibelleOffre() : ctx.abo.getCodeOffre();

		// 1. forfait
		double forfait = montantForfait(ctx);
		if (ctx.abo.getAncienneOffre() != null && DateUtil.memeMois(ctx.abo.getDateChgtOffre(), ctx.mois)) {
			forfait = ligneChangementOffre(ctx, f, forfait);
		} else if (ctx.prorata < 1.0) {
			double p = MontantUtil.arrondi(forfait * ctx.prorata);
			ligne(f, L_FORFAIT, "Forfait " + libOffre + " - " + libelleMois(ctx.mois) + " (prorata " + Math.round(ctx.prorata * ctx.nbJours) + " j)", ctx.prorata, forfait, p);
			ctx.trace.append(" prorata=").append(ctx.prorata);
		} else {
			ligne(f, L_FORFAIT, "Forfait " + libOffre + " - " + libelleMois(ctx.mois), 1, forfait, forfait);
		}

		// 2. hors forfait
		if (!"SUSPENDU".equals(ctx.abo.getStatut()) || ctx.minutesNat > 0) {
			horsForfaitVoixPostpaye(ctx, f);
			horsForfaitSmsPostpaye(ctx, f);
			horsForfaitDataPostpaye(ctx, f);
		}
		internationalPostpaye(ctx, f);
		roamingPostpaye(ctx, f);

		// 3. promotions et remises
		appliquerPromotions(ctx, f);
		remiseFidelite(ctx, f);

		// 4. frais, cas particuliers
		ajustementsAnnuels(ctx, f);
		if ("SUSPENDU".equals(ctx.abo.getStatut()) && ctx.annee >= 2015) {
			ligne(f, L_FRAIS, "Frais de suspension de ligne", 1, 20.0, 20.0);
		}
		if (ctx.abo.getDateResiliation() != null && DateUtil.memeMois(ctx.abo.getDateResiliation(), ctx.mois)) {
			penaliteResiliation(ctx, f);
		}

		// 5. totaux
		calculerTotaux(ctx, f);
		return f;
	}

	// =====================================================================
	// prepaye : releve d'information
	// =====================================================================

	private Facture calculerPrepaye(Contexte ctx) {
		Facture f = nouvelleFacture(ctx, DOC_RELEVE);
		String code = ctx.abo.getCodeOffre();
		double pass = montantForfait(ctx);
		if (pass > 0) {
			String libOffre = ctx.tarif.getLibelleOffre() != null ? ctx.tarif.getLibelleOffre() : code;
			if (ctx.prorata < 1.0 && ctx.annee >= 2016) {
				// depuis 2016 le pass n'est plus proratise (decision marketing) mais on garde la ligne
				ligne(f, L_PASS, "Pass mensuel " + libOffre + " (activation en cours de mois)", 1, pass, pass);
			} else {
				ligne(f, L_PASS, "Pass mensuel " + libOffre, 1, pass, pass);
			}
		}
		horsForfaitVoixPrepaye(ctx, f);
		horsForfaitSmsPrepaye(ctx, f);
		horsForfaitDataPrepaye(ctx, f);
		internationalPrepaye(ctx, f);
		roamingPrepaye(ctx, f);
		if (f.getLignes().size() == 0) {
			return null;
		}
		calculerTotauxReleve(ctx, f);
		return f;
	}

	// =====================================================================
	// entreprises (flottes, pro, m2m)
	// =====================================================================

	private Facture calculerEntreprise(Contexte ctx) {
		Facture f = nouvelleFacture(ctx, DOC_FACTURE);
		String libOffre = ctx.tarif.getLibelleOffre() != null ? ctx.tarif.getLibelleOffre() : ctx.abo.getCodeOffre();

		double forfait = montantForfait(ctx);
		if (ctx.abo.getAncienneOffre() != null && DateUtil.memeMois(ctx.abo.getDateChgtOffre(), ctx.mois)) {
			forfait = ligneChangementOffre(ctx, f, forfait);
		} else if (ctx.prorata < 1.0) {
			double p = MontantUtil.arrondi(forfait * ctx.prorata);
			ligne(f, L_FORFAIT, "Forfait " + libOffre + " - " + libelleMois(ctx.mois) + " (prorata " + Math.round(ctx.prorata * ctx.nbJours) + " j)", ctx.prorata, forfait, p);
		} else {
			ligne(f, L_FORFAIT, "Forfait " + libOffre + " - " + libelleMois(ctx.mois), 1, forfait, forfait);
		}

		horsForfaitVoixEntreprise(ctx, f);
		horsForfaitSmsEntreprise(ctx, f);
		horsForfaitDataEntreprise(ctx, f);
		internationalEntreprise(ctx, f);
		roamingEntreprise(ctx, f);

		appliquerPromotionsEntreprise(ctx, f);
		remiseFlotte(ctx, f);

		if ("SUSPENDU".equals(ctx.abo.getStatut()) && ctx.annee >= 2015) {
			ligne(f, L_FRAIS, "Frais de suspension de ligne", 1, 20.0, 20.0);
		}
		if (ctx.abo.getDateResiliation() != null && DateUtil.memeMois(ctx.abo.getDateResiliation(), ctx.mois)) {
			penaliteResiliationEntreprise(ctx, f);
		}

		calculerTotauxEntreprise(ctx, f);
		return f;
	}

	// =====================================================================
	// prorata / changement d'offre
	// =====================================================================

	double calculerProrata(Contexte ctx) {
		Date debut = ctx.abo.getDateDebut();
		Date fin = ctx.abo.getDateResiliation();
		int premier = 1;
		int dernier = ctx.nbJours;
		if (debut != null && DateUtil.memeMois(debut, ctx.mois)) {
			premier = DateUtil.jour(debut);
		}
		if (fin != null && DateUtil.memeMois(fin, ctx.mois)) {
			dernier = DateUtil.jour(fin);
		}
		if (premier == 1 && dernier == ctx.nbJours) {
			return 1.0;
		}
		int jours = dernier - premier + 1;
		if (jours <= 0) {
			return 0;
		}
		// regle 2012 : mois de 30 jours (on a garde pour les contrats d'avant 2014)
		if (debut != null && DateUtil.annee(debut) < 2014) {
			return Math.min(1.0, jours / 30.0);
		}
		return jours / (double) ctx.nbJours;
	}

	/**
	 * Changement d'offre dans le mois : ancienne offre jusqu'a la veille du changement,
	 * nouvelle offre ensuite. Renvoie le montant du forfait facture.
	 */
	private double ligneChangementOffre(Contexte ctx, Facture f, double forfaitNouveau) {
		int jourChgt = DateUtil.jour(ctx.abo.getDateChgtOffre());
		OffreTarif ancien = tarif(ctx.abo.getAncienneOffre(), ctx.mois);
		double forfaitAncien;
		String libAncien;
		if (ancien != null) {
			forfaitAncien = ancien.getPrixHt();
			libAncien = ancien.getLibelleOffre();
		} else {
			Offre o = offreDao.trouver(ctx.abo.getAncienneOffre());
			forfaitAncien = o == null ? 0 : MontantUtil.ht(o.getPrixMensuel(), TAUX_TVA);
			libAncien = o == null ? ctx.abo.getAncienneOffre() : o.getLibelle();
		}
		double partAncien = (jourChgt - 1) / (double) ctx.nbJours;
		double partNouveau = 1.0 - partAncien;
		double mtAncien = MontantUtil.arrondi(forfaitAncien * partAncien);
		double mtNouveau = MontantUtil.arrondi(forfaitNouveau * partNouveau);
		if (mtAncien > 0) {
			ligne(f, L_FORFAIT, "Forfait " + libAncien + " du 01 au " + (jourChgt - 1) + " " + libelleMois(ctx.mois), partAncien, forfaitAncien, mtAncien);
		}
		String libNouveau = ctx.tarif.getLibelleOffre();
		ligne(f, L_FORFAIT, "Forfait " + libNouveau + " a partir du " + jourChgt + " " + libelleMois(ctx.mois), partNouveau, forfaitNouveau, mtNouveau);
		// frais de changement : gratuit vers une offre superieure
		if (forfaitNouveau < forfaitAncien && ctx.annee >= 2014) {
			ligne(f, L_FRAIS, "Frais de changement d'offre", 1, 25.0, 25.0);
		}
		ctx.trace.append(" chgt ").append(ctx.abo.getAncienneOffre()).append("->").append(ctx.abo.getCodeOffre());
		return mtAncien + mtNouveau;
	}

	private void penaliteResiliation(Contexte ctx, Facture f) {
		Date finEng = ctx.abo.getDateFinEngagement();
		Date res = ctx.abo.getDateResiliation();
		if (finEng == null || !res.before(finEng)) {
			return;
		}
		int restants = DateUtil.moisEntre(res, finEng) + 1;
		double forfait = ctx.tarif.getPrixHt();
		double pen = MontantUtil.arrondi(restants * forfait * 0.5);
		if (pen > 0) {
			ligne(f, L_PENALITE, "Indemnite de resiliation anticipee (" + restants + " mois)", restants, forfait * 0.5, pen);
		}
	}

	// copie de penaliteResiliation, les entreprises payent 100% des mois restants (contrat cadre)
	private void penaliteResiliationEntreprise(Contexte ctx, Facture f) {
		Date finEng = ctx.abo.getDateFinEngagement();
		Date res = ctx.abo.getDateResiliation();
		if (finEng == null || !res.before(finEng)) {
			return;
		}
		int restants = DateUtil.moisEntre(res, finEng) + 1;
		double forfait = ctx.tarif.getPrixHt();
		double pen = MontantUtil.arrondi(restants * forfait);
		if (pen > 0) {
			ligne(f, L_PENALITE, "Indemnite de resiliation anticipee (" + restants + " mois)", restants, forfait, pen);
		}
	}

	// =====================================================================
	// forfaits par offre
	// =====================================================================

	/**
	 * Montant HT mensuel du forfait (avant prorata).
	 */
	double montantForfait(Contexte ctx) {
		String code = ctx.abo.getCodeOffre();
		if ("PP-SALAM".equals(code)) {
			return forfaitSalam(ctx);
		} else if ("PP-JAWAL20".equals(code)) {
			return forfaitJawal20(ctx);
		} else if ("PP-JAWAL49".equals(code)) {
			return forfaitJawal49(ctx);
		} else if ("PO-NJOUM79".equals(code)) {
			return forfaitNjoum79(ctx);
		} else if ("PO-NJOUM99".equals(code)) {
			return forfaitNjoum99(ctx);
		} else if ("PO-NJOUM149".equals(code)) {
			return forfaitNjoum149(ctx);
		} else if ("PO-NJOUM249".equals(code)) {
			return forfaitNjoum249(ctx);
		} else if ("PO-ILIMIT299".equals(code)) {
			return forfaitIllimite299(ctx);
		} else if ("PO-CTRL79".equals(code)) {
			return forfaitControle79(ctx);
		} else if ("PO-CHABAB59".equals(code)) {
			return forfaitChabab59(ctx);
		} else if ("PO-DATA50".equals(code)) {
			return forfaitData50(ctx);
		} else if ("EN-PRO200".equals(code)) {
			return forfaitPro200(ctx);
		} else if ("EN-PRO400".equals(code)) {
			return forfaitPro400(ctx);
		} else if ("EN-FLOTTE150".equals(code)) {
			return forfaitFlotte150(ctx);
		} else if ("EN-FLOTTE300".equals(code)) {
			return forfaitFlotte300(ctx);
		} else if ("EN-M2M10".equals(code)) {
			return forfaitM2M(ctx);
		}
		// offre inconnue : prix du tarif tel quel
		log.warn("pas de regle de forfait pour " + code);
		return ctx.tarif.getPrixHt();
	}

	/** Salam : carte prepayee sans pass, rien a facturer au titre du forfait */
	private double forfaitSalam(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (ctx.annee < 2012) {
			log.warn("PP-SALAM facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		// pas de pass, la consommation est valorisee au tarif prepaye
		prix = 0;
		return MontantUtil.arrondi(prix);
	}

	/** Jawal 20 (pass 1 Go) */
	private double forfaitJawal20(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(20.0, TAUX_TVA);
		}
		if (ctx.annee < 2013) {
			log.warn("PP-JAWAL20 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee == 2013 && ctx.moisNum >= 6 && ctx.moisNum <= 8) {
			// lancement ete 2013 : pass a 15 Dh
			prix = MontantUtil.ht(15.0, TAUX_TVA);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Jawal 49 (pass 5 Go) */
	private double forfaitJawal49(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(49.0, TAUX_TVA);
		}
		if (ctx.annee < 2015) {
			log.warn("PP-JAWAL49 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.dataMo > 5000 && ctx.annee >= 2018) {
			ligne(ctx.facture, L_BONUS, "Bonus 1 Go offert (recharges du mois)", 1, 0, 0);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Njoum 79, offre de lancement 2012, plus commercialisee */
	private double forfaitNjoum79(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(79.0, TAUX_TVA);
		}
		if (ctx.annee < 2012) {
			log.warn("PO-NJOUM79 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		// les clients Njoum 79 restants gardent le prix 2012 (engagement de la direction commerciale)
		prix = MontantUtil.ht(79.0, TAUX_TVA_2012);
		if (ctx.annee >= 2016) {
			// TODO nadia 2016-02 : migrer les derniers Njoum 79 vers Njoum 99 ?
			prix = prix + 0;
		}
		return MontantUtil.arrondi(prix);
	}

	/** Njoum 99 */
	private double forfaitNjoum99(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(99.0, TAUX_TVA);
		}
		if (ctx.annee < 2014) {
			log.warn("PO-NJOUM99 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee == 2014 && ctx.moisNum <= 3) {
			// promo de lancement : 3 premiers mois a 89 Dh
			prix = MontantUtil.ht(89.0, TAUX_TVA);
		}
		if (ctx.abo.getDateDebut() != null && DateUtil.annee(ctx.abo.getDateDebut()) < 2018 && ctx.annee >= 2018) {
			// passage de 4 a 10 Go en 2018 pour tout le monde, meme prix
			ctx.trace.append(" njoum99-2018");
		}
		return MontantUtil.arrondi(prix);
	}

	/** Njoum 149 (159 Dh jusqu'en juin 2016) */
	private double forfaitNjoum149(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(149.0, TAUX_TVA);
		}
		if (ctx.annee < 2012) {
			log.warn("PO-NJOUM149 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee < 2016 || (ctx.annee == 2016 && ctx.moisNum <= 6)) {
			if (ctx.tarif.getPrixTtc() < 159.0) {
				// le tarif du mois n'a pas ete trouve, on remet l'ancien prix
				prix = MontantUtil.ht(159.0, TAUX_TVA);
			}
		}
		return MontantUtil.arrondi(prix);
	}

	/** Njoum 249 */
	private double forfaitNjoum249(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(249.0, TAUX_TVA);
		}
		if (ctx.annee < 2013) {
			log.warn("PO-NJOUM249 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.minutesNat > 1200 && ctx.annee >= 2017) {
			ligne(ctx.facture, L_BONUS, "Appels illimites vers 3 numeros favoris", 1, 0, 0);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Illimite 299 (remplace par 349 en novembre 2018) */
	private double forfaitIllimite299(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(299.0, TAUX_TVA);
		}
		if (ctx.annee < 2016) {
			log.warn("PO-ILIMIT299 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee > 2018 || (ctx.annee == 2018 && ctx.moisNum >= 11)) {
			// plus de tarif apres octobre 2018 : on facture au prix de l'offre
			if (ctx.offre != null) {
				prix = MontantUtil.ht(ctx.offre.getPrixMensuel(), TAUX_TVA);
			}
		}
		return MontantUtil.arrondi(prix);
	}

	/** Forfait bloque 79 : pas de hors forfait */
	private double forfaitControle79(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(79.0, TAUX_TVA);
		}
		if (ctx.annee < 2015) {
			log.warn("PO-CTRL79 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee == 2015) {
			prix = MontantUtil.ht(75.0, TAUX_TVA);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Chabab 59 (moins de 26 ans) */
	private double forfaitChabab59(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(59.0, TAUX_TVA);
		}
		if (ctx.annee < 2016) {
			log.warn("PO-CHABAB59 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.client.getDateNaissance() != null) {
			int age = DateUtil.moisEntre(ctx.client.getDateNaissance(), ctx.finMois) / 12;
			if (age >= 26) {
				// plus eligible : on facture comme Njoum 99 depuis 2018
				if (ctx.annee >= 2018) {
					prix = MontantUtil.ht(99.0, TAUX_TVA);
					ctx.trace.append(" chabab>26");
				}
			}
		}
		return MontantUtil.arrondi(prix);
	}

	/** Data 50 : cle 4G */
	private double forfaitData50(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(50.0, TAUX_TVA);
		}
		if (ctx.annee < 2015) {
			log.warn("PO-DATA50 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee <= 2016) {
			prix = MontantUtil.ht(60.0, TAUX_TVA);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Pro 200 */
	private double forfaitPro200(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(199.0, TAUX_TVA);
		}
		if (ctx.annee < 2013) {
			log.warn("EN-PRO200 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		// OFFRE_TARIF est a 200 Dh TTC, le contrat cadre dit 199 : on garde le tarif
		if (ctx.nbLignes >= 50) {
			prix = prix - 10;
		}
		return MontantUtil.arrondi(prix);
	}

	/** Pro 400 */
	private double forfaitPro400(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(399.0, TAUX_TVA);
		}
		if (ctx.annee < 2013) {
			log.warn("EN-PRO400 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.annee == 2013) {
			prix = MontantUtil.ht(379.0, TAUX_TVA);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Flotte 150 */
	private double forfaitFlotte150(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(150.0, TAUX_TVA);
		}
		if (ctx.annee < 2014) {
			log.warn("EN-FLOTTE150 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.nbLignes > 10 && ctx.annee >= 2016) {
			prix = MontantUtil.ht(140.0, TAUX_TVA);
		}
		return MontantUtil.arrondi(prix);
	}

	/** Flotte 300 (vendue par le commercial grands comptes) */
	private double forfaitFlotte300(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(300.0, TAUX_TVA);
		}
		if (ctx.annee < 2014) {
			log.warn("EN-FLOTTE300 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		if (ctx.nbLignes > 10 && ctx.annee >= 2016) {
			prix = MontantUtil.ht(285.0, TAUX_TVA);
		}
		return MontantUtil.arrondi(prix);
	}

	/** M2M (terminaux de paiement, compteurs...). Le tarif est saisi sous EN-M2M */
	private double forfaitM2M(Contexte ctx) {
		double prix = ctx.tarif.getPrixHt();
		if (prix <= 0) {
			prix = MontantUtil.ht(10.0, TAUX_TVA);
		}
		if (ctx.annee < 2017) {
			log.warn("EN-M2M10 facture avant son lancement (" + ctx.mois + ") contrat " + ctx.abo.getNumContrat());
		}

		OffreTarif t = tarif("EN-M2M", ctx.mois);
		if (t != null) {
			prix = t.getPrixHt();
		}
		return MontantUtil.arrondi(prix);
	}

	// ---------------------------------------------------------------- hors forfait Postpaye

	private double horsForfaitVoixPostpaye(Contexte ctx, Facture f) {
		if (ctx.minutesNat <= 0) {
			return 0;
		}
		if ("PO-CTRL79".equals(ctx.abo.getCodeOffre())) {
			// forfait bloque : le client recharge, rien en hors forfait
			return 0;
		}
		int forfaitMin = ctx.tarif.getForfaitMinutes();
		if (forfaitMin < 0) {
			// illimite
			return 0;
		}
		double inclus = ctx.tarif.getForfaitMinutes() * ctx.prorata;
		double depasse = Math.ceil(ctx.minutesNat - inclus);
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixMinNat() / (1 + TAUX_TVA / 100);
		double mt = MontantUtil.arrondi(depasse * pu);
		ligne(f, L_HF_VOIX, "Appels nationaux hors forfait", depasse, pu, mt);
		return mt;
	}

	private double horsForfaitSmsPostpaye(Contexte ctx, Facture f) {
		if (ctx.sms <= 0) {
			return 0;
		}
		if ("PO-CTRL79".equals(ctx.abo.getCodeOffre())) {
			// forfait bloque : le client recharge, rien en hors forfait
			return 0;
		}
		int forfaitSms = ctx.tarif.getForfaitSms();
		if (forfaitSms < 0) {
			return 0;
		}
		double depasse = Math.ceil(ctx.sms - ctx.tarif.getForfaitSms() * ctx.prorata);
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixSms() / (1 + TAUX_TVA / 100);
		double mt = MontantUtil.arrondi(depasse * pu);
		ligne(f, L_HF_SMS, "SMS hors forfait", depasse, pu, mt);
		return mt;
	}

	private double horsForfaitDataPostpaye(Contexte ctx, Facture f) {
		if (ctx.dataMo <= 0) {
			return 0;
		}
		if ("PO-CTRL79".equals(ctx.abo.getCodeOffre())) {
			// forfait bloque : le client recharge, rien en hors forfait
			return 0;
		}
		int forfaitMo = ctx.tarif.getForfaitMo();
		if (forfaitMo < 0) {
			return 0;
		}
		double depasse = Math.ceil(ctx.dataMo - ctx.tarif.getForfaitMo() * ctx.prorata);
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixMo();
		double mt = MontantUtil.arrondi(depasse * pu);
		String lib = "Internet mobile hors forfait (" + (long) depasse + " Mo)";
		if (ctx.annee >= 2016 && mt > PLAFOND_DATA_POSTPAYE) {
			mt = PLAFOND_DATA_POSTPAYE;
			lib = lib + " (plafonne)";
		}
		ligne(f, L_HF_DATA, lib, depasse, pu, mt);
		return mt;
	}

	private double internationalPostpaye(Contexte ctx, Facture f) {
		double total = 0;
		for (int i = 0; i < ZONES.length; i++) {
			String z = ZONES[i];
			Double q = ctx.inter.get(z);
			if (q == null || q.doubleValue() <= 0) {
				continue;
			}
			double pu = ctx.tarif.getPrixMinInter() * coefZone(z) / (1 + TAUX_TVA / 100);
			double mt = MontantUtil.arrondi(Math.ceil(q.doubleValue()) * pu);
			ligne(f, L_INTER, "Appels internationaux " + z.toLowerCase(), Math.ceil(q.doubleValue()), pu, mt);
			total += mt;
		}
		return total;
	}

	private double roamingPostpaye(Contexte ctx, Facture f) {
		double total = 0;
		Iterator<String> it = ctx.roaming.keySet().iterator();
		while (it.hasNext()) {
			String z = it.next();
			double q = ctx.roaming.get(z).doubleValue();
			double pu;
			if ("EUROPE".equals(z)) {
				pu = ROAMING_EUROPE;
			} else if ("GOLFE".equals(z)) {
				pu = ROAMING_GOLFE;
			} else if ("AFRIQUE".equals(z)) {
				pu = ROAMING_AFRIQUE;
			} else {
				pu = ROAMING_AUTRE;
			}
			pu = pu * 1.0;
			if (ctx.annee < 2017) {
				// ancienne grille (x2)
				pu = pu * 2;
			}
			double mt = MontantUtil.arrondi(q * pu);
			ligne(f, L_ROAMING, "Internet en itinerance " + z.toLowerCase() + " (" + (long) q + " Mo)", q, pu, mt);
			total += mt;
		}
		if (ctx.annee >= 2017 && total > PLAFOND_ROAMING) {
			// plafond reglementaire, on ajoute une ligne negative
			ligne(f, L_ROAMING, "Plafonnement itinerance", 1, -(total - PLAFOND_ROAMING), -(total - PLAFOND_ROAMING));
			total = PLAFOND_ROAMING;
		}
		return total;
	}

	// ---------------------------------------------------------------- hors forfait Prepaye

	private double horsForfaitVoixPrepaye(Contexte ctx, Facture f) {
		if (ctx.minutesNat <= 0) {
			return 0;
		}
		// pas de blocage
		int forfaitMin = (ctx.offre != null ? ctx.offre.getMinutes() : 0);
		if (forfaitMin < 0) {
			// illimite
			return 0;
		}
		double inclus = (ctx.offre != null ? ctx.offre.getMinutes() : 0);
		double depasse = Math.ceil(ctx.minutesNat - inclus);
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixMinNat() / (1 + TAUX_TVA / 100);
		double mt = MontantUtil.arrondi(depasse * pu);
		ligne(f, L_HF_VOIX, "Appels nationaux (debites sur recharge)", depasse, pu, mt);
		return mt;
	}

	private double horsForfaitSmsPrepaye(Contexte ctx, Facture f) {
		if (ctx.sms <= 0) {
			return 0;
		}
		// pas de blocage
		int forfaitSms = (ctx.offre != null ? ctx.offre.getNbSms() : 0);
		if (forfaitSms < 0) {
			return 0;
		}
		double depasse = Math.ceil(ctx.sms - (ctx.offre != null ? ctx.offre.getNbSms() : 0));
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixSms() / (1 + TAUX_TVA / 100);
		double mt = MontantUtil.arrondi(depasse * pu);
		ligne(f, L_HF_SMS, "SMS hors forfait", depasse, pu, mt);
		return mt;
	}

	private double horsForfaitDataPrepaye(Contexte ctx, Facture f) {
		if (ctx.dataMo <= 0) {
			return 0;
		}
		// pas de blocage
		int forfaitMo = (ctx.offre != null ? ctx.offre.getDataMo() : 0);
		if (forfaitMo < 0) {
			return 0;
		}
		double depasse = Math.ceil(ctx.dataMo - (ctx.offre != null ? ctx.offre.getDataMo() : 0));
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixMo() / (1 + TAUX_TVA / 100);
		double mt = MontantUtil.arrondi(depasse * pu);
		String lib = "Internet mobile hors forfait (" + (long) depasse + " Mo)";
		// pas de plafond
		ligne(f, L_HF_DATA, lib, depasse, pu, mt);
		return mt;
	}

	private double internationalPrepaye(Contexte ctx, Facture f) {
		double total = 0;
		for (int i = 0; i < ZONES.length; i++) {
			String z = ZONES[i];
			Double q = ctx.inter.get(z);
			if (q == null || q.doubleValue() <= 0) {
				continue;
			}
			double pu = ctx.tarif.getPrixMinInter() * coefZone(z) / (1 + TAUX_TVA / 100);
			double mt = MontantUtil.arrondi(Math.ceil(q.doubleValue()) * pu);
			ligne(f, L_INTER, "Appels internationaux " + z.toLowerCase(), Math.ceil(q.doubleValue()), pu, mt);
			total += mt;
		}
		return total;
	}

	private double roamingPrepaye(Contexte ctx, Facture f) {
		double total = 0;
		Iterator<String> it = ctx.roaming.keySet().iterator();
		while (it.hasNext()) {
			String z = it.next();
			double q = ctx.roaming.get(z).doubleValue();
			double pu;
			if ("EUROPE".equals(z)) {
				pu = ROAMING_EUROPE;
			} else if ("GOLFE".equals(z)) {
				pu = ROAMING_GOLFE;
			} else if ("AFRIQUE".equals(z)) {
				pu = ROAMING_AFRIQUE;
			} else {
				pu = ROAMING_AUTRE;
			}
			pu = pu * 1.5;
			if (ctx.annee < 2017) {
				// ancienne grille (x2)
				pu = pu * 2;
			}
			double mt = MontantUtil.arrondi(q * pu);
			ligne(f, L_ROAMING, "Internet en itinerance " + z.toLowerCase() + " (" + (long) q + " Mo)", q, pu, mt);
			total += mt;
		}
		return total;
	}

	// ---------------------------------------------------------------- hors forfait Entreprise

	private double horsForfaitVoixEntreprise(Contexte ctx, Facture f) {
		if (ctx.minutesNat <= 0) {
			return 0;
		}
		// pas de blocage
		int forfaitMin = (ctx.offre != null ? ctx.offre.getMinutes() : ctx.tarif.getForfaitMinutes());
		if (forfaitMin < 0) {
			// illimite
			return 0;
		}
		double inclus = (ctx.offre != null ? ctx.offre.getMinutes() : ctx.tarif.getForfaitMinutes());
		double depasse = Math.ceil(ctx.minutesNat - inclus);
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixMinNat();
		double mt = MontantUtil.arrondi(depasse * pu);
		ligne(f, L_HF_VOIX, "Communications nationales hors forfait", depasse, pu, mt);
		return mt;
	}

	private double horsForfaitSmsEntreprise(Contexte ctx, Facture f) {
		if (ctx.sms <= 0) {
			return 0;
		}
		// pas de blocage
		int forfaitSms = (ctx.offre != null ? ctx.offre.getNbSms() : ctx.tarif.getForfaitSms());
		if (forfaitSms < 0) {
			return 0;
		}
		double depasse = Math.ceil(ctx.sms - (ctx.offre != null ? ctx.offre.getNbSms() : ctx.tarif.getForfaitSms()));
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixSms();
		double mt = MontantUtil.arrondi(depasse * pu);
		ligne(f, L_HF_SMS, "SMS hors forfait", depasse, pu, mt);
		return mt;
	}

	private double horsForfaitDataEntreprise(Contexte ctx, Facture f) {
		if (ctx.dataMo <= 0) {
			return 0;
		}
		// pas de blocage
		int forfaitMo = (ctx.offre != null ? ctx.offre.getDataMo() : ctx.tarif.getForfaitMo());
		if (forfaitMo < 0) {
			return 0;
		}
		double depasse = Math.ceil(ctx.dataMo - (ctx.offre != null ? ctx.offre.getDataMo() : ctx.tarif.getForfaitMo()));
		if (depasse <= 0) {
			return 0;
		}
		double pu = ctx.tarif.getPrixMo();
		double mt = MontantUtil.arrondi(depasse * pu);
		String lib = "Internet mobile hors forfait (" + (long) depasse + " Mo)";
		if (mt > PLAFOND_DATA_ENTREPRISE) {
			mt = PLAFOND_DATA_ENTREPRISE;
			lib = lib + " (plafond contrat cadre)";
		}
		ligne(f, L_HF_DATA, lib, depasse, pu, mt);
		return mt;
	}

	private double internationalEntreprise(Contexte ctx, Facture f) {
		double total = 0;
		for (int i = 0; i < ZONES.length; i++) {
			String z = ZONES[i];
			Double q = ctx.inter.get(z);
			if (q == null || q.doubleValue() <= 0) {
				continue;
			}
			double pu = ctx.tarif.getPrixMinInter() * coefZone(z) / (1 + TAUX_TVA / 100);
			if ("EUROPE".equals(z) && ctx.annee >= 2015) {
				// accord grands comptes 2015 : -10% sur l'Europe
				pu = pu * 0.9;
			}
			double mt = MontantUtil.arrondi(Math.ceil(q.doubleValue()) * pu);
			ligne(f, L_INTER, "Appels internationaux " + z.toLowerCase(), Math.ceil(q.doubleValue()), pu, mt);
			total += mt;
		}
		return total;
	}

	private double roamingEntreprise(Contexte ctx, Facture f) {
		double total = 0;
		Iterator<String> it = ctx.roaming.keySet().iterator();
		while (it.hasNext()) {
			String z = it.next();
			double q = ctx.roaming.get(z).doubleValue();
			double pu;
			if ("EUROPE".equals(z)) {
				pu = ROAMING_EUROPE;
			} else if ("GOLFE".equals(z)) {
				pu = ROAMING_GOLFE;
			} else if ("AFRIQUE".equals(z)) {
				pu = ROAMING_AFRIQUE;
			} else {
				pu = ROAMING_AUTRE;
			}
			pu = pu * 1.0;
			if (ctx.annee < 2017) {
				// ancienne grille (x2)
				pu = pu * 2;
			}
			double mt = MontantUtil.arrondi(q * pu);
			ligne(f, L_ROAMING, "Internet en itinerance " + z.toLowerCase() + " (" + (long) q + " Mo)", q, pu, mt);
			total += mt;
		}
		if (ctx.annee >= 2017 && total > PLAFOND_ROAMING) {
			// plafond reglementaire, on ajoute une ligne negative
			ligne(f, L_ROAMING, "Plafonnement itinerance", 1, -(total - PLAFOND_ROAMING), -(total - PLAFOND_ROAMING));
			total = PLAFOND_ROAMING;
		}
		return total;
	}

	// =====================================================================
	// promotions
	// =====================================================================

	/**
	 * Promotions particuliers. Le code promo est saisi en boutique a la souscription
	 * (ABONNEMENT.CODE_PROMO). Une seule promo par contrat.
	 */
	private double appliquerPromotions(Contexte ctx, Facture f) {
		String code = ctx.abo.getCodePromo();
		if (code == null || code.trim().length() == 0) {
			return 0;
		}
		code = code.trim().toUpperCase();
		double remise = 0;
		if ("BIENVENUE".equals(code)) {
			remise = promoBienvenue(ctx, f);
		} else if ("FIDELITE".equals(code)) {
			remise = promoFidelite(ctx, f);
		} else if ("RAMADAN".equals(code) || code.startsWith("RAMADAN")) {
			remise = promoRamadan(ctx, f);
		} else if ("ETE2015".equals(code)) {
			remise = promoEte2015(ctx, f);
		} else if ("PARRAINAGE".equals(code)) {
			remise = promoParrainage(ctx, f);
		} else if ("NOEL2012".equals(code)) {
			remise = promoNoel2012(ctx, f);
		} else {
			log.info("code promo inconnu " + code + " contrat " + ctx.abo.getNumContrat());
		}
		if (remise != 0) {
			ctx.trace.append(" promo=").append(code).append(":").append(remise);
		}
		return remise;
	}

	/**
	 * Meme chose pour les entreprises mais seules certaines promos sont acceptees.
	 */
	private double appliquerPromotionsEntreprise(Contexte ctx, Facture f) {
		String code = ctx.abo.getCodePromo();
		if (code == null || code.trim().length() == 0) {
			return 0;
		}
		code = code.trim().toUpperCase();
		double remise = 0;
		if ("BIENVENUE".equals(code)) {
			// entreprises : bienvenue = 1er mois a 50% seulement
			if (DateUtil.moisEntre(ctx.abo.getDateDebut(), ctx.finMois) == 0) {
				double base = totalLignes(f, L_FORFAIT);
				remise = MontantUtil.arrondi(base * 0.5);
				ligne(f, L_PROMO, "Offre de bienvenue entreprise -50% (1er mois)", 1, -remise, -remise);
			}
		} else if ("FIDELITE".equals(code)) {
			// pas de promo fidelite pour les entreprises, la remise flotte s'applique deja
			log.debug("promo FIDELITE ignoree pour entreprise " + ctx.abo.getNumContrat());
		} else {
			log.info("code promo non applicable aux entreprises " + code + " contrat " + ctx.abo.getNumContrat());
		}
		return remise;
	}

	/** BIENVENUE : -50% sur le forfait les 3 premiers mois (depuis 2014, avant : 1 mois offert) */
	private double promoBienvenue(Contexte ctx, Facture f) {
		int n = DateUtil.moisEntre(ctx.abo.getDateDebut(), ctx.finMois);
		double base = totalLignes(f, L_FORFAIT);
		if (base <= 0) {
			return 0;
		}
		Date debut = ctx.abo.getDateDebut();
		if (debut != null && DateUtil.annee(debut) < 2014) {
			if (n == 0) {
				ligne(f, L_PROMO, "Offre de bienvenue : 1er mois offert", 1, -base, -base);
				return base;
			}
			return 0;
		}
		if (n < 3) {
			double r = MontantUtil.arrondi(base * 0.5);
			ligne(f, L_PROMO, "Offre de bienvenue -50% (mois " + (n + 1) + "/3)", 1, -r, -r);
			return r;
		}
		return 0;
	}

	/** FIDELITE : 10% sur le forfait pour les clients de plus de 5 ans (reengagement) */
	private double promoFidelite(Contexte ctx, Facture f) {
		if (ctx.ancienneteMois < SEUIL_FIDELITE_2 && ctx.annee >= 2016) {
			return 0;
		}
		double base = totalLignes(f, L_FORFAIT);
		double r = MontantUtil.arrondi(base * 0.10);
		if (r > 0) {
			ligne(f, L_PROMO, "Promotion reengagement fidelite -10%", 1, -r, -r);
		}
		return r;
	}

	/**
	 * RAMADAN : data doublee + 20% sur le hors forfait voix pendant le mois sacre.
	 * Les dates changent chaque annee, en dur avant 2016 puis dans PARAMETRE.
	 */
	private double promoRamadan(Contexte ctx, Facture f) {
		// TODO nadia 2015-05 : dates a mettre en parametre pour 2016 (fait pour >= 2016)
		Date debut = null;
		Date fin = null;
		switch (ctx.annee) {
		case 2012:
			debut = DateUtil.lire("2012-07-20", DateUtil.FMT_SQL);
			fin = DateUtil.lire("2012-08-18", DateUtil.FMT_SQL);
			break;
		case 2013:
			debut = DateUtil.lire("2013-07-09", DateUtil.FMT_SQL);
			fin = DateUtil.lire("2013-08-07", DateUtil.FMT_SQL);
			break;
		case 2014:
			debut = DateUtil.lire("2014-06-29", DateUtil.FMT_SQL);
			fin = DateUtil.lire("2014-07-28", DateUtil.FMT_SQL);
			break;
		case 2015:
			debut = DateUtil.lire("2015-06-18", DateUtil.FMT_SQL);
			fin = DateUtil.lire("2015-07-17", DateUtil.FMT_SQL);
			break;
		default:
			debut = DateUtil.lire(paramDao.lire("PROMO_RAMADAN_DEBUT", ""), DateUtil.FMT_SQL);
			fin = DateUtil.lire(paramDao.lire("PROMO_RAMADAN_FIN", ""), DateUtil.FMT_SQL);
		}
		if (debut == null || fin == null) {
			return 0;
		}
		if (fin.before(ctx.debutMois) || debut.after(ctx.finMois)) {
			return 0;
		}
		double hf = totalLignes(f, L_HF_VOIX);
		double r = MontantUtil.arrondi(hf * 0.20);
		if (r > 0) {
			ligne(f, L_PROMO, "Ramadan Karim : -20% sur les appels hors forfait", 1, -r, -r);
		}
		ligne(f, L_BONUS, "Ramadan Karim : Internet double", 1, 0, 0);
		return r;
	}

	/** ETE2015 : 2 Go offerts juillet / aout 2015 - le hors forfait data est rembourse dans la limite de 2 Go */
	private double promoEte2015(Contexte ctx, Facture f) {
		if (ctx.annee != 2015 || (ctx.moisNum != 7 && ctx.moisNum != 8)) {
			return 0;
		}
		double hf = totalLignes(f, L_HF_DATA);
		double maxi = 2000 * ctx.tarif.getPrixMo();
		double r = MontantUtil.arrondi(Math.min(hf, maxi));
		if (r > 0) {
			ligne(f, L_PROMO, "Ete 2015 : 2 Go offerts", 1, -r, -r);
		}
		return r;
	}

	/** PARRAINAGE : 30 Dh HT de remise par mois pendant 6 mois */
	private double promoParrainage(Contexte ctx, Facture f) {
		int n = DateUtil.moisEntre(ctx.abo.getDateDebut(), ctx.finMois);
		if (n >= 6) {
			return 0;
		}
		double base = totalLignes(f, L_FORFAIT);
		double r = Math.min(30.0, base);
		if (r > 0) {
			ligne(f, L_PROMO, "Parrainage : -30 Dh (" + (n + 1) + "/6)", 1, -r, -r);
		}
		return r;
	}

	/** NOEL2012 : jamais lancee (annulee par la direction), garde au cas ou */
	private double promoNoel2012(Contexte ctx, Facture f) {
		if (ctx.annee != 2012 || ctx.moisNum != 12) {
			return 0;
		}
		double sms = totalLignes(f, L_HF_SMS);
		if (sms > 0) {
			ligne(f, L_PROMO, "SMS offerts pour les fetes", 1, -sms, -sms);
		}
		return sms;
	}

	// =====================================================================
	// remises
	// =====================================================================

	/**
	 * Remise fidelite particuliers (sur forfait uniquement) :
	 * plus de 24 mois : 5%, plus de 60 mois : 10%.
	 * Pas cumulable avec une promo FIDELITE ou BIENVENUE.
	 */
	private double remiseFidelite(Contexte ctx, Facture f) {
		String promo = ctx.abo.getCodePromo();
		if ("FIDELITE".equals(promo) || "BIENVENUE".equals(promo)) {
			return 0;
		}
		if (ctx.annee < 2013) {
			return 0;
		}
		double taux = 0;
		if (ctx.ancienneteMois >= SEUIL_FIDELITE_2) {
			taux = 0.10;
		} else if (ctx.ancienneteMois >= SEUIL_FIDELITE_1) {
			taux = 0.05;
		}
		// 2013-2014 : 5% seulement, quelle que soit l'anciennete
		if (ctx.annee <= 2014 && taux > 0.05) {
			taux = 0.05;
		}
		if (taux == 0) {
			return 0;
		}
		double base = totalLignes(f, L_FORFAIT);
		double r = MontantUtil.arrondi(base * taux);
		if (r > 0) {
			ligne(f, L_REMISE, "Remise fidelite " + Math.round(taux * 100) + "% (" + ctx.ancienneteMois / 12 + " ans)", 1, -r, -r);
		}
		return r;
	}

	/**
	 * Remise flotte entreprises selon le nombre de lignes actives du client.
	 */
	private double remiseFlotte(Contexte ctx, Facture f) {
		int n = ctx.nbLignes;
		double taux = 0;
		if (n >= 20) {
			taux = 0.15;
		} else if (n >= 10) {
			taux = 0.10;
		} else if (n >= 5) {
			taux = 0.05;
		}
		if ("EN-M2M10".equals(ctx.abo.getCodeOffre())) {
			// pas de remise sur le M2M
			taux = 0;
		}
		if (taux == 0) {
			return 0;
		}
		double base = totalLignes(f, L_FORFAIT);
		double r = MontantUtil.arrondi(base * taux);
		ligne(f, L_REMISE, "Remise flotte " + n + " lignes (" + Math.round(taux * 100) + "%)", 1, -r, -r);
		return r;
	}

	/** ancienne remise 2012 (avant la grille fidelite) */
	private double remiseAnciennete2012(Contexte ctx, Facture f) {
		if (ctx.ancienneteMois < 12) {
			return 0;
		}
		double base = totalLignes(f, L_FORFAIT);
		double r = MontantUtil.arrondi(Math.min(base * 0.03, 10.0));
		ligne(f, L_REMISE, "Remise anciennete", 1, -r, -r);
		return r;
	}

	// =====================================================================
	// variantes par offre (traitements a part demandes par le marketing)
	// =====================================================================

	/**
	 * Forfait bloque : copie de calculerPostpaye sans hors forfait national
	 * (le client recharge), mais l'international et l'itinerance restent factures.
	 * K.S. 09/2015
	 */
	private Facture calculerPostpayeBloque(Contexte ctx) {
		Facture f = nouvelleFacture(ctx, DOC_FACTURE);
		String libOffre = ctx.tarif.getLibelleOffre() != null ? ctx.tarif.getLibelleOffre() : ctx.abo.getCodeOffre();
		double forfait = montantForfait(ctx);
		if (ctx.abo.getAncienneOffre() != null && DateUtil.memeMois(ctx.abo.getDateChgtOffre(), ctx.mois)) {
			forfait = ligneChangementOffre(ctx, f, forfait);
		} else if (ctx.prorata < 1.0) {
			double p = MontantUtil.arrondi(forfait * ctx.prorata);
			ligne(f, L_FORFAIT, "Forfait " + libOffre + " - " + libelleMois(ctx.mois) + " (prorata " + Math.round(ctx.prorata * ctx.nbJours) + " j)", ctx.prorata, forfait, p);
		} else {
			ligne(f, L_FORFAIT, "Forfait " + libOffre + " - " + libelleMois(ctx.mois), 1, forfait, forfait);
		}
		if (ctx.minutesNat > ctx.tarif.getForfaitMinutes() && ctx.tarif.getForfaitMinutes() > 0) {
			// information seulement
			ligne(f, L_BONUS, "Recharges consommees : " + (long) (ctx.minutesNat - ctx.tarif.getForfaitMinutes()) + " min (payees a la recharge)", 1, 0, 0);
		}
		internationalPostpaye(ctx, f);
		roamingPostpaye(ctx, f);
		appliquerPromotions(ctx, f);
		remiseFidelite(ctx, f);
		ajustementsAnnuels(ctx, f);
		if (ctx.abo.getDateResiliation() != null && DateUtil.memeMois(ctx.abo.getDateResiliation(), ctx.mois)) {
			penaliteResiliation(ctx, f);
		}
		calculerTotaux(ctx, f);
		return f;
	}

	/**
	 * M2M : pas de prorata, pas de promo, data facturee au Ko au-dela de 50 Mo, TVA selon zone.
	 * Copie simplifiee de calculerEntreprise (2017).
	 */
	private Facture calculerM2M(Contexte ctx) {
		Facture f = nouvelleFacture(ctx, DOC_FACTURE);
		double forfait = montantForfait(ctx);
		ligne(f, L_FORFAIT, "Carte M2M " + ctx.abo.getMsisdn() + " - " + libelleMois(ctx.mois), 1, forfait, forfait);
		int inclus = ctx.offre != null ? ctx.offre.getDataMo() : 50;
		if (ctx.dataMo > inclus) {
			double depasse = ctx.dataMo - inclus;
			double pu = ctx.tarif.getPrixMo();
			double mt = MontantUtil.arrondi(depasse * pu);
			if (mt > 50) {
				mt = 50;
			}
			ligne(f, L_HF_DATA, "Donnees M2M au-dela du forfait (" + (long) depasse + " Mo)", depasse, pu, mt);
		}
		if (ctx.sms > 0) {
			horsForfaitSmsEntreprise(ctx, f);
		}
		if (!ctx.roaming.isEmpty()) {
			roamingEntreprise(ctx, f);
		}
		if ("SUSPENDU".equals(ctx.abo.getStatut())) {
			// carte suspendue : forfait reduit de moitie (contrat cadre M2M)
			double r = MontantUtil.arrondi(forfait * 0.5);
			ligne(f, L_REMISE, "Carte suspendue", 1, -r, -r);
		}
		calculerTotauxEntreprise(ctx, f);
		return f;
	}

	/**
	 * Salam : uniquement un releve de consommation valorisee, rien si aucune conso.
	 */
	private Facture calculerPrepayeSalam(Contexte ctx) {
		if (ctx.minutesNat <= 0 && ctx.sms <= 0 && ctx.dataMo <= 0 && ctx.inter.isEmpty()) {
			return null;
		}
		Facture f = nouvelleFacture(ctx, DOC_RELEVE);
		horsForfaitVoixPrepaye(ctx, f);
		horsForfaitSmsPrepaye(ctx, f);
		horsForfaitDataPrepaye(ctx, f);
		internationalPrepaye(ctx, f);
		if (f.getLignes().size() == 0) {
			return null;
		}
		calculerTotauxReleve(ctx, f);
		return f;
	}

	// =====================================================================
	// totaux, taxes
	// =====================================================================

	private void calculerTotaux(Contexte ctx, Facture f) {
		double ht = 0;
		double remise = 0;
		for (int i = 0; i < f.getLignes().size(); i++) {
			LigneFacture l = f.getLignes().get(i);
			if (l.getMontantHt() < 0) {
				remise += -l.getMontantHt();
			} else {
				ht += l.getMontantHt();
			}
		}
		ht = MontantUtil.arrondi(ht);
		remise = MontantUtil.arrondi(remise);
		double base = ht - remise;
		if (base < 0) {
			log.warn("base negative " + base + " contrat " + ctx.abo.getNumContrat() + ", ramenee a 0");
			base = 0;
		}
		double tva = calculerTva(ctx, f, base);
		double timbre = calculerTimbre(ctx, base + tva);
		double ttc = base + tva + timbre;
		ttc = arrondiParticulier(ctx, ttc);
		f.setMontantHt(ht);
		f.setMontantRemise(remise);
		f.setMontantTva(tva);
		f.setDroitTimbre(timbre);
		f.setMontantTtc(ttc);
	}

	// copie de calculerTotaux pour les entreprises (pas de timbre, arrondi au dixieme pour les anciens clients)
	private void calculerTotauxEntreprise(Contexte ctx, Facture f) {
		double ht = 0;
		double remise = 0;
		for (int i = 0; i < f.getLignes().size(); i++) {
			LigneFacture l = f.getLignes().get(i);
			if (l.getMontantHt() < 0) {
				remise += -l.getMontantHt();
			} else {
				ht += l.getMontantHt();
			}
		}
		ht = MontantUtil.arrondi(ht);
		remise = MontantUtil.arrondi(remise);
		double base = ht - remise;
		if (base < 0) {
			base = 0;
		}
		double tva = calculerTva(ctx, f, base);
		double timbre = 0;
		if ("ESPECES".equals(ctx.abo.getModePaiement())) {
			// ne devrait pas arriver (contrat cadre = virement ou cheque)
			timbre = calculerTimbre(ctx, base + tva);
		}
		double ttc = base + tva + timbre;
		Date creation = ctx.client.getDateCreation();
		if (creation != null && DateUtil.annee(creation) < 2016) {
			ttc = arrondiDixieme(ttc);
		} else {
			ttc = arrondiCentime(ttc);
		}
		f.setMontantHt(ht);
		f.setMontantRemise(remise);
		f.setMontantTva(tva);
		f.setDroitTimbre(timbre);
		f.setMontantTtc(ttc);
	}

	// releve prepaye : meme calcul, sans timbre ni remise
	private void calculerTotauxReleve(Contexte ctx, Facture f) {
		double ht = 0;
		for (int i = 0; i < f.getLignes().size(); i++) {
			ht += f.getLignes().get(i).getMontantHt();
		}
		ht = MontantUtil.arrondi(ht);
		double tva = MontantUtil.arrondi(ht * TAUX_TVA / 100);
		f.setMontantHt(ht);
		f.setMontantRemise(0);
		f.setMontantTva(tva);
		f.setDroitTimbre(0);
		f.setMontantTtc(arrondiCentime(ht + tva));
	}

	/**
	 * TVA telecom 20%.
	 * Cas particuliers : l'itinerance n'etait pas soumise a TVA avant 2014 (consommee hors Maroc),
	 * et les lignes M2M des zones franches (Tanger) sont exonerees depuis 2018.
	 */
	double calculerTva(Contexte ctx, Facture f, double base) {
		double exonere = 0;
		for (int i = 0; i < f.getLignes().size(); i++) {
			LigneFacture l = f.getLignes().get(i);
			if (L_ROAMING.equals(l.getCodeLigne()) && ctx.annee < 2014) {
				exonere += l.getMontantHt();
				l.setTauxTva(0);
			}
		}
		if ("EN-M2M10".equals(ctx.abo.getCodeOffre()) && ctx.annee >= 2018 && zoneFranche(ctx)) {
			for (int i = 0; i < f.getLignes().size(); i++) {
				f.getLignes().get(i).setTauxTva(0);
			}
			return 0;
		}
		double taux = tauxTva(ctx.annee);
		double tva = (base - exonere) * taux / 100;
		if (tva < 0) {
			tva = 0;
		}
		return MontantUtil.arrondi(tva);
	}

	private double tauxTva(int annee) {
		String p = null;
		try {
			p = paramDao.lire("TVA_TELECOM");
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (p != null && annee >= 2016) {
			try {
				return Double.parseDouble(p);
			} catch (NumberFormatException e) {
				log.error("TVA_TELECOM invalide : " + p);
			}
		}
		if (annee <= 2012) {
			return TAUX_TVA_2012;
		}
		return TAUX_TVA;
	}

	private boolean zoneFranche(Contexte ctx) {
		String ville = ctx.client.getVille();
		String adr = ctx.client.getAdresse();
		if (ville == null) {
			return false;
		}
		if ("Tanger".equalsIgnoreCase(ville) && adr != null && adr.toLowerCase().indexOf("zone franche") >= 0) {
			return true;
		}
		return false;
	}

	/**
	 * Droit de timbre sur les paiements en especes en boutique : 0,25% du TTC (depuis 2014).
	 * Avant 2014 : 0,20 Dh fixe par facture.
	 */
	double calculerTimbre(Contexte ctx, double ttc) {
		String mode = ctx.abo.getModePaiement();
		if (!"ESPECES".equals(mode)) {
			return 0;
		}
		if (ctx.annee < 2014) {
			return 0.20;
		}
		double t = ttc * TAUX_TIMBRE / 100;
		if (t < TIMBRE_MINIMUM) {
			t = TIMBRE_MINIMUM;
		}
		return MontantUtil.arrondi(t);
	}

	// =====================================================================
	// arrondis
	// =====================================================================

	/**
	 * Particuliers : au centime, sauf paiement especes en boutique depuis 2016 ou on arrondit
	 * aux 10 centimes superieurs (plus de pieces de 5 cts dans les caisses).
	 */
	private double arrondiParticulier(Contexte ctx, double v) {
		if ("ESPECES".equals(ctx.abo.getModePaiement()) && ctx.annee >= 2016) {
			return arrondiCaisse(v);
		}
		return arrondiCentime(v);
	}

	double arrondiCentime(double v) {
		return MontantUtil.arrondi(v);
	}

	double arrondiDixieme(double v) {
		return MontantUtil.arrondi(v, 1);
	}

	double arrondiCaisse(double v) {
		double c = Math.ceil(MontantUtil.arrondi(v) * 10 - 0.0001) / 10.0;
		return MontantUtil.arrondi(c);
	}

	double arrondiSuperieur(double v) {
		return Math.ceil(v);
	}

	// arrondi bancaire (demande de la compta en 2013, jamais branche)
	double arrondiBanque(double v) {
		return new java.math.BigDecimal(String.valueOf(v)).setScale(2, java.math.BigDecimal.ROUND_HALF_EVEN).doubleValue();
	}

	// =====================================================================
	// controles
	// =====================================================================

	/**
	 * Verifications avant enregistrement. On ne bloque pas, on trace.
	 */
	private void controlerFacture(Contexte ctx, Facture f) {
		double somme = 0;
		for (int i = 0; i < f.getLignes().size(); i++) {
			somme += f.getLignes().get(i).getMontantHt();
		}
		somme = MontantUtil.arrondi(somme);
		double attendu = MontantUtil.arrondi(f.getMontantHt() - f.getMontantRemise());
		if (Math.abs(somme - attendu) > 0.05 && attendu > 0) {
			log.warn("ecart lignes / total " + somme + " / " + attendu + " contrat " + ctx.abo.getNumContrat());
		}
		if (f.getMontantTtc() > 5000 && !DOC_RELEVE.equals(f.getTypeDoc())) {
			log.warn("facture elevee " + f.getMontantTtc() + " DH contrat " + ctx.abo.getNumContrat() + " " + ctx.trace);
		}
		if (f.getMontantTtc() < 0) {
			log.error("facture negative contrat " + ctx.abo.getNumContrat());
			f.setMontantTtc(0);
		}
		if (ctx.offre != null && ctx.tarif != null && ctx.tarif.getLibelleOffre() != null
				&& !ctx.tarif.getLibelleOffre().equals(ctx.offre.getLibelle())) {
			log.debug("libelle different OFFRE / OFFRE_TARIF pour " + ctx.offre.getCodeOffre());
		}
	}

	private double totalLignes(Facture f, String code) {
		double t = 0;
		for (int i = 0; i < f.getLignes().size(); i++) {
			LigneFacture l = f.getLignes().get(i);
			if (code.equals(l.getCodeLigne()) && l.getMontantHt() > 0) {
				t += l.getMontantHt();
			}
		}
		return MontantUtil.arrondi(t);
	}

	// =====================================================================
	// avoirs, refacturation
	// =====================================================================

	/**
	 * Avoir total sur une facture (reclamation acceptee). Montants negatifs, meme mois.
	 * La facture d'origine passe ANNULEE.
	 */
	public Facture creerAvoir(Facture origine, String motif) {
		if (origine == null) {
			return null;
		}
		if (DOC_AVOIR.equals(origine.getTypeDoc())) {
			throw new IllegalArgumentException("on ne fait pas d'avoir sur un avoir");
		}
		Facture a = new Facture();
		a.setTypeDoc(DOC_AVOIR);
		a.setIdClient(origine.getIdClient());
		a.setIdAbonnement(origine.getIdAbonnement());
		a.setMois(origine.getMois());
		a.setDateEmission(new Date());
		a.setDateEcheance(new Date());
		a.setStatut("EMISE");
		List<LigneFacture> lignes = origine.getLignes();
		if (lignes == null || lignes.isEmpty()) {
			lignes = factureDao.lignes(origine.getIdFacture());
		}
		for (int i = 0; i < lignes.size(); i++) {
			LigneFacture l = lignes.get(i);
			LigneFacture n = new LigneFacture(l.getCodeLigne(), "Avoir : " + l.getLibelle(), l.getQuantite(), l.getPuHt(), -l.getMontantHt());
			n.setTauxTva(l.getTauxTva());
			a.ajouterLigne(n);
		}
		if (motif != null) {
			a.ajouterLigne(new LigneFacture("INFO", "Motif : " + motif, 0, 0, 0));
		}
		a.setMontantHt(-origine.getMontantHt());
		a.setMontantRemise(-origine.getMontantRemise());
		a.setMontantTva(-origine.getMontantTva());
		a.setDroitTimbre(-origine.getDroitTimbre());
		a.setMontantTtc(-origine.getMontantTtc());
		return a;
	}

	/**
	 * Avoir partiel : on recalcule la facture du mois et on fait la difference avec l'existante.
	 * Utilise quand une consommation arrive en retard de la plateforme reseau.
	 */
	public Facture avoirDifferentiel(Facture origine, Abonnement abo) {
		Facture recalcul = simulerFacture(abo, origine.getMois());
		if (recalcul == null) {
			return null;
		}
		double ecart = MontantUtil.arrondi(recalcul.getMontantTtc() - origine.getMontantTtc());
		if (Math.abs(ecart) < 0.01) {
			return null;
		}
		Facture a = new Facture();
		a.setTypeDoc(ecart < 0 ? DOC_AVOIR : DOC_FACTURE);
		a.setIdClient(origine.getIdClient());
		a.setIdAbonnement(origine.getIdAbonnement());
		a.setMois(origine.getMois());
		a.setDateEmission(new Date());
		a.setDateEcheance(DateUtil.ajouterJours(new Date(), delaiEcheance));
		a.setStatut("EMISE");
		double ht = MontantUtil.arrondi(recalcul.getMontantHt() - recalcul.getMontantRemise() - origine.getMontantHt() + origine.getMontantRemise());
		a.ajouterLigne(new LigneFacture("REGUL", "Regularisation " + libelleMois(origine.getMois()), 1, ht, ht));
		a.setMontantHt(ht);
		a.setMontantTva(MontantUtil.arrondi(recalcul.getMontantTva() - origine.getMontantTva()));
		a.setMontantTtc(ecart);
		return a;
	}

	/**
	 * Facture de cloture a la resiliation (solde de tout compte) : forfait au prorata + penalites.
	 * Appelee par le WS resilierAbonnement, en direct.
	 */
	public Facture factureCloture(Abonnement abo) {
		String mois = DateUtil.format(abo.getDateResiliation() != null ? abo.getDateResiliation() : new Date(), DateUtil.FMT_MOIS);
		Contexte ctx = preparer(abo, mois, true);
		if (ctx == null) {
			return null;
		}
		String type = typeOffre(ctx);
		Facture f;
		if (TYPE_PREPAYE.equals(type)) {
			return null;
		} else if (TYPE_ENTREPRISE.equals(type)) {
			f = calculerEntreprise(ctx);
		} else {
			f = calculerPostpaye(ctx);
		}
		f.setTypeDoc(DOC_FACTURE);
		f.setDateEcheance(DateUtil.ajouterJours(new Date(), 8));
		return f;
	}

	// =====================================================================
	// affichage
	// =====================================================================

	private static final String[] MOIS_FR = { "janvier", "fevrier", "mars", "avril", "mai", "juin", "juillet", "aout", "septembre", "octobre",
			"novembre", "decembre" };

	public static String libelleMois(String mois) {
		try {
			int m = Integer.parseInt(mois.substring(5, 7));
			return MOIS_FR[m - 1] + " " + mois.substring(0, 4);
		} catch (Exception e) {
			return mois;
		}
	}

	/**
	 * Texte de la facture pour le ticket boutique (imprimante 40 colonnes).
	 */
	public String ticket(Facture f, Client c, Abonnement a) {
		StringBuffer sb = new StringBuffer();
		sb.append("        TELECOM ATLAS MOBILE\n");
		sb.append("   Av. Annakhil, Hay Riad - Rabat\n");
		sb.append("----------------------------------------\n");
		sb.append(f.getTypeDoc()).append(" ").append(f.getNumFacture() == null ? "(simulation)" : f.getNumFacture()).append("\n");
		sb.append("Periode : ").append(libelleMois(f.getMois())).append("\n");
		if (c != null) {
			sb.append("Client  : ").append(couper(c.getNomComplet(), 30)).append("\n");
		}
		if (a != null) {
			sb.append("Ligne   : ").append(a.getMsisdn()).append("  Contrat ").append(a.getNumContrat()).append("\n");
		}
		sb.append("----------------------------------------\n");
		for (int i = 0; i < f.getLignes().size(); i++) {
			LigneFacture l = f.getLignes().get(i);
			String mt = MontantUtil.texte(l.getMontantHt());
			String lib = couper(l.getLibelle(), 39 - mt.length() - 1);
			sb.append(lib);
			for (int k = lib.length(); k < 40 - mt.length(); k++) {
				sb.append(' ');
			}
			sb.append(mt).append("\n");
		}
		sb.append("----------------------------------------\n");
		sb.append(colonne("Total HT", f.getMontantHt()));
		if (f.getMontantRemise() != 0) {
			sb.append(colonne("Remises", -f.getMontantRemise()));
		}
		sb.append(colonne("TVA " + (int) TAUX_TVA + "%", f.getMontantTva()));
		if (f.getDroitTimbre() != 0) {
			sb.append(colonne("Droit de timbre", f.getDroitTimbre()));
		}
		sb.append(colonne("TOTAL TTC (DH)", f.getMontantTtc()));
		if (f.getDateEcheance() != null && !DOC_RELEVE.equals(f.getTypeDoc())) {
			sb.append("A regler avant le ").append(DateUtil.versFr(f.getDateEcheance())).append("\n");
		}
		return sb.toString();
	}

	private String colonne(String lib, double v) {
		String mt = MontantUtil.texte(v);
		StringBuffer sb = new StringBuffer(lib);
		while (sb.length() < 40 - mt.length()) {
			sb.append(' ');
		}
		return sb.append(mt).append("\n").toString();
	}

	private String couper(String s, int n) {
		if (s == null) {
			return "";
		}
		return s.length() > n ? s.substring(0, n) : s;
	}

	/**
	 * Recapitulatif d'un lot pour le mail a la compta (copie colle dans Outlook).
	 */
	public String recapLot(String lot, List<Facture> factures) {
		int nbFact = 0;
		int nbReleve = 0;
		double ttc = 0;
		double tva = 0;
		double timbre = 0;
		Map<String, Double> parStatut = new HashMap<String, Double>();
		for (int i = 0; i < factures.size(); i++) {
			Facture f = factures.get(i);
			if (DOC_RELEVE.equals(f.getTypeDoc())) {
				nbReleve++;
				continue;
			}
			nbFact++;
			ttc += f.getMontantTtc();
			tva += f.getMontantTva();
			timbre += f.getDroitTimbre();
			cumuler(parStatut, f.getStatut(), f.getMontantTtc());
		}
		StringBuffer sb = new StringBuffer();
		sb.append("Lot ").append(lot).append(" du ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
		sb.append("Factures : ").append(nbFact).append("   Releves prepayes : ").append(nbReleve).append("\n");
		sb.append("Total TTC : ").append(MontantUtil.affichage(ttc)).append("\n");
		sb.append("dont TVA : ").append(MontantUtil.affichage(tva)).append("\n");
		sb.append("dont timbre : ").append(MontantUtil.affichage(timbre)).append("\n");
		Iterator<String> it = parStatut.keySet().iterator();
		while (it.hasNext()) {
			String s = it.next();
			sb.append("  ").append(s).append(" : ").append(MontantUtil.affichage(parStatut.get(s).doubleValue())).append("\n");
		}
		return sb.toString();
	}

	// =====================================================================
	// factures groupees entreprises (synthese par client)
	// =====================================================================

	/**
	 * Synthese mensuelle d'un client entreprise : une ligne par abonnement + totaux.
	 * Demande du service grands comptes (2015) : la facture "officielle" reste une par ligne,
	 * la synthese est envoyee en plus au DAF du client. Non enregistree en base.
	 */
	public Facture syntheseEntreprise(int idClient, String mois) {
		Client c = clientDao.trouver(idClient);
		if (c == null || !c.isEntreprise()) {
			return null;
		}
		List<Abonnement> abos = abonnementDao.listerParClient(idClient);
		Facture s = new Facture();
		s.setTypeDoc("SYNTHESE");
		s.setIdClient(idClient);
		s.setMois(mois);
		s.setDateEmission(new Date());
		s.setStatut("INFO");
		double ht = 0;
		double remise = 0;
		double tva = 0;
		double ttc = 0;
		int nb = 0;
		for (int i = 0; i < abos.size(); i++) {
			Abonnement a = abos.get(i);
			if ("RESILIE".equals(a.getStatut()) && a.getDateResiliation() != null && a.getDateResiliation().before(DateUtil.premierJourMois(mois))) {
				continue;
			}
			if ("EN_ATTENTE".equals(a.getStatut())) {
				continue;
			}
			Facture f = null;
			// on reprend la facture emise si elle existe, sinon simulation
			List<Facture> emises = factureDao.listerParAbonnement(a.getIdAbonnement());
			for (int k = 0; k < emises.size(); k++) {
				if (mois.equals(emises.get(k).getMois()) && !"ANNULEE".equals(emises.get(k).getStatut())) {
					f = emises.get(k);
					break;
				}
			}
			if (f == null) {
				f = simulerFacture(a, mois);
			}
			if (f == null) {
				continue;
			}
			nb++;
			LigneFacture l = new LigneFacture("LIGNE", a.getMsisdn() + " - " + a.getCodeOffre() + (f.getNumFacture() != null ? " (" + f.getNumFacture() + ")" : " (estimation)"),
					1, f.getMontantHt() - f.getMontantRemise(), MontantUtil.arrondi(f.getMontantHt() - f.getMontantRemise()));
			s.ajouterLigne(l);
			ht += f.getMontantHt();
			remise += f.getMontantRemise();
			tva += f.getMontantTva();
			ttc += f.getMontantTtc();
		}
		if (nb == 0) {
			return null;
		}
		s.setMontantHt(MontantUtil.arrondi(ht));
		s.setMontantRemise(MontantUtil.arrondi(remise));
		s.setMontantTva(MontantUtil.arrondi(tva));
		s.setMontantTtc(MontantUtil.arrondi(ttc));
		return s;
	}

	/**
	 * Estimation de la prochaine facture a partir de la moyenne des 3 derniers mois.
	 * (ecran fiche client et operation estimerFacture)
	 */
	public double estimationProchaineFacture(Abonnement abo) {
		List<Facture> l = factureDao.listerParAbonnement(abo.getIdAbonnement());
		double somme = 0;
		int n = 0;
		for (int i = 0; i < l.size() && n < 3; i++) {
			Facture f = l.get(i);
			if (DOC_FACTURE.equals(f.getTypeDoc()) && !"ANNULEE".equals(f.getStatut())) {
				somme += f.getMontantTtc();
				n++;
			}
		}
		if (n == 0) {
			// pas d'historique : prix de l'offre
			Offre o = offreDao.trouver(abo.getCodeOffre());
			if (o != null) {
				return o.getPrixMensuel();
			}
			OffreTarif t = offreDao.tarif(abo.getCodeOffre());
			return t == null ? 0 : t.getPrixTtc();
		}
		return MontantUtil.arrondi(somme / n);
	}

	// =====================================================================
	// coherence des referentiels
	// =====================================================================

	/**
	 * Compare OFFRE et OFFRE_TARIF (prix, libelles, offres manquantes).
	 * Lance a la main par l'operation verifierTarifs, le resultat part dans le log.
	 */
	public List<String> verifierTarifs() {
		List<String> anomalies = new ArrayList<String>();
		List<Map<String, Object>> offres = jdbcTemplate.queryForList("SELECT CODE_OFFRE, LIBELLE, PRIX_MENSUEL, TYPE_OFFRE, ACTIF FROM OFFRE");
		List<Map<String, Object>> tarifs = jdbcTemplate.queryForList("SELECT CODE_OFFRE, LIBELLE_OFFRE, PRIX_TTC, TYPE_OFFRE FROM OFFRE_TARIF WHERE DATE_FIN IS NULL");
		Map<String, Map<String, Object>> parCode = new HashMap<String, Map<String, Object>>();
		for (int i = 0; i < tarifs.size(); i++) {
			parCode.put((String) tarifs.get(i).get("CODE_OFFRE"), tarifs.get(i));
		}
		for (int i = 0; i < offres.size(); i++) {
			Map<String, Object> o = offres.get(i);
			String code = (String) o.get("CODE_OFFRE");
			Map<String, Object> t = parCode.remove(code);
			if (t == null) {
				anomalies.add(code + " : pas de tarif en cours dans OFFRE_TARIF");
				continue;
			}
			double p1 = ((Number) o.get("PRIX_MENSUEL")).doubleValue();
			double p2 = t.get("PRIX_TTC") == null ? 0 : ((Number) t.get("PRIX_TTC")).doubleValue();
			if (Math.abs(p1 - p2) > 0.001) {
				anomalies.add(code + " : prix OFFRE " + p1 + " / OFFRE_TARIF " + p2);
			}
			String l1 = (String) o.get("LIBELLE");
			String l2 = (String) t.get("LIBELLE_OFFRE");
			if (l1 != null && !l1.equals(l2)) {
				anomalies.add(code + " : libelle '" + l1 + "' / '" + l2 + "'");
			}
			if (!String.valueOf(o.get("TYPE_OFFRE")).equals(String.valueOf(t.get("TYPE_OFFRE")))) {
				anomalies.add(code + " : type different");
			}
		}
		Iterator<String> it = parCode.keySet().iterator();
		while (it.hasNext()) {
			anomalies.add(it.next() + " : present dans OFFRE_TARIF mais pas dans OFFRE");
		}
		for (int i = 0; i < anomalies.size(); i++) {
			log.warn("tarifs : " + anomalies.get(i));
		}
		return anomalies;
	}

	// =====================================================================
	// relances impayes (2016, en attente du module recouvrement)
	// =====================================================================

	/**
	 * Niveau de relance d'une facture : 0 rien, 1 SMS, 2 courrier, 3 suspension.
	 */
	public int niveauRelance(Facture f, Date aujourdhui) {
		if (f == null || !"EMISE".equals(f.getStatut()) && !"IMPAYEE".equals(f.getStatut())) {
			return 0;
		}
		if (f.getDateEcheance() == null) {
			return 0;
		}
		int retard = DateUtil.joursEntre(f.getDateEcheance(), aujourdhui);
		if (retard <= 0) {
			return 0;
		}
		if (retard <= 10) {
			return 1;
		}
		if (retard <= 30) {
			return 2;
		}
		double seuil = 300;
		try {
			seuil = Double.parseDouble(paramDao.lire("SEUIL_IMPAYE", "300"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (f.getMontantTtc() >= seuil) {
			return 3;
		}
		return 2;
	}

	public String messageRelance(Facture f, int niveau) {
		String mt = MontantUtil.texte(f.getMontantTtc());
		switch (niveau) {
		case 1:
			return "Telecom Atlas : votre facture " + f.getNumFacture() + " de " + mt + " DH est arrivee a echeance. Merci de la regler en boutique ou par virement.";
		case 2:
			return "Telecom Atlas : sauf erreur, la facture " + f.getNumFacture() + " (" + mt + " DH) reste impayee. Sans reglement sous 8 jours votre ligne pourra etre suspendue.";
		case 3:
			return "Telecom Atlas : votre ligne va etre suspendue pour impaye (" + mt + " DH). Contactez le 777.";
		default:
			return null;
		}
	}

	// =====================================================================
	// anciennes versions (2012-2013), plus appelees
	// =====================================================================

	/**
	 * Version 2 du calcul (2013) : tout dans une seule methode, avant la separation
	 * prepaye / postpaye / entreprise. Gardee pour comparer en cas de reclamation sur
	 * une facture 2013.
	 * @deprecated remplace par calculerFacture
	 */
	public Facture calculerFactureV2(int idAbonnement, String mois) {
		Map<String, Object> abo;
		try {
			abo = jdbcTemplate.queryForMap("SELECT a.*, c.TYPE_CLIENT, c.CLIENT_DEPUIS FROM ABONNEMENT a, CLIENT c WHERE a.ID_CLIENT = c.ID_CLIENT AND a.ID_ABONNEMENT = ?", idAbonnement);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		String codeOffre = (String) abo.get("CODE_OFFRE");
		String msisdn = (String) abo.get("MSISDN");
		String typeClient = (String) abo.get("TYPE_CLIENT");
		String modePaiement = (String) abo.get("MODE_PAIEMENT");

		List<Map<String, Object>> tarifs = jdbcTemplate.queryForList("SELECT * FROM OFFRE_TARIF WHERE CODE_OFFRE = ?", codeOffre);
		if (tarifs.isEmpty()) {
			System.out.println("V2 : pas de tarif pour " + codeOffre);
			return null;
		}
		Map<String, Object> t = tarifs.get(0);
		double prixHt = num(t.get("PRIX_HT"));
		double prixMin = num(t.get("PRIX_MIN_NAT"));
		double prixInter = num(t.get("PRIX_MIN_INTER"));
		double prixSms = num(t.get("PRIX_SMS"));
		double prixMo = num(t.get("PRIX_MO"));
		int fMin = (int) num(t.get("FORFAIT_MINUTES"));
		int fSms = (int) num(t.get("FORFAIT_SMS"));
		int fMo = (int) num(t.get("FORFAIT_MO"));

		Facture f = new Facture();
		f.setTypeDoc(DOC_FACTURE);
		f.setIdClient(((Number) abo.get("ID_CLIENT")).intValue());
		f.setIdAbonnement(idAbonnement);
		f.setMois(mois);
		f.setDateEmission(new Date());
		f.setDateEcheance(DateUtil.ajouterJours(new Date(), 15));
		f.setStatut("EMISE");

		if (codeOffre.startsWith("PP-")) {
			f.setTypeDoc(DOC_RELEVE);
			f.setStatut("INFO");
		}

		// forfait
		if (prixHt > 0) {
			f.ajouterLigne(new LigneFacture(L_FORFAIT, "Abonnement " + num(t.get("PRIX_TTC")) + " DH", 1, prixHt, prixHt));
		}

		List<Map<String, Object>> consos = jdbcTemplate.queryForList("SELECT TYPE_CONSO, ZONE, SUM(QUANTITE) AS Q FROM CONSOMMATION WHERE MSISDN = ? AND MOIS = ? GROUP BY TYPE_CONSO, ZONE",
				msisdn, mois);
		double hf = 0;
		for (int i = 0; i < consos.size(); i++) {
			Map<String, Object> c = consos.get(i);
			String type = (String) c.get("TYPE_CONSO");
			double q = num(c.get("Q"));
			if ("VOIX_NAT".equals(type)) {
				if (fMin >= 0 && q > fMin) {
					double mt = MontantUtil.arrondi((q - fMin) * prixMin / 1.2);
					f.ajouterLigne(new LigneFacture(L_HF_VOIX, "Depassement forfait voix", q - fMin, prixMin / 1.2, mt));
					hf += mt;
				}
			} else if ("SMS".equals(type)) {
				if (fSms >= 0 && q > fSms) {
					double mt = MontantUtil.arrondi((q - fSms) * prixSms / 1.2);
					f.ajouterLigne(new LigneFacture(L_HF_SMS, "Depassement SMS", q - fSms, prixSms / 1.2, mt));
					hf += mt;
				}
			} else if ("DATA".equals(type)) {
				if (fMo >= 0 && q > fMo) {
					double mt = MontantUtil.arrondi((q - fMo) * prixMo);
					f.ajouterLigne(new LigneFacture(L_HF_DATA, "Depassement internet", q - fMo, prixMo, mt));
					hf += mt;
				}
			} else if ("VOIX_INTER".equals(type)) {
				String z = zone((String) c.get("ZONE"));
				double pu = prixInter * coefZone(z) / 1.2;
				double mt = MontantUtil.arrondi(q * pu);
				f.ajouterLigne(new LigneFacture(L_INTER, "International " + z, q, pu, mt));
				hf += mt;
			} else if ("ROAMING".equals(type)) {
				double mt = MontantUtil.arrondi(q * 5.0);
				f.ajouterLigne(new LigneFacture(L_ROAMING, "Itinerance", q, 5.0, mt));
				hf += mt;
			}
		}

		double ht = prixHt + hf;
		double remise = 0;
		// remise entreprise 2013 : 10% sur tout
		if ("ENTR".equals(typeClient)) {
			remise = MontantUtil.arrondi(ht * 0.10);
			f.ajouterLigne(new LigneFacture(L_REMISE, "Remise entreprise", 1, -remise, -remise));
		}
		String promo = (String) abo.get("CODE_PROMO");
		if ("BIENVENUE".equals(promo)) {
			Date debut = DateUtil.depuisSql(String.valueOf(abo.get("DATE_DEBUT")));
			if (debut != null && DateUtil.memeMois(debut, mois)) {
				remise += prixHt;
				f.ajouterLigne(new LigneFacture(L_PROMO, "1er mois offert", 1, -prixHt, -prixHt));
			}
		}
		double base = ht - remise;
		double tva = MontantUtil.arrondi(base * 0.20);
		double timbre = "ESPECES".equals(modePaiement) ? 0.20 : 0;
		f.setMontantHt(MontantUtil.arrondi(ht));
		f.setMontantRemise(remise);
		f.setMontantTva(tva);
		f.setDroitTimbre(timbre);
		f.setMontantTtc(MontantUtil.arrondi(base + tva + timbre));
		if ("ENTR".equals(typeClient)) {
			f.setMontantTtc(arrondiDixieme(f.getMontantTtc()));
		}
		return f;
	}

	private double num(Object o) {
		if (o == null) {
			return 0;
		}
		if (o instanceof Number) {
			return ((Number) o).doubleValue();
		}
		try {
			return Double.parseDouble(o.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Version 1 du prepaye (2012) : un releve avec juste le nombre de minutes et SMS.
	 * @deprecated
	 */
	public Facture calculerPrepayeV1(Abonnement abo, String mois) {
		Facture f = new Facture();
		f.setTypeDoc(DOC_RELEVE);
		f.setIdClient(abo.getIdClient());
		f.setIdAbonnement(abo.getIdAbonnement());
		f.setMois(mois);
		f.setDateEmission(new Date());
		f.setStatut("INFO");
		List<Consommation> l = consoDao.lister(abo.getMsisdn(), mois);
		double min = 0;
		double sms = 0;
		double mo = 0;
		for (int i = 0; i < l.size(); i++) {
			Consommation c = l.get(i);
			if ("VOIX_NAT".equals(c.getTypeConso()) || "VOIX_INTER".equals(c.getTypeConso())) {
				min += c.getQuantite();
			} else if ("SMS".equals(c.getTypeConso())) {
				sms += c.getQuantite();
			} else {
				mo += c.getQuantite();
			}
		}
		f.ajouterLigne(new LigneFacture("INFO", "Minutes consommees", min, 0, 0));
		f.ajouterLigne(new LigneFacture("INFO", "SMS envoyes", sms, 0, 0));
		f.ajouterLigne(new LigneFacture("INFO", "Internet (Mo)", mo, 0, 0));
		return f;
	}

	/**
	 * Prix minute international par indicatif (grille 2012, avant les zones).
	 * @deprecated les zones sont calculees par la plateforme reseau depuis 2013
	 */
	double prixInter2012(String indicatif) {
		if (indicatif == null) {
			return 6.00;
		}
		String i = indicatif.startsWith("00") ? indicatif.substring(2) : indicatif;
		// Europe de l'ouest
		if (i.startsWith("33")) {
			return 3.60; // France
		}
		if (i.startsWith("34")) {
			return 3.60; // Espagne
		}
		if (i.startsWith("39")) {
			return 3.90; // Italie
		}
		if (i.startsWith("32")) {
			return 3.90; // Belgique
		}
		if (i.startsWith("31")) {
			return 3.90; // Pays-Bas
		}
		if (i.startsWith("49")) {
			return 3.90; // Allemagne
		}
		if (i.startsWith("44")) {
			return 4.20; // Royaume-Uni
		}
		if (i.startsWith("41")) {
			return 4.20; // Suisse
		}
		if (i.startsWith("351")) {
			return 4.20; // Portugal
		}
		// Maghreb
		if (i.startsWith("213")) {
			return 4.80; // Algerie
		}
		if (i.startsWith("216")) {
			return 4.80; // Tunisie
		}
		if (i.startsWith("218")) {
			return 6.00; // Libye
		}
		if (i.startsWith("222")) {
			return 6.00; // Mauritanie
		}
		// Afrique de l'ouest
		if (i.startsWith("221")) {
			return 6.60; // Senegal
		}
		if (i.startsWith("225")) {
			return 6.60; // Cote d'Ivoire
		}
		if (i.startsWith("223")) {
			return 7.20; // Mali
		}
		// Golfe
		if (i.startsWith("966")) {
			return 7.20; // Arabie Saoudite
		}
		if (i.startsWith("971")) {
			return 7.20; // Emirats
		}
		if (i.startsWith("974")) {
			return 7.20; // Qatar
		}
		// Amerique
		if (i.startsWith("1")) {
			return 4.20; // USA / Canada
		}
		return 9.00;
	}

	/**
	 * Contribution au service universel facturee en 2012 (0,5% du HT), supprimee en 2013.
	 * @deprecated
	 */
	double contributionServiceUniversel(Contexte ctx, double ht) {
		if (ctx.annee != 2012) {
			return 0;
		}
		return MontantUtil.arrondi(ht * 0.005);
	}

	/**
	 * Ancien calcul du prorata (2012), jours calendaires sur 30 sans tenir compte de la resiliation.
	 * @deprecated voir calculerProrata
	 */
	double prorata2012(Date debut, String mois) {
		if (debut == null || !DateUtil.memeMois(debut, mois)) {
			return 1.0;
		}
		int j = DateUtil.jour(debut);
		return (31 - j) / 30.0;
	}

	/**
	 * Copie des lignes d'une facture vers les DTO de l'ancien ecran de caisse (2012).
	 * @deprecated
	 */
	public Vector<String[]> lignesPourCaisse(Facture f) {
		Vector<String[]> v = new Vector<String[]>();
		for (int i = 0; i < f.getLignes().size(); i++) {
			LigneFacture l = f.getLignes().get(i);
			String[] t = new String[4];
			t[0] = String.valueOf(l.getNumOrdre());
			t[1] = l.getLibelle();
			t[2] = MontantUtil.texte(l.getQuantite());
			t[3] = MontantUtil.texte(l.getMontantHt());
			v.add(t);
		}
		return v;
	}

	// =====================================================================
	// cas particuliers par annee
	// =====================================================================

	/**
	 * Ajustements ponctuels decides en cours d'annee (notes de la direction commerciale).
	 * A nettoyer un jour : les annees passees ne servent plus qu'en cas de recalcul.
	 */
	private void ajustementsAnnuels(Contexte ctx, Facture f) {
		switch (ctx.annee) {
		case 2012:
			ajustements2012(ctx, f);
			break;
		case 2013:
			ajustements2013(ctx, f);
			break;
		case 2014:
			ajustements2014(ctx, f);
			break;
		case 2015:
			ajustements2015(ctx, f);
			break;
		case 2016:
		case 2017:
			ajustements2016(ctx, f);
			break;
		case 2018:
			ajustements2018(ctx, f);
			break;
		default:
			ajustements2018(ctx, f);
		}
	}

	private void ajustements2012(Contexte ctx, Facture f) {
		// lancement : frais d'activation 50 Dh sur la 1ere facture postpayee
		if (DateUtil.memeMois(ctx.abo.getDateDebut(), ctx.mois) && !TYPE_PREPAYE.equals(typeOffre(ctx))) {
			ligne(f, L_FRAIS, "Frais de mise en service", 1, 41.67, 41.67);
		}
		double csu = contributionServiceUniversel(ctx, totalLignes(f, L_FORFAIT));
		if (csu > 0) {
			ligne(f, L_FRAIS, "Contribution service universel", 1, csu, csu);
		}
	}

	private void ajustements2013(Contexte ctx, Facture f) {
		if (DateUtil.memeMois(ctx.abo.getDateDebut(), ctx.mois) && !TYPE_PREPAYE.equals(typeOffre(ctx))) {
			ligne(f, L_FRAIS, "Frais de mise en service", 1, 41.67, 41.67);
		}
		// panne reseau Casablanca du 14 au 16 novembre 2013 : 3 jours de forfait rembourses
		if (ctx.moisNum == 11 && "Casablanca".equalsIgnoreCase(ctx.client.getVille())) {
			double base = totalLignes(f, L_FORFAIT);
			double r = MontantUtil.arrondi(base * 3 / ctx.nbJours);
			if (r > 0) {
				ligne(f, L_REMISE, "Geste commercial incident reseau du 14/11", 1, -r, -r);
			}
		}
	}

	private void ajustements2014(Contexte ctx, Facture f) {
		// frais de mise en service supprimes au 01/03/2014
		if (ctx.moisNum < 3 && DateUtil.memeMois(ctx.abo.getDateDebut(), ctx.mois) && !TYPE_PREPAYE.equals(typeOffre(ctx))) {
			ligne(f, L_FRAIS, "Frais de mise en service", 1, 41.67, 41.67);
		}
	}

	private void ajustements2015(Contexte ctx, Facture f) {
		// facture papier : 5 Dh / mois pour les particuliers qui n'ont pas d'e-mail (a partir de juin 2015)
		if (ctx.moisNum >= 6 && !ctx.client.isEntreprise() && (ctx.client.getEmail() == null || ctx.client.getEmail().length() == 0)) {
			ligne(f, L_FRAIS, "Facture papier", 1, 4.17, 4.17);
		}
	}

	private void ajustements2016(Contexte ctx, Facture f) {
		if (!ctx.client.isEntreprise() && (ctx.client.getEmail() == null || ctx.client.getEmail().length() == 0)) {
			ligne(f, L_FRAIS, "Facture papier", 1, 4.17, 4.17);
		}
		// coupure Oujda / Nador janvier 2016
		if (ctx.annee == 2016 && ctx.moisNum == 1 && ("Oujda".equalsIgnoreCase(ctx.client.getVille()) || "Nador".equalsIgnoreCase(ctx.client.getVille()))) {
			double base = totalLignes(f, L_FORFAIT);
			double r = MontantUtil.arrondi(base * 0.25);
			if (r > 0) {
				ligne(f, L_REMISE, "Geste commercial incident reseau Oriental", 1, -r, -r);
			}
		}
	}

	private void ajustements2018(Contexte ctx, Facture f) {
		// facture papier : gratuite depuis avril 2018 (decision ANRT)
		if (ctx.moisNum < 4 && !ctx.client.isEntreprise() && (ctx.client.getEmail() == null || ctx.client.getEmail().length() == 0)) {
			ligne(f, L_FRAIS, "Facture papier", 1, 4.17, 4.17);
		}
	}

	// =====================================================================
	// version 2014 du postpaye, avant les promos ramadan / salon
	// =====================================================================

	/**
	 * @deprecated remplace par calculerPostpaye en 2016, garde pour les recalculs 2014-2015
	 */
	Facture calculerPostpaye2014(Contexte ctx) {
		Facture f = nouvelleFacture(ctx, DOC_FACTURE);
		double forfait = ctx.tarif.getPrixHt();
		if (forfait <= 0 && ctx.offre != null) {
			forfait = MontantUtil.ht(ctx.offre.getPrixMensuel(), TAUX_TVA);
		}
		double p = MontantUtil.arrondi(forfait * ctx.prorata);
		ligne(f, L_FORFAIT, "Forfait " + ctx.tarif.getLibelleOffre(), ctx.prorata, forfait, p);

		int fMin = ctx.tarif.getForfaitMinutes();
		if (fMin >= 0 && ctx.minutesNat > fMin) {
			double q = Math.ceil(ctx.minutesNat - fMin);
			double pu = ctx.tarif.getPrixMinNat() / 1.2;
			ligne(f, L_HF_VOIX, "Appels hors forfait", q, pu, q * pu);
		}
		int fSms = ctx.tarif.getForfaitSms();
		if (fSms >= 0 && ctx.sms > fSms) {
			double q = Math.ceil(ctx.sms - fSms);
			double pu = ctx.tarif.getPrixSms() / 1.2;
			ligne(f, L_HF_SMS, "SMS hors forfait", q, pu, q * pu);
		}
		int fMo = ctx.tarif.getForfaitMo();
		if (fMo >= 0 && ctx.dataMo > fMo) {
			double q = Math.ceil(ctx.dataMo - fMo);
			double pu = ctx.tarif.getPrixMo();
			ligne(f, L_HF_DATA, "Internet hors forfait", q, pu, q * pu);
		}
		Iterator<String> it = ctx.inter.keySet().iterator();
		while (it.hasNext()) {
			String z = it.next();
			double q = Math.ceil(ctx.inter.get(z).doubleValue());
			double pu = ctx.tarif.getPrixMinInter() * coefZone(z) / 1.2;
			ligne(f, L_INTER, "International " + z.toLowerCase(), q, pu, q * pu);
		}
		it = ctx.roaming.keySet().iterator();
		while (it.hasNext()) {
			String z = it.next();
			double q = ctx.roaming.get(z).doubleValue();
			ligne(f, L_ROAMING, "Itinerance " + z.toLowerCase(), q, 5.0, q * 5.0);
		}
		if ("BIENVENUE".equals(ctx.abo.getCodePromo())) {
			promoBienvenue(ctx, f);
		} else if ("PARRAINAGE".equals(ctx.abo.getCodePromo())) {
			promoParrainage(ctx, f);
		} else {
			remiseFidelite(ctx, f);
		}
		if (ctx.annee == 2014) {
			ajustements2014(ctx, f);
		} else {
			ajustements2015(ctx, f);
		}
		calculerTotaux(ctx, f);
		return f;
	}

	/**
	 * Comparaison ancien / nouveau calcul pour un contrat (outil service client).
	 */
	public String comparerCalculs(Abonnement abo, String mois) {
		Contexte ctx = preparer(abo, mois, true);
		if (ctx == null) {
			return "rien a facturer";
		}
		Facture ancien = calculerPostpaye2014(ctx);
		Contexte ctx2 = preparer(abo, mois, true);
		Facture nouveau = calculerPostpaye(ctx2);
		StringBuffer sb = new StringBuffer();
		sb.append("contrat ").append(abo.getNumContrat()).append(" mois ").append(mois).append("\n");
		sb.append("calcul 2014 : ").append(MontantUtil.texte(ancien.getMontantTtc())).append(" TTC, ").append(ancien.getLignes().size()).append(" lignes\n");
		sb.append("calcul actuel : ").append(MontantUtil.texte(nouveau.getMontantTtc())).append(" TTC, ").append(nouveau.getLignes().size()).append(" lignes\n");
		sb.append("ecart : ").append(MontantUtil.texte(nouveau.getMontantTtc() - ancien.getMontantTtc())).append("\n");
		return sb.toString();
	}

	// =====================================================================

	public void setOffreDao(OffreDao offreDao) {
		this.offreDao = offreDao;
	}

	public void setConsoDao(ConsoDao consoDao) {
		this.consoDao = consoDao;
	}

	public void setClientDao(ClientDao clientDao) {
		this.clientDao = clientDao;
	}

	public void setAbonnementDao(AbonnementDao abonnementDao) {
		this.abonnementDao = abonnementDao;
	}

	public void setFactureDao(FactureDAO factureDao) {
		this.factureDao = factureDao;
	}

	public void setParamDao(ParamDao paramDao) {
		this.paramDao = paramDao;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void setDelaiEcheance(int delaiEcheance) {
		this.delaiEcheance = delaiEcheance;
	}

	public void setModeTest(boolean modeTest) {
		this.modeTest = modeTest;
	}

	public boolean isModeTest() {
		return modeTest;
	}
}
