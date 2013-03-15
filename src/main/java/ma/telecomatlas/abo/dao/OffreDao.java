package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ma.telecomatlas.abo.domaine.Offre;
import ma.telecomatlas.abo.domaine.OffreTarif;

import org.springframework.jdbc.core.RowMapper;

/**
 * Offres (table OFFRE) et tarifs facturation (OFFRE_TARIF).
 */
public class OffreDao extends AbstractDao {

    private RowMapper<Offre> mapperOffre = new RowMapper<Offre>() {
        public Offre mapRow(ResultSet rs, int i) throws SQLException {
            Offre o = new Offre();
            o.setCodeOffre(rs.getString("CODE_OFFRE"));
            o.setLibelle(rs.getString("LIBELLE"));
            o.setTypeOffre(rs.getString("TYPE_OFFRE"));
            o.setPrixMensuel(rs.getDouble("PRIX_MENSUEL"));
            o.setDataMo(rs.getInt("DATA_MO"));
            o.setMinutes(rs.getInt("MINUTES"));
            o.setNbSms(rs.getInt("NB_SMS"));
            o.setEngagementMois(rs.getInt("ENGAGEMENT_MOIS"));
            o.setActif(rs.getInt("ACTIF") == 1);
            o.setDateLancement(date(rs, "DATE_LANCEMENT"));
            return o;
        }
    };

    private RowMapper<OffreTarif> mapperTarif = new RowMapper<OffreTarif>() {
        public OffreTarif mapRow(ResultSet rs, int i) throws SQLException {
            OffreTarif t = new OffreTarif();
            t.setIdTarif(rs.getInt("ID_TARIF"));
            t.setCodeOffre(rs.getString("CODE_OFFRE"));
            t.setLibelleOffre(rs.getString("LIBELLE_OFFRE"));
            t.setTypeOffre(rs.getString("TYPE_OFFRE"));
            t.setPrixHt(rs.getDouble("PRIX_HT"));
            t.setPrixTtc(rs.getDouble("PRIX_TTC"));
            t.setPrixMinNat(rs.getDouble("PRIX_MIN_NAT"));
            t.setPrixMinInter(rs.getDouble("PRIX_MIN_INTER"));
            t.setPrixSms(rs.getDouble("PRIX_SMS"));
            t.setPrixMo(rs.getDouble("PRIX_MO"));
            t.setForfaitMinutes(rs.getInt("FORFAIT_MINUTES"));
            t.setForfaitSms(rs.getInt("FORFAIT_SMS"));
            t.setForfaitMo(rs.getInt("FORFAIT_MO"));
            t.setDateDebut(date(rs, "DATE_DEBUT"));
            t.setDateFin(date(rs, "DATE_FIN"));
            return t;
        }
    };

    public Offre trouver(String code) {
        List<Offre> l = jdbcTemplate.query("SELECT * FROM OFFRE WHERE CODE_OFFRE = ?", mapperOffre, code);
        if (l.size() == 0) {
            return null;
        }
        return l.get(0);
    }

    public List<Offre> listerActives() {
        return jdbcTemplate.query("SELECT * FROM OFFRE WHERE ACTIF = 1 ORDER BY TYPE_OFFRE, PRIX_MENSUEL", mapperOffre);
    }

    public List<Offre> listerToutes() {
        return jdbcTemplate.query("SELECT * FROM OFFRE ORDER BY CODE_OFFRE", mapperOffre);
    }

    /**
     * Tarif en vigueur pour un mois donne (yyyy-MM).
     */
    public OffreTarif tarif(String code, String mois) {
        List<OffreTarif> l = jdbcTemplate.query("SELECT * FROM OFFRE_TARIF WHERE CODE_OFFRE = ? AND DATE_DEBUT <= ? "
                + "AND (DATE_FIN IS NULL OR DATE_FIN >= ?) ORDER BY DATE_DEBUT DESC", mapperTarif, code, mois + "-28", mois + "-01");
        if (l.isEmpty()) {
            return null;
        }
        return l.get(0);
    }

    // ancienne version (avant les tarifs dates), encore utilisee par l'ecran fiche
    public OffreTarif tarif(String code) {
        List<OffreTarif> l = jdbcTemplate.query("SELECT * FROM OFFRE_TARIF WHERE CODE_OFFRE = ?", mapperTarif, code);
        return l.isEmpty() ? null : l.get(0);
    }

    public void desactiver(String code) {
        jdbcTemplate.update("UPDATE OFFRE SET ACTIF = 0 WHERE CODE_OFFRE = ?", code);
    }
}
