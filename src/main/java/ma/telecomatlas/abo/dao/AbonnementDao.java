package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import ma.telecomatlas.abo.domaine.Abonnement;

import org.springframework.jdbc.core.RowMapper;

public class AbonnementDao extends AbstractDao {

	private static final String SELECT = "SELECT a.*, o.TYPE_OFFRE FROM ABONNEMENT a LEFT JOIN OFFRE o ON o.CODE_OFFRE = a.CODE_OFFRE ";

	private RowMapper<Abonnement> mapper = new RowMapper<Abonnement>() {
		public Abonnement mapRow(ResultSet rs, int n) throws SQLException {
			Abonnement a = new Abonnement();
			a.setIdAbonnement(rs.getInt("ID_ABONNEMENT"));
			a.setNumContrat(rs.getString("NUM_CONTRAT"));
			a.setIdClient(rs.getInt("ID_CLIENT"));
			a.setCodeOffre(rs.getString("CODE_OFFRE"));
			a.setAncienneOffre(rs.getString("ANCIENNE_OFFRE"));
			a.setDateChgtOffre(date(rs, "DATE_CHGT_OFFRE"));
			a.setMsisdn(rs.getString("MSISDN"));
			a.setIdSim(rs.getInt("ID_SIM"));
			a.setDateDebut(date(rs, "DATE_DEBUT"));
			a.setDateFinEngagement(date(rs, "DATE_FIN_ENGAGEMENT"));
			a.setStatut(rs.getString("STATUT"));
			a.setModePaiement(rs.getString("MODE_PAIEMENT"));
			a.setCodePromo(rs.getString("CODE_PROMO"));
			a.setIdBoutique(rs.getInt("ID_BOUTIQUE"));
			a.setDateResiliation(date(rs, "DATE_RESILIATION"));
			a.setMotifResiliation(rs.getString("MOTIF_RESILIATION"));
			a.setTypeOffre(rs.getString("TYPE_OFFRE"));
			return a;
		}
	};

	public Abonnement trouver(int id) {
		List<Abonnement> l = jdbcTemplate.query(SELECT + "WHERE a.ID_ABONNEMENT = ?", mapper, id);
		return l.isEmpty() ? null : l.get(0);
	}

	public Abonnement trouverParContrat(String num) {
		List<Abonnement> l = jdbcTemplate.query(SELECT + "WHERE a.NUM_CONTRAT = ?", mapper, num);
		return l.isEmpty() ? null : l.get(0);
	}

	public Abonnement trouverParMsisdn(String msisdn) {
		List<Abonnement> l = jdbcTemplate.query(SELECT + "WHERE a.MSISDN = ? AND a.STATUT <> 'RESILIE' ORDER BY a.ID_ABONNEMENT DESC", mapper, msisdn);
		return l.isEmpty() ? null : l.get(0);
	}

	public List<Abonnement> listerParClient(int idClient) {
		return jdbcTemplate.query(SELECT + "WHERE a.ID_CLIENT = ? ORDER BY a.DATE_DEBUT", mapper, idClient);
	}

	/** abonnements a facturer pour le mois : en cours, suspendus, ou resilies dans le mois */
	public List<Abonnement> listerAFacturer(String mois) {
		return jdbcTemplate.query(SELECT + "WHERE a.STATUT IN ('EN_COURS','SUSPENDU') OR (a.STATUT = 'RESILIE' AND a.DATE_RESILIATION >= ?) ORDER BY a.ID_CLIENT, a.ID_ABONNEMENT",
				mapper, mois + "-01");
	}

	public int creer(Abonnement a) {
		jdbcTemplate.update("INSERT INTO ABONNEMENT (NUM_CONTRAT, ID_CLIENT, CODE_OFFRE, MSISDN, ID_SIM, DATE_DEBUT, DATE_FIN_ENGAGEMENT, STATUT, MODE_PAIEMENT, CODE_PROMO, ID_BOUTIQUE) "
				+ "VALUES (?,?,?,?,?,?,?,?,?,?,?)", a.getNumContrat(), a.getIdClient(), a.getCodeOffre(), a.getMsisdn(), a.getIdSim(),
				sql(a.getDateDebut()), sql(a.getDateFinEngagement()), a.getStatut(), a.getModePaiement(), a.getCodePromo(), a.getIdBoutique());
		return dernierId();
	}

	public void changerOffre(int id, String ancienne, String nouvelle, Date quand) {
		jdbcTemplate.update("UPDATE ABONNEMENT SET ANCIENNE_OFFRE = ?, CODE_OFFRE = ?, DATE_CHGT_OFFRE = ? WHERE ID_ABONNEMENT = ?", ancienne, nouvelle, sql(quand), id);
	}

	public void resilier(int id, Date quand, String motif) {
		jdbcTemplate.update("UPDATE ABONNEMENT SET STATUT = 'RESILIE', DATE_RESILIATION = ?, MOTIF_RESILIATION = ? WHERE ID_ABONNEMENT = ?", sql(quand), motif, id);
	}

	public void changerStatut(int id, String statut) {
		jdbcTemplate.update("UPDATE ABONNEMENT SET STATUT = ? WHERE ID_ABONNEMENT = ?", statut, id);
	}

	/** appele par le batch quand la SIM est activee */
	public int activerParSim(int idSim) {
		return jdbcTemplate.update("UPDATE ABONNEMENT SET STATUT = 'EN_COURS' WHERE ID_SIM = ? AND STATUT = 'EN_ATTENTE'", idSim);
	}

	public int compterParOffre(String code) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ABONNEMENT WHERE CODE_OFFRE = ? AND STATUT = 'EN_COURS'", Integer.class, code);
	}
}
