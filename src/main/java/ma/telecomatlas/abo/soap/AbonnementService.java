/*
 * Copyright (c) 2012 Telecom Atlas Mobile SA - Rabat
 * Tous droits reserves.
 */
package ma.telecomatlas.abo.soap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import ma.telecomatlas.abo.dao.AbonnementDao;
import ma.telecomatlas.abo.dao.BoutiqueDao;
import ma.telecomatlas.abo.dao.ClientDao;
import ma.telecomatlas.abo.dao.ConsoDao;
import ma.telecomatlas.abo.dao.FactureDAO;
import ma.telecomatlas.abo.dao.OffreDao;
import ma.telecomatlas.abo.dao.PortabiliteDao;
import ma.telecomatlas.abo.dao.SimDAO;
import ma.telecomatlas.abo.domaine.Abonnement;
import ma.telecomatlas.abo.domaine.Boutique;
import ma.telecomatlas.abo.domaine.CarteSim;
import ma.telecomatlas.abo.domaine.Client;
import ma.telecomatlas.abo.domaine.Consommation;
import ma.telecomatlas.abo.domaine.DemandePortabilite;
import ma.telecomatlas.abo.domaine.Facture;
import ma.telecomatlas.abo.domaine.LigneFacture;
import ma.telecomatlas.abo.domaine.Offre;
import ma.telecomatlas.abo.facturation.FacturationManager;
import ma.telecomatlas.abo.facturation.LotFacturation;
import ma.telecomatlas.abo.facturation.TarifsBoutique;
import ma.telecomatlas.abo.metier.NumeroteurHelper;
import ma.telecomatlas.abo.metier.SouscriptionHelper;
import ma.telecomatlas.abo.outils.AboException;
import ma.telecomatlas.abo.outils.ConfigUtil;
import ma.telecomatlas.abo.outils.DateUtil;
import ma.telecomatlas.abo.outils.MontantUtil;
import ma.telecomatlas.abo.outils.MsisdnUtil;
import ma.telecomatlas.abo.soap.types.AbonnementInfo;
import ma.telecomatlas.abo.soap.types.ClientInfo;
import ma.telecomatlas.abo.soap.types.ConsoInfo;
import ma.telecomatlas.abo.soap.types.FactureInfo;
import ma.telecomatlas.abo.soap.types.LigneFactureInfo;
import ma.telecomatlas.abo.soap.types.OffreInfo;
import ma.telecomatlas.abo.soap.types.Reponse;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Service web appele par le logiciel caisse des boutiques.
 *
 * Dates : dd/MM/yyyy, montants : 123.45
 *
 * @author achraibi
 */
@WebService(serviceName = "AbonnementService", targetNamespace = "http://abo.telecomatlas.ma/")
public class AbonnementService {

	private static Logger log = Logger.getLogger(AbonnementService.class);

	private ClientDao clientDao;
	private AbonnementDao abonnementDao;
	private OffreDao offreDao;
	private SimDAO simDao;
	private FactureDAO factureDao;
	private ConsoDao consoDao;
	private BoutiqueDao boutiqueDao;
	private PortabiliteDao portabiliteDao;
	private SouscriptionHelper souscriptionHelper;
	private NumeroteurHelper numeroteur;
	private FacturationManager facturationManager;
	private LotFacturation lotFacturation;
	private TarifsBoutique tarifsBoutique;
	private JdbcTemplate jdbcTemplate;

	// ================================================================ clients

	@WebMethod
	public Reponse creerClient(@WebParam(name = "client") ClientInfo ci) {
		System.out.println("WS creerClient " + (ci == null ? "null" : ci.getNom() + " / " + ci.getCin()));
		if (ci == null) {
			return Reponse.ko("client vide");
		}
		boolean entreprise = "ENTR".equals(ci.getType());
		if (!entreprise) {
			if (ci.getCin() == null || ci.getCin().trim().length() < 5) {
				return Reponse.ko("CIN obligatoire");
			}
			if (ci.getNom() == null || ci.getNom().trim().length() == 0) {
				return Reponse.ko("nom obligatoire");
			}
			Client existant = clientDao.trouverParCin(ci.getCin().trim());
			if (existant != null) {
				Reponse r = new Reponse("EXISTE", "client deja connu");
				r.setReference(existant.getCodeClient());
				return r;
			}
		} else {
			if (ci.getRaisonSociale() == null || ci.getIce() == null) {
				return Reponse.ko("raison sociale et ICE obligatoires pour une entreprise");
			}
			// ICE : 15 chiffres
			if (ci.getIce().replace(" ", "").length() != 15) {
				return Reponse.ko("ICE invalide");
			}
		}
		Client c = new Client();
		c.setCodeClient(numeroteur.codeClient(entreprise));
		c.setTypeClient(entreprise ? "ENTR" : "PART");
		c.setCivilite(ci.getCivilite());
		c.setNom(ci.getNom());
		c.setPrenom(ci.getPrenom());
		c.setRaisonSociale(ci.getRaisonSociale());
		c.setCin(ci.getCin() == null ? null : ci.getCin().trim().toUpperCase());
		c.setIce(ci.getIce());
		c.setDateNaissance(DateUtil.lireFr(ci.getDateNaissance()));
		c.setAdresse(ci.getAdresse());
		c.setVille(ci.getVille());
		c.setTelContact(ci.getTelephone());
		c.setEmail(ci.getEmail());
		Boutique b = ci.getCodeBoutique() == null ? null : boutiqueDao.trouverParCode(ci.getCodeBoutique());
		c.setIdBoutique(b == null ? 1 : b.getIdBoutique());
		c.setDateCreation(new Date());
		c.setClientDepuis(new Date());
		c.setStatut("ACTIF");
		try {
			int id = clientDao.creer(c);
			log.info("client cree " + c.getCodeClient() + " id " + id);
			return Reponse.ok(c.getCodeClient());
		} catch (Exception e) {
			e.printStackTrace();
			return new Reponse("ERR", e.getMessage());
		}
	}

	@WebMethod
	public Reponse modifierCoordonnees(@WebParam(name = "codeClient") String codeClient, @WebParam(name = "adresse") String adresse,
			@WebParam(name = "ville") String ville, @WebParam(name = "telephone") String telephone, @WebParam(name = "email") String email) {
		Client c = clientDao.trouverParCode(codeClient);
		if (c == null) {
			return Reponse.ko("client inconnu " + codeClient);
		}
		if (adresse != null && adresse.length() > 0) {
			c.setAdresse(adresse);
		}
		if (ville != null && ville.length() > 0) {
			c.setVille(ville);
		}
		if (telephone != null && telephone.length() > 0) {
			c.setTelContact(telephone);
		}
		if (email != null) {
			c.setEmail(email.length() == 0 ? null : email);
		}
		clientDao.majCoordonnees(c);
		return Reponse.ok(codeClient);
	}

	@WebMethod
	public List<ClientInfo> rechercherClients(@WebParam(name = "nom") String nom, @WebParam(name = "cin") String cin, @WebParam(name = "ville") String ville) {
		List<ClientInfo> res = new ArrayList<ClientInfo>();
		if ((nom == null || nom.length() < 2) && (cin == null || cin.length() == 0)) {
			return res;
		}
		List<Client> l = clientDao.rechercher(nom, cin, ville);
		for (int i = 0; i < l.size(); i++) {
			res.add(versInfo(l.get(i)));
		}
		return res;
	}

	@WebMethod
	public ClientInfo consulterClient(@WebParam(name = "codeClient") String codeClient) {
		Client c = clientDao.trouverParCode(codeClient);
		if (c == null) {
			return null;
		}
		return versInfo(c);
	}

	// ================================================================ offres

	/**
	 * Catalogue de la boutique (OFFRE_BOUTIQUE).
	 */
	@WebMethod
	public List<OffreInfo> listerOffres(@WebParam(name = "codeBoutique") String codeBoutique) {
		Boutique b = codeBoutique == null ? null : boutiqueDao.trouverParCode(codeBoutique);
		List<Map<String, Object>> l = offreDao.catalogueBoutique(b == null ? 0 : b.getIdBoutique());
		List<OffreInfo> res = new ArrayList<OffreInfo>();
		for (int i = 0; i < l.size(); i++) {
			Map<String, Object> m = l.get(i);
			OffreInfo o = new OffreInfo();
			o.setCode((String) m.get("CODE_OFFRE"));
			o.setLibelle((String) m.get("LIBELLE_COMMERCIAL"));
			o.setCategorie((String) m.get("CATEGORIE"));
			o.setPrix(m.get("PRIX_AFFICHE") == null ? "" : MontantUtil.texte(((Number) m.get("PRIX_AFFICHE")).doubleValue()));
			o.setPromo((String) m.get("MENTION_PROMO"));
			String code = o.getCode();
			o.setType(code.startsWith("PP-") ? "PREPAYE" : code.startsWith("EN-") ? "ENTREPRISE" : "POSTPAYE");
			res.add(o);
		}
		return res;
	}

	// ================================================================ abonnements

	@WebMethod
	public Reponse souscrire(@WebParam(name = "codeClient") String codeClient, @WebParam(name = "codeOffre") String codeOffre,
			@WebParam(name = "iccid") String iccid, @WebParam(name = "msisdn") String msisdn, @WebParam(name = "modePaiement") String modePaiement,
			@WebParam(name = "codePromo") String codePromo, @WebParam(name = "codeBoutique") String codeBoutique) {
		log.info("souscrire " + codeClient + " " + codeOffre + " " + iccid + " " + msisdn);
		Client c = clientDao.trouverParCode(codeClient);
		if (c == null) {
			return Reponse.ko("client inconnu " + codeClient);
		}
		if (!"ACTIF".equals(c.getStatut())) {
			return Reponse.ko("client " + c.getStatut());
		}
		// impayes : pas de nouvelle ligne au dela de 500 DH
		double impaye = factureDao.encoursImpaye(c.getIdClient());
		if (impaye > 500) {
			return new Reponse("IMPAYE", "encours impaye " + MontantUtil.texte(impaye) + " DH");
		}
		// particulier : 5 lignes max (regle ANRT)
		if (!c.isEntreprise() && clientDao.nbLignesActives(c.getIdClient()) >= 5) {
			return Reponse.ko("5 lignes maximum pour un particulier");
		}
		if (c.isEntreprise() && !codeOffre.startsWith("EN-")) {
			return Reponse.ko("offre non autorisee pour une entreprise");
		}
		Boutique b = codeBoutique == null ? null : boutiqueDao.trouverParCode(codeBoutique);
		try {
			Abonnement a = souscriptionHelper.souscrire(c.getIdClient(), codeOffre, iccid, msisdn, modePaiement, codePromo, b == null ? c.getIdBoutique() : b.getIdBoutique());
			Reponse r = Reponse.ok(a.getNumContrat());
			r.setMessage("ligne " + a.getMsisdn() + " en attente d'activation");
			return r;
		} catch (AboException e) {
			return new Reponse(e.getCode(), e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return new Reponse("ERR", e.getMessage());
		}
	}

	@WebMethod
	public List<AbonnementInfo> listerAbonnements(@WebParam(name = "codeClient") String codeClient) {
		List<AbonnementInfo> res = new ArrayList<AbonnementInfo>();
		Client c = clientDao.trouverParCode(codeClient);
		if (c == null) {
			return res;
		}
		List<Abonnement> l = abonnementDao.listerParClient(c.getIdClient());
		for (int i = 0; i < l.size(); i++) {
			res.add(versInfo(l.get(i), c));
		}
		return res;
	}

	@WebMethod
	public AbonnementInfo consulterAbonnement(@WebParam(name = "msisdn") String msisdn) {
		Abonnement a = abonnementDao.trouverParMsisdn(MsisdnUtil.normaliser(msisdn));
		if (a == null) {
			return null;
		}
		return versInfo(a, clientDao.trouver(a.getIdClient()));
	}

	@WebMethod
	public Reponse changerOffre(@WebParam(name = "numContrat") String numContrat, @WebParam(name = "nouvelleOffre") String nouvelleOffre,
			@WebParam(name = "dateEffet") String dateEffet) {
		Abonnement a = abonnementDao.trouverParContrat(numContrat);
		if (a == null) {
			return Reponse.ko("contrat inconnu " + numContrat);
		}
		if (!"EN_COURS".equals(a.getStatut())) {
			return Reponse.ko("contrat " + a.getStatut());
		}
		if (a.getCodeOffre().equals(nouvelleOffre)) {
			return Reponse.ko("deja sur cette offre");
		}
		if (!souscriptionHelper.changementAutorise(a.getCodeOffre(), nouvelleOffre)) {
			return Reponse.ko("changement non autorise " + a.getCodeOffre() + " -> " + nouvelleOffre);
		}
		// un seul changement par mois
		if (a.getDateChgtOffre() != null && DateUtil.memeMois(a.getDateChgtOffre(), DateUtil.moisCourant())) {
			return Reponse.ko("deja un changement d'offre ce mois-ci");
		}
		Date effet = DateUtil.lireFr(dateEffet);
		if (effet == null) {
			effet = new Date();
		}
		// pendant l'engagement on ne descend pas en gamme
		Offre ancienne = offreDao.trouver(a.getCodeOffre());
		Offre nouvelle = offreDao.trouver(nouvelleOffre);
		if (a.getDateFinEngagement() != null && effet.before(a.getDateFinEngagement()) && ancienne != null && nouvelle != null
				&& nouvelle.getPrixMensuel() < ancienne.getPrixMensuel()) {
			return Reponse.ko("offre inferieure impossible avant le " + DateUtil.versFr(a.getDateFinEngagement()));
		}
		abonnementDao.changerOffre(a.getIdAbonnement(), a.getCodeOffre(), nouvelleOffre, effet);
		// nouvel engagement de 12 mois si offre superieure
		if (nouvelle != null && nouvelle.getEngagementMois() > 0) {
			jdbcTemplate.update("UPDATE ABONNEMENT SET DATE_FIN_ENGAGEMENT = ? WHERE ID_ABONNEMENT = ?",
					DateUtil.versSql(DateUtil.ajouterMois(effet, nouvelle.getEngagementMois())), a.getIdAbonnement());
		}
		log.info("changement offre " + numContrat + " " + a.getCodeOffre() + " -> " + nouvelleOffre);
		return Reponse.ok(numContrat);
	}

	/**
	 * Resiliation : met a jour le contrat, la SIM, puis calcule et enregistre la facture de cloture.
	 */
	@WebMethod
	public Reponse resilierAbonnement(@WebParam(name = "numContrat") String numContrat, @WebParam(name = "dateResiliation") String dateResiliation,
			@WebParam(name = "motif") String motif) {
		Abonnement a = abonnementDao.trouverParContrat(numContrat);
		if (a == null) {
			return Reponse.ko("contrat inconnu " + numContrat);
		}
		if ("RESILIE".equals(a.getStatut())) {
			return Reponse.ko("deja resilie");
		}
		Date d = DateUtil.lireFr(dateResiliation);
		if (d == null) {
			d = new Date();
		}
		if (portabiliteDao.enCoursPourNumero(a.getMsisdn())) {
			return Reponse.ko("portabilite en cours sur ce numero");
		}
		abonnementDao.resilier(a.getIdAbonnement(), d, motif);
		simDao.changerStatut(a.getIdSim(), "RESILIEE");
		a.setDateResiliation(d);
		a.setStatut("RESILIE");
		Reponse r = Reponse.ok(numContrat);
		try {
			Facture f = facturationManager.factureCloture(a);
			if (f != null) {
				f.setNumFacture(numeroteur.numeroFacture(f.getMois()));
				f.setLot("CLOTURE");
				factureDao.inserer(f);
				consoDao.marquerFacturees(a.getMsisdn(), f.getMois());
				r.setMessage("facture de cloture " + f.getNumFacture() + " : " + MontantUtil.texte(f.getMontantTtc()) + " DH");
			}
		} catch (Exception e) {
			e.printStackTrace();
			r.setMessage("resilie, facture de cloture a faire a la main (" + e.getMessage() + ")");
		}
		// client sans autre ligne : on passe le client en RESILIE
		if (clientDao.nbLignesActives(a.getIdClient()) == 0) {
			clientDao.changerStatut(a.getIdClient(), "RESILIE");
		}
		return r;
	}

	@WebMethod
	public Reponse suspendreLigne(@WebParam(name = "numContrat") String numContrat, @WebParam(name = "motif") String motif) {
		Abonnement a = abonnementDao.trouverParContrat(numContrat);
		if (a == null || !"EN_COURS".equals(a.getStatut())) {
			return Reponse.ko("contrat introuvable ou pas en cours");
		}
		abonnementDao.changerStatut(a.getIdAbonnement(), "SUSPENDU");
		simDao.changerStatut(a.getIdSim(), "SUSPENDUE");
		log.info("suspension " + numContrat + " : " + motif);
		return Reponse.ok(numContrat);
	}

	@WebMethod
	public Reponse reactiverLigne(@WebParam(name = "numContrat") String numContrat) {
		Abonnement a = abonnementDao.trouverParContrat(numContrat);
		if (a == null || !"SUSPENDU".equals(a.getStatut())) {
			return Reponse.ko("contrat introuvable ou pas suspendu");
		}
		abonnementDao.changerStatut(a.getIdAbonnement(), "EN_COURS");
		simDao.changerStatut(a.getIdSim(), "ACTIVE");
		return Reponse.ok(numContrat);
	}

	@WebMethod
	public String getVersion() {
		return ConfigUtil.get("app.version", "?");
	}

	// ================================================================ conversions

	private ClientInfo versInfo(Client c) {
		ClientInfo ci = new ClientInfo();
		ci.setIdClient(c.getIdClient());
		ci.setCodeClient(c.getCodeClient());
		ci.setType(c.getTypeClient());
		ci.setCivilite(c.getCivilite());
		ci.setNom(c.getNom());
		ci.setPrenom(c.getPrenom());
		ci.setRaisonSociale(c.getRaisonSociale());
		ci.setCin(c.getCin());
		ci.setIce(c.getIce());
		ci.setDateNaissance(DateUtil.versFr(c.getDateNaissance()));
		ci.setAdresse(c.getAdresse());
		ci.setVille(c.getVille());
		ci.setTelephone(c.getTelContact());
		ci.setEmail(c.getEmail());
		Boutique b = boutiqueDao.trouver(c.getIdBoutique());
		ci.setCodeBoutique(b == null ? null : b.getCodeBoutique());
		ci.setDateCreation(DateUtil.versFr(c.getDateCreation()));
		ci.setStatut(c.getStatut());
		return ci;
	}

	private AbonnementInfo versInfo(Abonnement a, Client c) {
		AbonnementInfo info = new AbonnementInfo();
		info.setIdAbonnement(a.getIdAbonnement());
		info.setNumContrat(a.getNumContrat());
		info.setCodeClient(c == null ? null : c.getCodeClient());
		info.setCodeOffre(a.getCodeOffre());
		info.setLibelleOffre(tarifsBoutique.libelle(a.getCodeOffre()));
		info.setMsisdn(a.getMsisdn());
		CarteSim s = simDao.trouver(a.getIdSim());
		info.setIccid(s == null ? null : s.getIccid());
		info.setDateDebut(DateUtil.versFr(a.getDateDebut()));
		info.setDateFinEngagement(DateUtil.versFr(a.getDateFinEngagement()));
		info.setStatut(a.getStatut());
		info.setModePaiement(a.getModePaiement());
		info.setCodePromo(a.getCodePromo());
		info.setPrixMensuel(MontantUtil.texte(tarifsBoutique.prix(a.getCodeOffre())));
		return info;
	}

	private FactureInfo versInfo(Facture f, boolean avecLignes) {
		FactureInfo fi = new FactureInfo();
		fi.setNumFacture(f.getNumFacture());
		fi.setTypeDoc(f.getTypeDoc());
		fi.setMois(f.getMois());
		fi.setDateEmission(DateUtil.versFr(f.getDateEmission()));
		fi.setDateEcheance(DateUtil.versFr(f.getDateEcheance()));
		fi.setMontantHT(MontantUtil.texte(f.getMontantHt()));
		fi.setRemise(MontantUtil.texte(f.getMontantRemise()));
		fi.setMontantTVA(MontantUtil.texte(f.getMontantTva()));
		fi.setTimbre(MontantUtil.texte(f.getDroitTimbre()));
		fi.setMontantTTC(MontantUtil.texte(f.getMontantTtc()));
		fi.setStatut(f.getStatut());
		Abonnement a = abonnementDao.trouver(f.getIdAbonnement());
		if (a != null) {
			fi.setNumContrat(a.getNumContrat());
			fi.setMsisdn(a.getMsisdn());
		}
		if (avecLignes) {
			List<LigneFactureInfo> lignes = new ArrayList<LigneFactureInfo>();
			for (int i = 0; i < f.getLignes().size(); i++) {
				LigneFacture l = f.getLignes().get(i);
				LigneFactureInfo li = new LigneFactureInfo();
				li.setOrdre(l.getNumOrdre());
				li.setCode(l.getCodeLigne());
				li.setLibelle(l.getLibelle());
				li.setQuantite(String.valueOf(l.getQuantite()));
				li.setPrixUnitaire(String.valueOf(l.getPuHt()));
				li.setMontant(MontantUtil.texte(l.getMontantHt()));
				lignes.add(li);
			}
			fi.setLignes(lignes);
		}
		return fi;
	}

	// ================================================================ injection

	@WebMethod(exclude = true)
	public void setClientDao(ClientDao clientDao) {
		this.clientDao = clientDao;
	}

	@WebMethod(exclude = true)
	public void setAbonnementDao(AbonnementDao abonnementDao) {
		this.abonnementDao = abonnementDao;
	}

	@WebMethod(exclude = true)
	public void setOffreDao(OffreDao offreDao) {
		this.offreDao = offreDao;
	}

	@WebMethod(exclude = true)
	public void setSimDao(SimDAO simDao) {
		this.simDao = simDao;
	}

	@WebMethod(exclude = true)
	public void setFactureDao(FactureDAO factureDao) {
		this.factureDao = factureDao;
	}

	@WebMethod(exclude = true)
	public void setConsoDao(ConsoDao consoDao) {
		this.consoDao = consoDao;
	}

	@WebMethod(exclude = true)
	public void setBoutiqueDao(BoutiqueDao boutiqueDao) {
		this.boutiqueDao = boutiqueDao;
	}

	@WebMethod(exclude = true)
	public void setPortabiliteDao(PortabiliteDao portabiliteDao) {
		this.portabiliteDao = portabiliteDao;
	}

	@WebMethod(exclude = true)
	public void setSouscriptionHelper(SouscriptionHelper souscriptionHelper) {
		this.souscriptionHelper = souscriptionHelper;
	}

	@WebMethod(exclude = true)
	public void setNumeroteur(NumeroteurHelper numeroteur) {
		this.numeroteur = numeroteur;
	}

	@WebMethod(exclude = true)
	public void setFacturationManager(FacturationManager facturationManager) {
		this.facturationManager = facturationManager;
	}

	@WebMethod(exclude = true)
	public void setLotFacturation(LotFacturation lotFacturation) {
		this.lotFacturation = lotFacturation;
	}

	@WebMethod(exclude = true)
	public void setTarifsBoutique(TarifsBoutique tarifsBoutique) {
		this.tarifsBoutique = tarifsBoutique;
	}

	@WebMethod(exclude = true)
	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
