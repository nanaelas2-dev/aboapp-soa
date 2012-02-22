package ma.telecomatlas.abo.dao;

import java.util.List;

/**
 * Table PARAMETRE (cle / valeur).
 */
public class ParamDao extends AbstractDao {

	public String lire(String cle) {
		List<String> l = jdbcTemplate.queryForList("SELECT VALEUR FROM PARAMETRE WHERE CLE = ?", String.class, cle);
		return l.isEmpty() ? null : l.get(0);
	}

	public String lire(String cle, String defaut) {
		String v = lire(cle);
		return v == null ? defaut : v;
	}

	public void ecrire(String cle, String valeur) {
		int n = jdbcTemplate.update("UPDATE PARAMETRE SET VALEUR = ? WHERE CLE = ?", valeur, cle);
		if (n == 0) {
			jdbcTemplate.update("INSERT INTO PARAMETRE (CLE, VALEUR) VALUES (?, ?)", cle, valeur);
		}
	}

	/** compteur (numeros de contrat, de facture) - pas de verrou, une seule appli */
	public synchronized int suivant(String cle) {
		int n = Integer.parseInt(lire(cle, "0")) + 1;
		ecrire(cle, String.valueOf(n));
		return n;
	}
}
