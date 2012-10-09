package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ma.telecomatlas.abo.domaine.Consommation;

import org.springframework.jdbc.core.RowMapper;

public class ConsoDao extends AbstractDao {

	private static RowMapper<Consommation> MAPPER = null;

	private RowMapper<Consommation> mapper() {
		if (MAPPER == null) {
			MAPPER = new RowMapper<Consommation>() {
				public Consommation mapRow(ResultSet rs, int i) throws SQLException {
					Consommation c = new Consommation();
					c.setIdConso(rs.getInt("ID_CONSO"));
					c.setMsisdn(rs.getString("MSISDN"));
					c.setIdAbonnement(rs.getInt("ID_ABONNEMENT"));
					c.setMois(rs.getString("MOIS"));
					c.setTypeConso(rs.getString("TYPE_CONSO"));
					c.setQuantite(rs.getDouble("QUANTITE"));
					c.setUnite(rs.getString("UNITE"));
					c.setZone(rs.getString("ZONE"));
					c.setDateReleve(DateUtilProxy.lire(rs.getString("DATE_RELEVE")));
					c.setFacture(rs.getString("FACTURE"));
					return c;
				}
			};
		}
		return MAPPER;
	}

	public List<Consommation> lister(String msisdn, String mois) {
		return jdbcTemplate.query("SELECT * FROM CONSOMMATION WHERE MSISDN = ? AND MOIS = ? ORDER BY TYPE_CONSO", mapper(), msisdn, mois);
	}

	public List<Consommation> listerNonFacturees(String msisdn, String mois) {
		return jdbcTemplate.query("SELECT * FROM CONSOMMATION WHERE MSISDN = ? AND MOIS = ? AND FACTURE = 'N'", mapper(), msisdn, mois);
	}

	public int marquerFacturees(String msisdn, String mois) {
		return jdbcTemplate.update("UPDATE CONSOMMATION SET FACTURE = 'O' WHERE MSISDN = ? AND MOIS = ?", msisdn, mois);
	}

	public void inserer(Consommation c) {
		jdbcTemplate.update("INSERT INTO CONSOMMATION (MSISDN, ID_ABONNEMENT, MOIS, TYPE_CONSO, QUANTITE, UNITE, ZONE, DATE_RELEVE, FACTURE) VALUES (?,?,?,?,?,?,?,NOW(),'N')",
				c.getMsisdn(), c.getIdAbonnement(), c.getMois(), c.getTypeConso(), c.getQuantite(), c.getUnite(), c.getZone());
	}

	// petite classe pour eviter d'importer DateUtil partout (Karim)
	static class DateUtilProxy {
		static java.util.Date lire(String s) {
			return ma.telecomatlas.abo.outils.DateUtil.depuisSql(s);
		}
	}
}
