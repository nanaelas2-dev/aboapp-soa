package ma.telecomatlas.abo.soap.types;

public class DemandePortaInfo {

	private String reference;
	private String msisdn;
	private String operateur;
	private String rio;
	private String codeClient;
	private String cin;
	private String dateDemande;
	private String datePortageSouhaitee;
	private String statut;
	private String motifRefus;
	private String commentaire;
	private String saisiPar;

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getOperateur() {
		return operateur;
	}

	public void setOperateur(String operateur) {
		this.operateur = operateur;
	}

	public String getRio() {
		return rio;
	}

	public void setRio(String rio) {
		this.rio = rio;
	}

	public String getCodeClient() {
		return codeClient;
	}

	public void setCodeClient(String codeClient) {
		this.codeClient = codeClient;
	}

	public String getCin() {
		return cin;
	}

	public void setCin(String cin) {
		this.cin = cin;
	}

	public String getDateDemande() {
		return dateDemande;
	}

	public void setDateDemande(String dateDemande) {
		this.dateDemande = dateDemande;
	}

	public String getDatePortageSouhaitee() {
		return datePortageSouhaitee;
	}

	public void setDatePortageSouhaitee(String datePortageSouhaitee) {
		this.datePortageSouhaitee = datePortageSouhaitee;
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

	public String getCommentaire() {
		return commentaire;
	}

	public void setCommentaire(String commentaire) {
		this.commentaire = commentaire;
	}

	public String getSaisiPar() {
		return saisiPar;
	}

	public void setSaisiPar(String saisiPar) {
		this.saisiPar = saisiPar;
	}

}
