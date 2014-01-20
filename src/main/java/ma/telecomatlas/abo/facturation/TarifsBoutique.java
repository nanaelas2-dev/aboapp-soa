package ma.telecomatlas.abo.facturation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prix affiches en boutique (table OFFRE_BOUTIQUE), pour le devis de changement d'offre
 * et le calcul du prorata a la souscription.
 *
 * @author nfassi 2014
 * (les prix dÃ©jÃ  promotionnÃ©s sont dans PRIX_AFFICHE)
 */
public class TarifsBoutique {

	private JdbcTemplate jdbcTemplate;

	private Map<String, Double> cache = new HashMap<String, Double>();
	private long chargement = 0;

	public double prix(String codeOffre) {
		if (System.currentTimeMillis() - chargement > 600000) {
			recharger();
		}
		Double d = cache.get(codeOffre);
		if (d == null) {
			System.out.println("TarifsBoutique : pas de prix pour " + codeOffre);
			return 0;
		}
		return d.doubleValue();
	}

	public String libelle(String codeOffre) {
		List<String> l = jdbcTemplate.queryForList("SELECT LIBELLE_COMMERCIAL FROM OFFRE_BOUTIQUE WHERE CODE_OFFRE = ? AND ID_BOUTIQUE IS NULL", String.class, codeOffre);
		return l.isEmpty() ? codeOffre : l.get(0);
	}

	private synchronized void recharger() {
		cache.clear();
		List<Map<String, Object>> l = jdbcTemplate.queryForList("SELECT CODE_OFFRE, PRIX_AFFICHE FROM OFFRE_BOUTIQUE WHERE ID_BOUTIQUE IS NULL");
		for (int i = 0; i < l.size(); i++) {
			Map<String, Object> m = l.get(i);
			Number n = (Number) m.get("PRIX_AFFICHE");
			cache.put((String) m.get("CODE_OFFRE"), new Double(n == null ? 0 : n.doubleValue()));
		}
		chargement = System.currentTimeMillis();
	}

	/** difference de prix mensuel entre deux offres (devis changement) */
	public double ecart(String ancienne, String nouvelle) {
		return prix(nouvelle) - prix(ancienne);
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
