/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 * Tous droits reserves.
 */
package ma.telecomatlas.abo.domaine;

import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

public class Facture implements Serializable {

	private static final long serialVersionUID = 1L;

	private int idFacture;
	private String numFacture;
	private String typeDoc;
	private int idClient;
	private int idAbonnement;
	private String mois;
	private Date dateEmission;
	private Date dateEcheance;
	private double montantHt;
	private double montantRemise;
	private double montantTva;
	private double droitTimbre;
	private double montantTtc;
	private String statut;
	private String lot;
	private Vector<LigneFacture> lignes = new Vector<LigneFacture>();

	public int getIdFacture() {
		return idFacture;
	}

	public void setIdFacture(int idFacture) {
		this.idFacture = idFacture;
	}

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

	public String getMois() {
		return mois;
	}

	public void setMois(String mois) {
		this.mois = mois;
	}

	public Date getDateEmission() {
		return dateEmission;
	}

	public void setDateEmission(Date dateEmission) {
		this.dateEmission = dateEmission;
	}

	public Date getDateEcheance() {
		return dateEcheance;
	}

	public void setDateEcheance(Date dateEcheance) {
		this.dateEcheance = dateEcheance;
	}

	public double getMontantHt() {
		return montantHt;
	}

	public void setMontantHt(double montantHt) {
		this.montantHt = montantHt;
	}

	public double getMontantRemise() {
		return montantRemise;
	}

	public void setMontantRemise(double montantRemise) {
		this.montantRemise = montantRemise;
	}

	public double getMontantTva() {
		return montantTva;
	}

	public void setMontantTva(double montantTva) {
		this.montantTva = montantTva;
	}

	public double getDroitTimbre() {
		return droitTimbre;
	}

	public void setDroitTimbre(double droitTimbre) {
		this.droitTimbre = droitTimbre;
	}

	public double getMontantTtc() {
		return montantTtc;
	}

	public void setMontantTtc(double montantTtc) {
		this.montantTtc = montantTtc;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public String getLot() {
		return lot;
	}

	public void setLot(String lot) {
		this.lot = lot;
	}

	public Vector<LigneFacture> getLignes() {
		return lignes;
	}

	public void setLignes(Vector<LigneFacture> lignes) {
		this.lignes = lignes;
	}

	public void ajouterLigne(LigneFacture l) {
		l.setNumOrdre(lignes.size() + 1);
		lignes.add(l);
	}

}
