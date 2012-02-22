package ma.telecomatlas.abo.domaine;

import java.util.Date;

/**
 * Carte SIM. STOCK -> A_ACTIVER -> ACTIVE (batch) -> SUSPENDUE / RESILIEE
 */
public class CarteSim {

	private int idSim;
	private String iccid;
	private String imsi;
	private String puk;
	private String msisdn;
	private String statut;
	private int idBoutique;
	private Date dateLivraison;
	private Date dateActivation;
	private String lotReseau;

	public int getIdSim() {
		return idSim;
	}

	public void setIdSim(int idSim) {
		this.idSim = idSim;
	}

	public String getIccid() {
		return iccid;
	}

	public void setIccid(String iccid) {
		this.iccid = iccid;
	}

	public String getImsi() {
		return imsi;
	}

	public void setImsi(String imsi) {
		this.imsi = imsi;
	}

	public String getPuk() {
		return puk;
	}

	public void setPuk(String puk) {
		this.puk = puk;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public int getIdBoutique() {
		return idBoutique;
	}

	public void setIdBoutique(int idBoutique) {
		this.idBoutique = idBoutique;
	}

	public Date getDateLivraison() {
		return dateLivraison;
	}

	public void setDateLivraison(Date dateLivraison) {
		this.dateLivraison = dateLivraison;
	}

	public Date getDateActivation() {
		return dateActivation;
	}

	public void setDateActivation(Date dateActivation) {
		this.dateActivation = dateActivation;
	}

	public String getLotReseau() {
		return lotReseau;
	}

	public void setLotReseau(String lotReseau) {
		this.lotReseau = lotReseau;
	}

}
