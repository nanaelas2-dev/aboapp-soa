package ma.telecomatlas.abo.domaine;

import java.util.Date;

public class Consommation {

    private int idConso;
    private String msisdn;
    private int idAbonnement;
    private String mois;
    private String typeConso;
    private double quantite;
    private String unite;
    private String zone;
    private Date dateReleve;
    private String facture;

    public int getIdConso() {
        return idConso;
    }

    public void setIdConso(int idConso) {
        this.idConso = idConso;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public int getIdAbonnement() {
        return idAbonnement;
    }

    public void setIdAbonnement(int idAbonnement) {
        this.idAbonnement = idAbonnement;
    }

    public String getMois() {
        return mois;
    }

    public void setMois(String mois) {
        this.mois = mois;
    }

    public String getTypeConso() {
        return typeConso;
    }

    public void setTypeConso(String typeConso) {
        this.typeConso = typeConso;
    }

    public double getQuantite() {
        return quantite;
    }

    public void setQuantite(double quantite) {
        this.quantite = quantite;
    }

    public String getUnite() {
        return unite;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public Date getDateReleve() {
        return dateReleve;
    }

    public void setDateReleve(Date dateReleve) {
        this.dateReleve = dateReleve;
    }

    public String getFacture() {
        return facture;
    }

    public void setFacture(String facture) {
        this.facture = facture;
    }

}
