package ma.telecomatlas.abo.domaine;

import java.io.Serializable;

public class LigneFacture implements Serializable {

	private static final long serialVersionUID = -95031L;

	private int idLigne;
	private int idFacture;
	private int numOrdre;
	private String codeLigne;
	private String libelle;
	private double quantite;
	private double puHt;
	private double montantHt;
	private double tauxTva;

	public LigneFacture() {
	}

	public LigneFacture(String code, String lib, double qte, double pu, double mt) {
		this.codeLigne = code;
		this.libelle = lib;
		this.quantite = qte;
		this.puHt = pu;
		this.montantHt = mt;
		this.tauxTva = 20.0;
	}

	public int getIdLigne() {
		return idLigne;
	}

	public void setIdLigne(int idLigne) {
		this.idLigne = idLigne;
	}

	public int getIdFacture() {
		return idFacture;
	}

	public void setIdFacture(int idFacture) {
		this.idFacture = idFacture;
	}

	public int getNumOrdre() {
		return numOrdre;
	}

	public void setNumOrdre(int numOrdre) {
		this.numOrdre = numOrdre;
	}

	public String getCodeLigne() {
		return codeLigne;
	}

	public void setCodeLigne(String codeLigne) {
		this.codeLigne = codeLigne;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public double getQuantite() {
		return quantite;
	}

	public void setQuantite(double quantite) {
		this.quantite = quantite;
	}

	public double getPuHt() {
		return puHt;
	}

	public void setPuHt(double puHt) {
		this.puHt = puHt;
	}

	public double getMontantHt() {
		return montantHt;
	}

	public void setMontantHt(double montantHt) {
		this.montantHt = montantHt;
	}

	public double getTauxTva() {
		return tauxTva;
	}

	public void setTauxTva(double tauxTva) {
		this.tauxTva = tauxTva;
	}

}
