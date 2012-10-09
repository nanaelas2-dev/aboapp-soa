package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import ma.telecomatlas.abo.domaine.Facture;
import ma.telecomatlas.abo.domaine.LigneFacture;

import org.springframework.jdbc.core.RowMapper;

/**
 * FACTURE + LIGNE_FACTURE
 *
 * @author nfassi
 */
public class FactureDAO extends AbstractDao {

    private RowMapper<Facture> mapper = new RowMapper<Facture>() {
        public Facture mapRow(ResultSet rs, int i) throws SQLException {
            Facture f = new Facture();
            f.setIdFacture(rs.getInt("ID_FACTURE"));
            f.setNumFacture(rs.getString("NUM_FACTURE"));
            f.setTypeDoc(rs.getString("TYPE_DOC"));
            f.setIdClient(rs.getInt("ID_CLIENT"));
            f.setIdAbonnement(rs.getInt("ID_ABONNEMENT"));
            f.setMois(rs.getString("MOIS"));
            f.setDateEmission(date(rs, "DATE_EMISSION"));
            f.setDateEcheance(date(rs, "DATE_ECHEANCE"));
            f.setMontantHt(rs.getDouble("MONTANT_HT"));
            f.setMontantRemise(rs.getDouble("MONTANT_REMISE"));
            f.setMontantTva(rs.getDouble("MONTANT_TVA"));
            f.setDroitTimbre(rs.getDouble("DROIT_TIMBRE"));
            f.setMontantTtc(rs.getDouble("MONTANT_TTC"));
            f.setStatut(rs.getString("STATUT"));
            f.setLot(rs.getString("LOT"));
            return f;
        }
    };

    private RowMapper<LigneFacture> mapperLigne = new RowMapper<LigneFacture>() {
        public LigneFacture mapRow(ResultSet rs, int i) throws SQLException {
            LigneFacture l = new LigneFacture();
            l.setIdLigne(rs.getInt("ID_LIGNE"));
            l.setIdFacture(rs.getInt("ID_FACTURE"));
            l.setNumOrdre(rs.getInt("NUM_ORDRE"));
            l.setCodeLigne(rs.getString("CODE_LIGNE"));
            l.setLibelle(rs.getString("LIBELLE"));
            l.setQuantite(rs.getDouble("QUANTITE"));
            l.setPuHt(rs.getDouble("PU_HT"));
            l.setMontantHt(rs.getDouble("MONTANT_HT"));
            l.setTauxTva(rs.getDouble("TAUX_TVA"));
            return l;
        }
    };

    public Facture trouverParNumero(String num) {
        List<Facture> l = jdbcTemplate.query("SELECT * FROM FACTURE WHERE NUM_FACTURE = ?", mapper, num);
        if (l.isEmpty()) {
            return null;
        }
        Facture f = l.get(0);
        f.getLignes().addAll(lignes(f.getIdFacture()));
        return f;
    }

    public List<Facture> listerParClient(int idClient) {
        return jdbcTemplate.query("SELECT * FROM FACTURE WHERE ID_CLIENT = ? ORDER BY MOIS DESC, NUM_FACTURE", mapper, idClient);
    }

    public List<Facture> listerParAbonnement(int idAbo) {
        return jdbcTemplate.query("SELECT * FROM FACTURE WHERE ID_ABONNEMENT = ? ORDER BY MOIS DESC", mapper, idAbo);
    }

    public List<Facture> listerParMois(String mois) {
        return jdbcTemplate.query("SELECT * FROM FACTURE WHERE MOIS = ? ORDER BY NUM_FACTURE", mapper, mois);
    }

    public List<LigneFacture> lignes(int idFacture) {
        return jdbcTemplate.query("SELECT * FROM LIGNE_FACTURE WHERE ID_FACTURE = ? ORDER BY NUM_ORDRE", mapperLigne, idFacture);
    }

    public boolean existe(int idAbo, String mois) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FACTURE WHERE ID_ABONNEMENT = ? AND MOIS = ? AND STATUT <> 'ANNULEE'", Integer.class, idAbo, mois);
        return n > 0;
    }

    public int inserer(Facture f) {
        jdbcTemplate.update("INSERT INTO FACTURE (NUM_FACTURE, TYPE_DOC, ID_CLIENT, ID_ABONNEMENT, MOIS, DATE_EMISSION, DATE_ECHEANCE, MONTANT_HT, MONTANT_REMISE, "
                + "MONTANT_TVA, DROIT_TIMBRE, MONTANT_TTC, STATUT, LOT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                f.getNumFacture(), f.getTypeDoc(), f.getIdClient(), f.getIdAbonnement(), f.getMois(), sql(f.getDateEmission()), sql(f.getDateEcheance()),
                f.getMontantHt(), f.getMontantRemise(), f.getMontantTva(), f.getDroitTimbre(), f.getMontantTtc(), f.getStatut(), f.getLot());
        int id = dernierId();
        for (int i = 0; i < f.getLignes().size(); i++) {
            LigneFacture l = f.getLignes().get(i);
            jdbcTemplate.update("INSERT INTO LIGNE_FACTURE (ID_FACTURE, NUM_ORDRE, CODE_LIGNE, LIBELLE, QUANTITE, PU_HT, MONTANT_HT, TAUX_TVA) VALUES (?,?,?,?,?,?,?,?)",
                    id, l.getNumOrdre(), l.getCodeLigne(), l.getLibelle(), l.getQuantite(), l.getPuHt(), l.getMontantHt(), l.getTauxTva());
        }
        f.setIdFacture(id);
        return id;
    }

    public void changerStatut(String num, String statut) {
        jdbcTemplate.update("UPDATE FACTURE SET STATUT = ? WHERE NUM_FACTURE = ?", statut, num);
    }

    public Map<String, Object> totauxLot(String lot) {
        return jdbcTemplate.queryForMap("SELECT COUNT(*) AS NB, IFNULL(SUM(MONTANT_TTC),0) AS TOTAL FROM FACTURE WHERE LOT = ?", lot);
    }

    public double encoursImpaye(int idClient) {
        Double d = jdbcTemplate.queryForObject("SELECT IFNULL(SUM(MONTANT_TTC),0) FROM FACTURE WHERE ID_CLIENT = ? AND STATUT IN ('EMISE','IMPAYEE')", Double.class, idClient);
        return d == null ? 0 : d.doubleValue();
    }
}
