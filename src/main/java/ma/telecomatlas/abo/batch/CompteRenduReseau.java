package ma.telecomatlas.abo.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import ma.telecomatlas.abo.dao.SimDAO;
import ma.telecomatlas.abo.domaine.CarteSim;

import org.apache.log4j.Logger;

/**
 * Comptes rendus de la plateforme reseau (fichiers CR_*.txt deposes dans echanges/reseau/entree).
 * Une ligne par SIM : ICCID;OK|KO;code erreur;libelle
 * Les KO repassent en ERREUR_RES, a reprendre a la main par le support.
 */
public class CompteRenduReseau {

	private static Logger log = Logger.getLogger(CompteRenduReseau.class);

	private SimDAO simDao;
	private String dossierEntree = "echanges/reseau/entree";
	private String dossierArchives = "echanges/reseau/archives";

	public int traiter() {
		File dir = new File(dossierEntree);
		if (!dir.exists()) {
			dir.mkdirs();
			return 0;
		}
		File arch = new File(dossierArchives);
		if (!arch.exists()) {
			arch.mkdirs();
		}
		File[] fichiers = dir.listFiles();
		int n = 0;
		if (fichiers == null) {
			return 0;
		}
		for (int i = 0; i < fichiers.length; i++) {
			File f = fichiers[i];
			if (!f.isFile() || !f.getName().startsWith("CR_")) {
				continue;
			}
			n += lire(f);
			if (!f.renameTo(new File(arch, f.getName()))) {
				log.warn("archivage impossible de " + f.getName());
				f.delete();
			}
		}
		return n;
	}

	private int lire(File f) {
		int n = 0;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			String ligne;
			while ((ligne = br.readLine()) != null) {
				if (ligne.trim().length() == 0 || ligne.startsWith("#")) {
					continue;
				}
				String[] t = ligne.split(";");
				if (t.length < 2) {
					continue;
				}
				CarteSim s = simDao.trouverParIccid(t[0].trim());
				if (s == null) {
					log.warn("CR reseau : ICCID inconnu " + t[0]);
					continue;
				}
				if ("KO".equalsIgnoreCase(t[1].trim())) {
					simDao.changerStatut(s.getIdSim(), "ERREUR_RES");
					log.warn("activation refusee par le reseau " + s.getIccid() + " : " + (t.length > 3 ? t[3] : ""));
				}
				n++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
			}
		}
		log.info("CR reseau " + f.getName() + " : " + n + " lignes");
		return n;
	}

	public void setSimDao(SimDAO simDao) {
		this.simDao = simDao;
	}

	public void setDossierEntree(String dossierEntree) {
		this.dossierEntree = dossierEntree;
	}

	public void setDossierArchives(String dossierArchives) {
		this.dossierArchives = dossierArchives;
	}
}
