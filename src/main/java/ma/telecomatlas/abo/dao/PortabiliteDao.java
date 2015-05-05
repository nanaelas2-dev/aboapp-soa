package ma.telecomatlas.abo.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ma.telecomatlas.abo.domaine.DemandePortabilite;

import org.springframework.jdbc.core.RowMapper;

public class PortabiliteDao extends AbstractDao {

    private RowMapper<DemandePortabilite> mapper = new RowMapper<DemandePortabilite>() {
        public DemandePortabilite mapRow(ResultSet rs, int i) throws SQLException {
            DemandePortabilite d = new DemandePortabilite();
            d.setIdDemande(rs.getInt("ID_DEMANDE"));
            d.setRefDemande(rs.getString("REF_DEMANDE"));
            d.setMsisdn(rs.getString("MSISDN"));
            d.setOperateurDonneur(rs.getString("OPERATEUR_DONNEUR"));
            d.setCodeRio(rs.getString("CODE_RIO"));
            d.setIdClient(rs.getInt("ID_CLIENT"));
            d.setIdAbonnement(rs.getInt("ID_ABONNEMENT"));
            d.setDateDemande(date(rs, "DATE_DEMANDE"));
            d.setDatePortage(date(rs, "DATE_PORTAGE"));
            d.setStatut(rs.getString("STATUT"));
            d.setMotifRefus(rs.getString("MOTIF_REFUS"));
            d.setSaisiPar(rs.getString("SAISI_PAR"));
            d.setIdBoutique(rs.getInt("ID_BOUTIQUE"));
            d.setCommentaire(rs.getString("COMMENTAIRE"));
            return d;
        }
    };

    public DemandePortabilite trouver(String ref) {
        List<DemandePortabilite> l = jdbcTemplate.query("SELECT * FROM DEMANDE_PORTABILITE WHERE REF_DEMANDE = ?", mapper, ref);
        return l.isEmpty() ? null : l.get(0);
    }

    public List<DemandePortabilite> listerEnCours() {
        return jdbcTemplate.query("SELECT * FROM DEMANDE_PORTABILITE WHERE STATUT IN ('SAISIE','ENVOYEE','ACCEPTEE') ORDER BY DATE_DEMANDE", mapper);
    }

    public List<DemandePortabilite> listerParClient(int idClient) {
        return jdbcTemplate.query("SELECT * FROM DEMANDE_PORTABILITE WHERE ID_CLIENT = ? ORDER BY DATE_DEMANDE DESC", mapper, idClient);
    }

    public boolean enCoursPourNumero(String msisdn) {
        Integer n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DEMANDE_PORTABILITE WHERE MSISDN = ? AND STATUT IN ('SAISIE','ENVOYEE','ACCEPTEE')", Integer.class, msisdn);
        return n > 0;
    }

    public void inserer(DemandePortabilite d) {
        jdbcTemplate.update("INSERT INTO DEMANDE_PORTABILITE (REF_DEMANDE, MSISDN, OPERATEUR_DONNEUR, CODE_RIO, ID_CLIENT, ID_ABONNEMENT, DATE_DEMANDE, DATE_PORTAGE, "
                + "STATUT, SAISI_PAR, ID_BOUTIQUE, COMMENTAIRE) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)", d.getRefDemande(), d.getMsisdn(), d.getOperateurDonneur(),
                d.getCodeRio(), d.getIdClient(), d.getIdAbonnement() == 0 ? null : d.getIdAbonnement(), sql(d.getDateDemande()), sql(d.getDatePortage()),
                d.getStatut(), d.getSaisiPar(), d.getIdBoutique(), d.getCommentaire());
    }

    public int changerStatut(String ref, String statut, String motif) {
        return jdbcTemplate.update("UPDATE DEMANDE_PORTABILITE SET STATUT = ?, MOTIF_REFUS = ? WHERE REF_DEMANDE = ?", statut, motif, ref);
    }
}
