package ma.telecomatlas.abo.soap.types;

public class AbonnementInfo {

    private int idAbonnement;
    private String numContrat;
    private String codeClient;
    private String codeOffre;
    private String libelleOffre;
    private String msisdn;
    private String iccid;
    private String dateDebut;
    private String dateFinEngagement;
    private String statut;
    private String modePaiement;
    private String codePromo;
    private String prixMensuel;

    public int getIdAbonnement() {
        return idAbonnement;
    }

    public void setIdAbonnement(int idAbonnement) {
        this.idAbonnement = idAbonnement;
    }

    public String getNumContrat() {
        return numContrat;
    }

    public void setNumContrat(String numContrat) {
        this.numContrat = numContrat;
    }

    public String getCodeClient() {
        return codeClient;
    }

    public void setCodeClient(String codeClient) {
        this.codeClient = codeClient;
    }

    public String getCodeOffre() {
        return codeOffre;
    }

    public void setCodeOffre(String codeOffre) {
        this.codeOffre = codeOffre;
    }

    public String getLibelleOffre() {
        return libelleOffre;
    }

    public void setLibelleOffre(String libelleOffre) {
        this.libelleOffre = libelleOffre;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(String dateDebut) {
        this.dateDebut = dateDebut;
    }

    public String getDateFinEngagement() {
        return dateFinEngagement;
    }

    public void setDateFinEngagement(String dateFinEngagement) {
        this.dateFinEngagement = dateFinEngagement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public String getCodePromo() {
        return codePromo;
    }

    public void setCodePromo(String codePromo) {
        this.codePromo = codePromo;
    }

    public String getPrixMensuel() {
        return prixMensuel;
    }

    public void setPrixMensuel(String prixMensuel) {
        this.prixMensuel = prixMensuel;
    }

}
