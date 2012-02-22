package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ma.telecomatlas.abo.domaine.CarteSim;

import org.springframework.jdbc.core.RowMapper;

/**
 * Cartes SIM
 * @author kSaidi
 */
public class SimDAO extends AbstractDao {

    class SimMapper implements RowMapper<CarteSim> {
        public CarteSim mapRow(ResultSet rs, int i) throws SQLException {
            CarteSim s = new CarteSim();
            s.setIdSim(rs.getInt("ID_SIM"));
            s.setIccid(rs.getString("ICCID"));
            s.setImsi(rs.getString("IMSI"));
            s.setPuk(rs.getString("PUK"));
            s.setMsisdn(rs.getString("MSISDN"));
            s.setStatut(rs.getString("STATUT"));
            s.setIdBoutique(rs.getInt("ID_BOUTIQUE"));
            s.setDateLivraison(date(rs, "DATE_LIVRAISON"));
            s.setDateActivation(date(rs, "DATE_ACTIVATION"));
            s.setLotReseau(rs.getString("LOT_RESEAU"));
            return s;
        }
    }

    public CarteSim trouver(int id) {
        List<CarteSim> l = jdbcTemplate.query("SELECT * FROM SIM WHERE ID_SIM = ?", new SimMapper(), id);
        return l.isEmpty() ? null : l.get(0);
    }

    public CarteSim trouverParIccid(String iccid) {
        List<CarteSim> l = jdbcTemplate.query("SELECT * FROM SIM WHERE ICCID = ?", new SimMapper(), iccid);
        return l.isEmpty() ? null : l.get(0);
    }

    public List<CarteSim> listerAActiver(int max) {
        return jdbcTemplate.query("SELECT * FROM SIM WHERE STATUT = 'A_ACTIVER' ORDER BY ID_SIM LIMIT " + max, new SimMapper());
    }

    public List<CarteSim> stockBoutique(int idBoutique) {
        return jdbcTemplate.query("SELECT * FROM SIM WHERE STATUT = 'STOCK' AND ID_BOUTIQUE = ?", new SimMapper(), idBoutique);
    }

    public void reserver(int idSim, String msisdn) {
        jdbcTemplate.update("UPDATE SIM SET STATUT = 'A_ACTIVER', MSISDN = ? WHERE ID_SIM = ?", msisdn, idSim);
    }

    public void marquerActive(int idSim, String lot) {
        jdbcTemplate.update("UPDATE SIM SET STATUT = 'ACTIVE', DATE_ACTIVATION = NOW(), LOT_RESEAU = ? WHERE ID_SIM = ?", lot, idSim);
    }

    public void changerStatut(int idSim, String statut) {
        jdbcTemplate.update("UPDATE SIM SET STATUT = ? WHERE ID_SIM = ?", statut, idSim);
    }

    public boolean msisdnLibre(String msisdn) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SIM WHERE MSISDN = ? AND STATUT NOT IN ('RESILIEE','PERDUE')", Integer.class, msisdn);
        return n.intValue() == 0;
    }
}
