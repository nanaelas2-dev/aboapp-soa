package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ma.telecomatlas.abo.domaine.Boutique;

import org.springframework.jdbc.core.RowMapper;

public class BoutiqueDao extends AbstractDao {

	private RowMapper<Boutique> mapper = new RowMapper<Boutique>() {
		public Boutique mapRow(ResultSet rs, int i) throws SQLException {
			Boutique b = new Boutique();
			b.setIdBoutique(rs.getInt("ID_BOUTIQUE"));
			b.setCodeBoutique(rs.getString("CODE_BOUTIQUE"));
			b.setNom(rs.getString("NOM"));
			b.setVille(rs.getString("VILLE"));
			b.setAdresse(rs.getString("ADRESSE"));
			b.setTel(rs.getString("TEL"));
			b.setResponsable(rs.getString("RESPONSABLE"));
			b.setOuverte("O".equals(rs.getString("OUVERTE")));
			return b;
		}
	};

	public Boutique trouverParCode(String code) {
		List<Boutique> l = jdbcTemplate.query("SELECT * FROM BOUTIQUE WHERE CODE_BOUTIQUE = ?", mapper, code);
		return l.isEmpty() ? null : l.get(0);
	}

	public Boutique trouver(int id) {
		List<Boutique> l = jdbcTemplate.query("SELECT * FROM BOUTIQUE WHERE ID_BOUTIQUE = ?", mapper, id);
		return l.isEmpty() ? null : l.get(0);
	}

	public List<Boutique> lister() {
		return jdbcTemplate.query("SELECT * FROM BOUTIQUE ORDER BY VILLE, CODE_BOUTIQUE", mapper);
	}
}
