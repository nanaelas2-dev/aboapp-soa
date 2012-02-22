package ma.telecomatlas.abo.domaine;

public class Boutique {

	private int idBoutique;
	private String codeBoutique;
	private String nom;
	private String ville;
	private String adresse;
	private String tel;
	private String responsable;
	private boolean ouverte;

	public int getIdBoutique() {
		return idBoutique;
	}

	public void setIdBoutique(int idBoutique) {
		this.idBoutique = idBoutique;
	}

	public String getCodeBoutique() {
		return codeBoutique;
	}

	public void setCodeBoutique(String codeBoutique) {
		this.codeBoutique = codeBoutique;
	}

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getVille() {
		return ville;
	}

	public void setVille(String ville) {
		this.ville = ville;
	}

	public String getAdresse() {
		return adresse;
	}

	public void setAdresse(String adresse) {
		this.adresse = adresse;
	}

	public String getTel() {
		return tel;
	}

	public void setTel(String tel) {
		this.tel = tel;
	}

	public String getResponsable() {
		return responsable;
	}

	public void setResponsable(String responsable) {
		this.responsable = responsable;
	}

	public boolean isOuverte() {
		return ouverte;
	}

	public void setOuverte(boolean ouverte) {
		this.ouverte = ouverte;
	}

}
