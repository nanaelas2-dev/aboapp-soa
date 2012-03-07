/*
 * Copyright (c) 2013 Telecom Atlas Mobile SA - Rabat
 */
package ma.telecomatlas.abo.metier;

import java.util.Date;
import java.util.List;

import ma.telecomatlas.abo.dao.AbonnementDao;
import ma.telecomatlas.abo.dao.OffreDao;
import ma.telecomatlas.abo.dao.SimDAO;
import ma.telecomatlas.abo.domaine.Abonnement;
import ma.telecomatlas.abo.domaine.CarteSim;
import ma.telecomatlas.abo.domaine.Offre;
import ma.telecomatlas.abo.outils.AboException;
import ma.telecomatlas.abo.outils.DateUtil;
import ma.telecomatlas.abo.outils.MsisdnUtil;

import org.apache.log4j.Logger;

/**
 * Regles de souscription communes au WS et a l'ancien ecran de saisie.
 */
public class SouscriptionHelper {

    private static Logger log = Logger.getLogger(SouscriptionHelper.class);

    private AbonnementDao abonnementDao;
    private OffreDao offreDao;
    private SimDAO simDao;
    private NumeroteurHelper numeroteur;

    public Abonnement souscrire(int idClient, String codeOffre, String iccid, String msisdn, String modePaiement, String codePromo, int idBoutique) {
        Offre o = offreDao.trouver(codeOffre);
        if (o == null) {
            throw new AboException("OFFRE", "Offre inconnue : " + codeOffre);
        }
        if (!o.isActif()) {
            throw new AboException("OFFRE", "Offre plus commercialisee : " + codeOffre);
        }
        CarteSim sim = simDao.trouverParIccid(iccid);
        if (sim == null) {
            throw new AboException("SIM", "ICCID inconnu " + iccid);
        }
        if (!"STOCK".equals(sim.getStatut())) {
            throw new AboException("SIM", "SIM deja utilisee (" + sim.getStatut() + ")");
        }
        String num = MsisdnUtil.normaliser(msisdn);
        if (!MsisdnUtil.estMobile(num)) {
            throw new AboException("MSISDN", "Numero invalide " + msisdn);
        }
        if (!simDao.msisdnLibre(num)) {
            throw new AboException("MSISDN", "Numero deja attribue " + num);
        }
        if ("PREPAYE".equals(o.getTypeOffre())) {
            modePaiement = "RECHARGE";
            codePromo = null;
        }
        Date debut = new Date();
        Abonnement a = new Abonnement();
        a.setNumContrat(numeroteur.numeroContrat());
        a.setIdClient(idClient);
        a.setCodeOffre(codeOffre);
        a.setMsisdn(num);
        a.setIdSim(sim.getIdSim());
        a.setDateDebut(debut);
        if (o.getEngagementMois() > 0) {
            a.setDateFinEngagement(DateUtil.ajouterMois(debut, o.getEngagementMois()));
        }
        a.setStatut("EN_ATTENTE");
        a.setModePaiement(modePaiement == null ? "ESPECES" : modePaiement);
        a.setCodePromo(codePromo);
        a.setIdBoutique(idBoutique);
        int id = abonnementDao.creer(a);
        a.setIdAbonnement(id);
        simDao.reserver(sim.getIdSim(), num);
        log.info("souscription " + a.getNumContrat() + " offre " + codeOffre + " sim " + iccid);
        System.out.println(">> nouvelle souscription " + a.getNumContrat());
        return a;
    }

    /**
     * Penalite si resiliation avant la fin d'engagement : mois restants x 50% du prix.
     */
    public double penaliteResiliation(Abonnement a, Date quand) {
        if (a.getDateFinEngagement() == null || quand.after(a.getDateFinEngagement())) {
            return 0;
        }
        Offre o = offreDao.trouver(a.getCodeOffre());
        if (o == null) {
            return 0;
        }
        int restants = DateUtil.moisEntre(quand, a.getDateFinEngagement()) + 1;
        return restants * o.getPrixMensuel() * 0.5;
    }

    public boolean changementAutorise(String ancienne, String nouvelle) {
        Offre a = offreDao.trouver(ancienne);
        Offre n = offreDao.trouver(nouvelle);
        if (a == null || n == null) {
            return false;
        }
        // pas de passage prepaye <-> entreprise
        if ("PREPAYE".equals(a.getTypeOffre()) && "ENTREPRISE".equals(n.getTypeOffre())) {
            return false;
        }
        if ("ENTREPRISE".equals(a.getTypeOffre()) && !"ENTREPRISE".equals(n.getTypeOffre())) {
            return false;
        }
        return n.isActif();
    }

    public List<CarteSim> simsDisponibles(int idBoutique) {
        return simDao.stockBoutique(idBoutique);
    }

    public void setAbonnementDao(AbonnementDao abonnementDao) {
        this.abonnementDao = abonnementDao;
    }

    public void setOffreDao(OffreDao offreDao) {
        this.offreDao = offreDao;
    }

    public void setSimDao(SimDAO simDao) {
        this.simDao = simDao;
    }

    public void setNumeroteur(NumeroteurHelper numeroteur) {
        this.numeroteur = numeroteur;
    }
}
