/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 * Tous droits reserves.
 */
package ma.telecomatlas.abo.domaine;

import java.util.Date;

public class Offre {

	private String codeOffre;
	private String libelle;
	private String typeOffre;
	private double prixMensuel;
	private int dataMo;
	private int minutes;
	private int nbSms;
	private int engagementMois;
	private boolean actif;
	private Date dateLancement;

	public String getCodeOffre() {
		return codeOffre;
	}

	public void setCodeOffre(String codeOffre) {
		this.codeOffre = codeOffre;
	}

	public String getLibelle() {
		return libelle;
	}

	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}

	public String getTypeOffre() {
		return typeOffre;
	}

	public void setTypeOffre(String typeOffre) {
		this.typeOffre = typeOffre;
	}

	public double getPrixMensuel() {
		return prixMensuel;
	}

	public void setPrixMensuel(double prixMensuel) {
		this.prixMensuel = prixMensuel;
	}

	public int getDataMo() {
		return dataMo;
	}

	public void setDataMo(int dataMo) {
		this.dataMo = dataMo;
	}

	public int getMinutes() {
		return minutes;
	}

	public void setMinutes(int minutes) {
		this.minutes = minutes;
	}

	public int getNbSms() {
		return nbSms;
	}

	public void setNbSms(int nbSms) {
		this.nbSms = nbSms;
	}

	public int getEngagementMois() {
		return engagementMois;
	}

	public void setEngagementMois(int engagementMois) {
		this.engagementMois = engagementMois;
	}

	public boolean isActif() {
		return actif;
	}

	public void setActif(boolean actif) {
		this.actif = actif;
	}

	public Date getDateLancement() {
		return dateLancement;
	}

	public void setDateLancement(Date dateLancement) {
		this.dateLancement = dateLancement;
	}

	// -1 = illimite
	public boolean isVoixIllimitee() {
		return minutes < 0;
	}

	public String toString() {
		return codeOffre + " - " + libelle;
	}

}
