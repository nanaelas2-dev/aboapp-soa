package ma.telecomatlas.abo.domaine;

import java.util.Date;

/**
 * Portabilite entrante (numero d'un autre operateur).
 * SAISIE -> ENVOYEE -> ACCEPTEE / REFUSEE -> PORTEE
 * @since 2.3
 */
public class DemandePortabilite {

    private int idDemande;
    private String refDemande;
    private String msisdn;
    private String operateurDonneur;
    private String codeRio;
    private int idClient;
    private int idAbonnement;
    private Date dateDemande;
    private Date datePortage;
    private String statut;
    private String motifRefus;
    private String saisiPar;
    private int idBoutique;
    private String commentaire;

    public int getIdDemande() {
        return idDemande;
    }

    public void setIdDemande(int idDemande) {
        this.idDemande = idDemande;
    }

    public String getRefDemande() {
        return refDemande;
    }

    public void setRefDemande(String refDemande) {
        this.refDemande = refDemande;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getOperateurDonneur() {
        return operateurDonneur;
    }

    public void setOperateurDonneur(String operateurDonneur) {
        this.operateurDonneur = operateurDonneur;
    }

    public String getCodeRio() {
        return codeRio;
    }

    public void setCodeRio(String codeRio) {
        this.codeRio = codeRio;
    }

    public int getIdClient() {
        return idClient;
    }

    public void setIdClient(int idClient) {
        this.idClient = idClient;
    }

    public int getIdAbonnement() {
        return idAbonnement;
    }

    public void setIdAbonnement(int idAbonnement) {
        this.idAbonnement = idAbonnement;
    }

    public Date getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Date dateDemande) {
        this.dateDemande = dateDemande;
    }

    public Date getDatePortage() {
        return datePortage;
    }

    public void setDatePortage(Date datePortage) {
        this.datePortage = datePortage;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getMotifRefus() {
        return motifRefus;
    }

    public void setMotifRefus(String motifRefus) {
        this.motifRefus = motifRefus;
    }

    public String getSaisiPar() {
        return saisiPar;
    }

    public void setSaisiPar(String saisiPar) {
        this.saisiPar = saisiPar;
    }

    public int getIdBoutique() {
        return idBoutique;
    }

    public void setIdBoutique(int idBoutique) {
        this.idBoutique = idBoutique;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

}
