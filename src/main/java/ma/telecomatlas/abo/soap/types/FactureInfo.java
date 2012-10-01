package ma.telecomatlas.abo.soap.types;

import java.util.List;

/**
 * Montants en texte, deux decimales, point decimal (demande caisse 2013).
 */
public class FactureInfo {

    private String numFacture;
    private String typeDoc;
    private String mois;
    private String dateEmission;
    private String dateEcheance;
    private String montantHT;
    private String remise;
    private String montantTVA;
    private String timbre;
    private String montantTTC;
    private String statut;
    private String numContrat;
    private String msisdn;
    private List<LigneFactureInfo> lignes;

    public String getNumFacture() {
        return numFacture;
    }

    public void setNumFacture(String numFacture) {
        this.numFacture = numFacture;
    }

    public String getTypeDoc() {
        return typeDoc;
    }

    public void setTypeDoc(String typeDoc) {
        this.typeDoc = typeDoc;
    }

    public String getMois() {
        return mois;
    }

    public void setMois(String mois) {
        this.mois = mois;
    }

    public String getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(String dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(String dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public String getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(String montantHT) {
        this.montantHT = montantHT;
    }

    public String getRemise() {
        return remise;
    }

    public void setRemise(String remise) {
        this.remise = remise;
    }

    public String getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(String montantTVA) {
        this.montantTVA = montantTVA;
    }

    public String getTimbre() {
        return timbre;
    }

    public void setTimbre(String timbre) {
        this.timbre = timbre;
    }

    public String getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(String montantTTC) {
        this.montantTTC = montantTTC;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getNumContrat() {
        return numContrat;
    }

    public void setNumContrat(String numContrat) {
        this.numContrat = numContrat;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public List<LigneFactureInfo> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneFactureInfo> lignes) {
        this.lignes = lignes;
    }

}
