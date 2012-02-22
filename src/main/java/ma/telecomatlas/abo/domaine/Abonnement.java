/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 * Tous droits reserves.
 */
package ma.telecomatlas.abo.domaine;

import java.io.Serializable;
import java.util.Date;

/**
 * Contrat d'abonnement d'une ligne.
 * statuts : EN_ATTENTE, EN_COURS, SUSPENDU, RESILIE
 * typeOffre n'est pas une colonne (rempli par jointure sur OFFRE)
 */
public class Abonnement implements Serializable {

	private static final long serialVersionUID = -79193L;

	private int idAbonnement;
	private String numContrat;
	private int idClient;
	private String codeOffre;
	private String ancienneOffre;
	private Date dateChgtOffre;
	private String msisdn;
	private int idSim;
	private Date dateDebut;
	private Date dateFinEngagement;
	private String statut;
	private String modePaiement;
	private String codePromo;
	private int idBoutique;
	private Date dateResiliation;
	private String motifResiliation;
	private String typeOffre;

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

	public int getIdClient() {
		return idClient;
	}

	public void setIdClient(int idClient) {
		this.idClient = idClient;
	}

	public String getCodeOffre() {
		return codeOffre;
	}

	public void setCodeOffre(String codeOffre) {
		this.codeOffre = codeOffre;
	}

	public String getAncienneOffre() {
		return ancienneOffre;
	}

	public void setAncienneOffre(String ancienneOffre) {
		this.ancienneOffre = ancienneOffre;
	}

	public Date getDateChgtOffre() {
		return dateChgtOffre;
	}

	public void setDateChgtOffre(Date dateChgtOffre) {
		this.dateChgtOffre = dateChgtOffre;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public int getIdSim() {
		return idSim;
	}

	public void setIdSim(int idSim) {
		this.idSim = idSim;
	}

	public Date getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(Date dateDebut) {
		this.dateDebut = dateDebut;
	}

	public Date getDateFinEngagement() {
		return dateFinEngagement;
	}

	public void setDateFinEngagement(Date dateFinEngagement) {
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

	public int getIdBoutique() {
		return idBoutique;
	}

	public void setIdBoutique(int idBoutique) {
		this.idBoutique = idBoutique;
	}

	public Date getDateResiliation() {
		return dateResiliation;
	}

	public void setDateResiliation(Date dateResiliation) {
		this.dateResiliation = dateResiliation;
	}

	public String getMotifResiliation() {
		return motifResiliation;
	}

	public void setMotifResiliation(String motifResiliation) {
		this.motifResiliation = motifResiliation;
	}

	public String getTypeOffre() {
		return typeOffre;
	}

	public void setTypeOffre(String typeOffre) {
		this.typeOffre = typeOffre;
	}

}
