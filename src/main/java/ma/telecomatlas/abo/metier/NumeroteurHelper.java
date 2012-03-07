package ma.telecomatlas.abo.metier;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ma.telecomatlas.abo.dao.ParamDao;

/**
 * Numeros de contrat, de facture, de demande.
 * Les compteurs sont dans PARAMETRE.
 */
public class NumeroteurHelper {

	private ParamDao paramDao;

	public synchronized String numeroContrat() {
		int n = paramDao.suivant("CONTRAT_DERNIER_NUM");
		return "CT" + new SimpleDateFormat("yy").format(new Date()) + new DecimalFormat("000000").format(n);
	}

	public synchronized String numeroFacture(String mois) {
		int n = paramDao.suivant("FACTURATION_DERNIER_NUM");
		return "FA" + mois.replace("-", "") + new DecimalFormat("000000").format(n);
	}

	public String numeroLot(String mois, int passage) {
		return "LOT_" + mois.replace("-", "") + "_" + (passage < 10 ? "0" : "") + passage;
	}

	public String refPortabilite() {
		// pas de compteur en base pour la portabilite, on prend l'heure (Karim 2015)
		String s = new SimpleDateFormat("yyMM").format(new Date());
		long t = System.currentTimeMillis() % 100000;
		return "PRT" + s + new DecimalFormat("00000").format(t);
	}

	public String codeClient(boolean entreprise) {
		int n = paramDao.suivant(entreprise ? "CLIENT_DERNIER_NUM_E" : "CLIENT_DERNIER_NUM");
		return (entreprise ? "E" : "C") + new DecimalFormat("0000000").format(n + (entreprise ? 700 : 14000));
	}

	public void setParamDao(ParamDao paramDao) {
		this.paramDao = paramDao;
	}
}
