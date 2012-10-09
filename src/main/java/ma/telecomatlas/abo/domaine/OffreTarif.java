package ma.telecomatlas.abo.domaine;

import java.util.Date;

/**
 * Ligne de la table OFFRE_TARIF (tarifs du service facturation).
 *
 * @author ksaidi
 */
public class OffreTarif {

    private int idTarif;
    private String codeOffre;
    private String libelleOffre;
    private String typeOffre;
    private double prixHt;
    private double prixTtc;
    private double prixMinNat;
    private double prixMinInter;
    private double prixSms;
    private double prixMo;
    private int forfaitMinutes;
    private int forfaitSms;
    private int forfaitMo;
    private Date dateDebut;
    private Date dateFin;

    public int getIdTarif() {
        return idTarif;
    }

    public void setIdTarif(int idTarif) {
        this.idTarif = idTarif;
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

    public String getTypeOffre() {
        return typeOffre;
    }

    public void setTypeOffre(String typeOffre) {
        this.typeOffre = typeOffre;
    }

    public double getPrixHt() {
        return prixHt;
    }

    public void setPrixHt(double prixHt) {
        this.prixHt = prixHt;
    }

    public double getPrixTtc() {
        return prixTtc;
    }

    public void setPrixTtc(double prixTtc) {
        this.prixTtc = prixTtc;
    }

    public double getPrixMinNat() {
        return prixMinNat;
    }

    public void setPrixMinNat(double prixMinNat) {
        this.prixMinNat = prixMinNat;
    }

    public double getPrixMinInter() {
        return prixMinInter;
    }

    public void setPrixMinInter(double prixMinInter) {
        this.prixMinInter = prixMinInter;
    }

    public double getPrixSms() {
        return prixSms;
    }

    public void setPrixSms(double prixSms) {
        this.prixSms = prixSms;
    }

    public double getPrixMo() {
        return prixMo;
    }

    public void setPrixMo(double prixMo) {
        this.prixMo = prixMo;
    }

    public int getForfaitMinutes() {
        return forfaitMinutes;
    }

    public void setForfaitMinutes(int forfaitMinutes) {
        this.forfaitMinutes = forfaitMinutes;
    }

    public int getForfaitSms() {
        return forfaitSms;
    }

    public void setForfaitSms(int forfaitSms) {
        this.forfaitSms = forfaitSms;
    }

    public int getForfaitMo() {
        return forfaitMo;
    }

    public void setForfaitMo(int forfaitMo) {
        this.forfaitMo = forfaitMo;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

}
