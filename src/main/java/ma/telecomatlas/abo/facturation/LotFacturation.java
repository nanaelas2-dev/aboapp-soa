package ma.telecomatlas.abo.facturation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ma.telecomatlas.abo.dao.AbonnementDao;
import ma.telecomatlas.abo.dao.ClientDao;
import ma.telecomatlas.abo.dao.ConsoDao;
import ma.telecomatlas.abo.dao.FactureDAO;
import ma.telecomatlas.abo.dao.ParamDao;
import ma.telecomatlas.abo.domaine.Abonnement;
import ma.telecomatlas.abo.domaine.Facture;
import ma.telecomatlas.abo.metier.NumeroteurHelper;
import ma.telecomatlas.abo.outils.ConfigUtil;

import org.apache.log4j.Logger;

/**
 * Facturation mensuelle : tous les abonnements du mois en une passe.
 * Lancee par le batch (PARAMETRE FACTURATION_LANCER = O) ou par le WS lancerFacturationMensuelle.
 *
 * @author nfassi
 */
public class LotFacturation {

	private static Logger log = Logger.getLogger(LotFacturation.class);

	private FacturationManager facturationManager;
	private AbonnementDao abonnementDao;
	private FactureDAO factureDao;
	private ConsoDao consoDao;
	private ClientDao clientDao;
	private ParamDao paramDao;
	private NumeroteurHelper numeroteur;

	private boolean enCours = false;

	public synchronized String executer(String mois) {
		if (enCours) {
			return "facturation deja en cours";
		}
		enCours = true;
		long t0 = System.currentTimeMillis();
		System.out.println("=== FACTURATION " + mois + " debut " + new Date());
		int nbOk = 0;
		int nbDeja = 0;
		int nbRien = 0;
		int nbErreur = 0;
		List<Facture> faites = new ArrayList<Facture>();
		String lot = null;
		try {
			facturationManager.viderCache();
			int passage = 1;
			String dernier = paramDao.lire("FACTURATION_DERNIER_LOT", "");
			if (dernier.startsWith("LOT_" + mois.replace("-", "") + "_")) {
				passage = Integer.parseInt(dernier.substring(dernier.length() - 2)) + 1;
			}
			lot = numeroteur.numeroLot(mois, passage);
			List<Abonnement> abos = abonnementDao.listerAFacturer(mois);
			log.info("lot " + lot + " : " + abos.size() + " abonnements a examiner");
			for (int i = 0; i < abos.size(); i++) {
				Abonnement a = abos.get(i);
				try {
					if (factureDao.existe(a.getIdAbonnement(), mois)) {
						nbDeja++;
						continue;
					}
					Facture f = facturationManager.calculerFacture(a, mois);
					if (f == null) {
						nbRien++;
						continue;
					}
					f.setNumFacture(numeroteur.numeroFacture(mois));
					f.setLot(lot);
					factureDao.inserer(f);
					consoDao.marquerFacturees(a.getMsisdn(), mois);
					faites.add(f);
					nbOk++;
				} catch (Exception e) {
					nbErreur++;
					log.error("erreur facturation contrat " + a.getNumContrat(), e);
					e.printStackTrace();
				}
			}
			paramDao.ecrire("FACTURATION_DERNIER_LOT", lot);
			ecrireJournalVentes(lot, faites);
			log.info(facturationManager.recapLot(lot, faites));
		} finally {
			enCours = false;
		}
		String res = lot + " : " + nbOk + " factures, " + nbDeja + " deja faites, " + nbRien + " sans objet, " + nbErreur + " erreurs ("
				+ (System.currentTimeMillis() - t0) / 1000 + " s)";
		System.out.println("=== FACTURATION " + mois + " fin : " + res);
		return res;
	}

	/** fichier pour la compta (import Sage), meme dossier que les echanges */
	private void ecrireJournalVentes(String lot, List<Facture> factures) {
		File dir = new File(ConfigUtil.get("compta.dossier", "echanges/compta"));
		if (!dir.exists()) {
			dir.mkdirs();
		}
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, "VT_" + lot + ".csv")), "ISO-8859-1"));
			for (int i = 0; i < factures.size(); i++) {
				Facture f = factures.get(i);
				List<String> l = facturationManager.journalVentes(f, clientDao.trouver(f.getIdClient()));
				for (int k = 0; k < l.size(); k++) {
					pw.println(l.get(k));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pw != null) {
				pw.close();
			}
		}
	}

	public boolean isEnCours() {
		return enCours;
	}

	public void setFacturationManager(FacturationManager facturationManager) {
		this.facturationManager = facturationManager;
	}

	public void setAbonnementDao(AbonnementDao abonnementDao) {
		this.abonnementDao = abonnementDao;
	}

	public void setFactureDao(FactureDAO factureDao) {
		this.factureDao = factureDao;
	}

	public void setConsoDao(ConsoDao consoDao) {
		this.consoDao = consoDao;
	}

	public void setClientDao(ClientDao clientDao) {
		this.clientDao = clientDao;
	}

	public void setParamDao(ParamDao paramDao) {
		this.paramDao = paramDao;
	}

	public void setNumeroteur(NumeroteurHelper numeroteur) {
		this.numeroteur = numeroteur;
	}
}
