/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 */
package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ma.telecomatlas.abo.domaine.Client;

import org.springframework.jdbc.core.RowMapper;

public class ClientDao extends AbstractDao {

	private static final String COLS = "ID_CLIENT, CODE_CLIENT, TYPE_CLIENT, CIVILITE, NOM, PRENOM, RAISON_SOCIALE, CIN, ICE, DATE_NAISSANCE, "
			+ "ADRESSE, VILLE, TEL_CONTACT, EMAIL, ID_BOUTIQUE, DATE_CREATION, STATUT, CLIENT_DEPUIS";

	private RowMapper<Client> mapper = new RowMapper<Client>() {
		public Client mapRow(ResultSet rs, int i) throws SQLException {
			Client c = new Client();
			c.setIdClient(rs.getInt("ID_CLIENT"));
			c.setCodeClient(rs.getString("CODE_CLIENT"));
			c.setTypeClient(rs.getString("TYPE_CLIENT"));
			c.setCivilite(rs.getString("CIVILITE"));
			c.setNom(rs.getString("NOM"));
			c.setPrenom(rs.getString("PRENOM"));
			c.setRaisonSociale(rs.getString("RAISON_SOCIALE"));
			c.setCin(rs.getString("CIN"));
			c.setIce(rs.getString("ICE"));
			c.setDateNaissance(date(rs, "DATE_NAISSANCE"));
			c.setAdresse(rs.getString("ADRESSE"));
			c.setVille(rs.getString("VILLE"));
			c.setTelContact(rs.getString("TEL_CONTACT"));
			c.setEmail(rs.getString("EMAIL"));
			c.setIdBoutique(rs.getInt("ID_BOUTIQUE"));
			c.setDateCreation(date(rs, "DATE_CREATION"));
			c.setStatut(rs.getString("STATUT"));
			c.setClientDepuis(date(rs, "CLIENT_DEPUIS"));
			return c;
		}
	};

	public Client trouver(int id) {
		List<Client> l = jdbcTemplate.query("SELECT " + COLS + " FROM CLIENT WHERE ID_CLIENT = ?", mapper, id);
		return l.isEmpty() ? null : l.get(0);
	}

	public Client trouverParCode(String code) {
		List<Client> l = jdbcTemplate.query("SELECT " + COLS + " FROM CLIENT WHERE CODE_CLIENT = ?", mapper, code);
		return l.isEmpty() ? null : l.get(0);
	}

	public Client trouverParCin(String cin) {
		List<Client> l = jdbcTemplate.query("SELECT " + COLS + " FROM CLIENT WHERE CIN = ?", mapper, cin.toUpperCase());
		return l.isEmpty() ? null : l.get(0);
	}

	public List<Client> rechercher(String nom, String cin, String ville) {
		StringBuffer sql = new StringBuffer("SELECT " + COLS + " FROM CLIENT WHERE 1=1");
		List<Object> params = new ArrayList<Object>();
		if (nom != null && nom.length() > 0) {
			sql.append(" AND (NOM LIKE ? OR RAISON_SOCIALE LIKE ?)");
			params.add("%" + nom + "%");
			params.add("%" + nom + "%");
		}
		if (cin != null && cin.length() > 0) {
			sql.append(" AND CIN = ?");
			params.add(cin.toUpperCase());
		}
		if (ville != null && ville.length() > 0) {
			sql.append(" AND VILLE = ?");
			params.add(ville);
		}
		sql.append(" ORDER BY NOM, PRENOM LIMIT 200");
		return jdbcTemplate.query(sql.toString(), params.toArray(), mapper);
	}

	public int creer(Client c) {
		jdbcTemplate.update("INSERT INTO CLIENT (CODE_CLIENT, TYPE_CLIENT, CIVILITE, NOM, PRENOM, RAISON_SOCIALE, CIN, ICE, DATE_NAISSANCE, ADRESSE, VILLE, "
				+ "TEL_CONTACT, EMAIL, ID_BOUTIQUE, DATE_CREATION, STATUT, CLIENT_DEPUIS) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
				c.getCodeClient(), c.getTypeClient(), c.getCivilite(), c.getNom(), c.getPrenom(), c.getRaisonSociale(), c.getCin(), c.getIce(),
				sql(c.getDateNaissance()), c.getAdresse(), c.getVille(), c.getTelContact(), c.getEmail(), c.getIdBoutique(),
				sql(c.getDateCreation()), c.getStatut(), sql(c.getClientDepuis()));
		return dernierId();
	}

	public void majCoordonnees(Client c) {
		jdbcTemplate.update("UPDATE CLIENT SET ADRESSE = ?, VILLE = ?, TEL_CONTACT = ?, EMAIL = ? WHERE ID_CLIENT = ?",
				c.getAdresse(), c.getVille(), c.getTelContact(), c.getEmail(), c.getIdClient());
	}

	public void changerStatut(int id, String statut) {
		jdbcTemplate.update("UPDATE CLIENT SET STATUT = ? WHERE ID_CLIENT = ?", statut, id);
	}

	public int compter() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CLIENT", Integer.class);
	}

	/** nombre de lignes actives du client (remise flotte entreprise) */
	public int nbLignesActives(int idClient) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ABONNEMENT WHERE ID_CLIENT = ? AND STATUT IN ('EN_COURS','SUSPENDU')", Integer.class, idClient);
	}
}
