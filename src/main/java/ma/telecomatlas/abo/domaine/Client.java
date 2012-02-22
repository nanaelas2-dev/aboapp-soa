/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 * Tous droits reserves.
 */
package ma.telecomatlas.abo.domaine;

import java.io.Serializable;
import java.util.Date;

/**
 * Client (particulier ou entreprise).
 * TYPE_CLIENT : PART / ENTR
 */
public class Client implements Serializable {

	private static final long serialVersionUID = -47517L;

	private int idClient;
	private String codeClient;
	private String typeClient;
	private String civilite;
	private String nom;
	private String prenom;
	private String raisonSociale;
	private String cin;
	private String ice;
	private Date dateNaissance;
	private String adresse;
	private String ville;
	private String telContact;
	private String email;
	private int idBoutique;
	private Date dateCreation;
	private String statut;
	private Date clientDepuis;

	public int getIdClient() {
		return idClient;
	}

	public void setIdClient(int idClient) {
		this.idClient = idClient;
	}

	public String getCodeClient() {
		return codeClient;
	}

	public void setCodeClient(String codeClient) {
		this.codeClient = codeClient;
	}

	public String getTypeClient() {
		return typeClient;
	}

	public void setTypeClient(String typeClient) {
		this.typeClient = typeClient;
	}

	public String getCivilite() {
		return civilite;
	}

	public void setCivilite(String civilite) {
		this.civilite = civilite;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getRaisonSociale() {
		return raisonSociale;
	}

	public void setRaisonSociale(String raisonSociale) {
		this.raisonSociale = raisonSociale;
	}

	public String getCin() {
		return cin;
	}

	public void setCin(String cin) {
		this.cin = cin;
	}

	public String getIce() {
		return ice;
	}

	public void setIce(String ice) {
		this.ice = ice;
	}

	public Date getDateNaissance() {
		return dateNaissance;
	}

	public void setDateNaissance(Date dateNaissance) {
		this.dateNaissance = dateNaissance;
	}

	public String getAdresse() {
		return adresse;
	}

	public void setAdresse(String adresse) {
		this.adresse = adresse;
	}

	public String getVille() {
		return ville;
	}

	public void setVille(String ville) {
		this.ville = ville;
	}

	public String getTelContact() {
		return telContact;
	}

	public void setTelContact(String telContact) {
		this.telContact = telContact;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getIdBoutique() {
		return idBoutique;
	}

	public void setIdBoutique(int idBoutique) {
		this.idBoutique = idBoutique;
	}

	public Date getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(Date dateCreation) {
		this.dateCreation = dateCreation;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public Date getClientDepuis() {
		return clientDepuis;
	}

	public void setClientDepuis(Date clientDepuis) {
		this.clientDepuis = clientDepuis;
	}

	public String getNomComplet() {
		if ("ENTR".equals(typeClient) && raisonSociale != null) {
			return raisonSociale;
		}
		return (prenom == null ? "" : prenom + " ") + (nom == null ? "" : nom);
	}

	public boolean isEntreprise() {
		return "ENTR".equals(typeClient);
	}

}
