package ma.telecomatlas.abo.soap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import ma.telecomatlas.abo.dao.AbonnementDao;
import ma.telecomatlas.abo.dao.ClientDao;
import ma.telecomatlas.abo.dao.PortabiliteDao;
import ma.telecomatlas.abo.domaine.Abonnement;
import ma.telecomatlas.abo.domaine.Client;
import ma.telecomatlas.abo.domaine.DemandePortabilite;
import ma.telecomatlas.abo.metier.NumeroteurHelper;
import ma.telecomatlas.abo.outils.ConfigUtil;
import ma.telecomatlas.abo.outils.DateUtil;
import ma.telecomatlas.abo.outils.MsisdnUtil;
import ma.telecomatlas.abo.soap.types.DemandePortaInfo;
import ma.telecomatlas.abo.soap.types.Reponse;

import org.apache.log4j.Logger;

/**
 * Portabilite entrante. Les demandes sont saisies en boutique puis transmises a la main
 * a l'operateur donneur par le back-office (fax / mail), qui met a jour le statut ici.
 *
 * @author ksaidi 2015
 */
@WebService(serviceName = "PortabiliteService", targetNamespace = "http://abo.telecomatlas.ma/")
public class PortabiliteService {

    private static final Logger LOG = Logger.getLogger(PortabiliteService.class);

    private static final String[] STATUTS = { "SAISIE", "ENVOYEE", "ACCEPTEE", "REFUSEE", "PORTEE", "ANNULEE" };

    private PortabiliteDao portabiliteDao;
    private ClientDao clientDao;
    private AbonnementDao abonnementDao;
    private NumeroteurHelper numeroteur;

    @WebMethod
    public Reponse saisirDemande(@WebParam(name = "demande") DemandePortaInfo info) {
        if (info == null) {
            return Reponse.ko("demande vide");
        }
        Client c = null;
        if (info.getCodeClient() != null) {
            c = clientDao.trouverParCode(info.getCodeClient());
        }
        if (c == null && info.getCin() != null) {
            c = clientDao.trouverParCin(info.getCin());
        }
        if (c == null) {
            return Reponse.ko("client inconnu, creer le client d'abord");
        }
        String num = MsisdnUtil.normaliser(info.getMsisdn());
        if (!MsisdnUtil.estMobile(num)) {
            return Reponse.ko("numero invalide : " + info.getMsisdn());
        }
        String ops = ConfigUtil.get("porta.operateurs", "IAM,MDI,WAN");
        if (info.getOperateur() == null || ops.indexOf(info.getOperateur().toUpperCase()) < 0) {
            return Reponse.ko("operateur donneur inconnu (" + ops + ")");
        }
        if (portabiliteDao.enCoursPourNumero(num)) {
            return Reponse.ko("une demande est deja en cours pour " + num);
        }
        Abonnement existant = abonnementDao.trouverParMsisdn(num);
        if (existant != null) {
            return Reponse.ko("numero deja chez Telecom Atlas (contrat " + existant.getNumContrat() + ")");
        }
        if (info.getRio() == null || info.getRio().trim().length() < 9) {
            return Reponse.ko("RIO invalide");
        }
        DemandePortabilite d = new DemandePortabilite();
        d.setRefDemande(numeroteur.refPortabilite());
        d.setMsisdn(num);
        d.setOperateurDonneur(info.getOperateur().toUpperCase());
        d.setCodeRio(info.getRio().trim().toUpperCase());
        d.setIdClient(c.getIdClient());
        d.setDateDemande(new Date());
        Date souhaitee = DateUtil.lireFr(info.getDatePortageSouhaitee());
        Date mini = DateUtil.ajouterJours(new Date(), ConfigUtil.getInt("porta.delai.jours", 3));
        if (souhaitee == null || souhaitee.before(mini)) {
            souhaitee = mini;
        }
        d.setDatePortage(souhaitee);
        d.setStatut("SAISIE");
        d.setSaisiPar(info.getSaisiPar());
        d.setIdBoutique(c.getIdBoutique());
        d.setCommentaire(info.getCommentaire());
        portabiliteDao.inserer(d);
        LOG.info("portabilite " + d.getRefDemande() + " " + num + " depuis " + d.getOperateurDonneur());
        return Reponse.ok(d.getRefDemande());
    }

    @WebMethod
    public DemandePortaInfo consulterDemande(@WebParam(name = "reference") String reference) {
        DemandePortabilite d = portabiliteDao.trouver(reference);
        if (d == null) {
            return null;
        }
        return versInfo(d);
    }

    @WebMethod
    public List<DemandePortaInfo> listerDemandesEnCours() {
        List<DemandePortaInfo> res = new ArrayList<DemandePortaInfo>();
        List<DemandePortabilite> l = portabiliteDao.listerEnCours();
        for (int i = 0; i < l.size(); i++) {
            res.add(versInfo(l.get(i)));
        }
        return res;
    }

    /**
     * Mise a jour par le back-office apres reponse de l'operateur donneur.
     */
    @WebMethod
    public Reponse changerStatut(@WebParam(name = "reference") String reference, @WebParam(name = "statut") String statut,
            @WebParam(name = "motif") String motif) {
        boolean connu = false;
        for (int i = 0; i < STATUTS.length; i++) {
            if (STATUTS[i].equals(statut)) {
                connu = true;
            }
        }
        if (!connu) {
            return Reponse.ko("statut inconnu " + statut);
        }
        DemandePortabilite d = portabiliteDao.trouver(reference);
        if (d == null) {
            return Reponse.ko("demande inconnue");
        }
        if ("PORTEE".equals(d.getStatut()) || "ANNULEE".equals(d.getStatut())) {
            return Reponse.ko("demande terminee (" + d.getStatut() + ")");
        }
        if ("REFUSEE".equals(statut) && (motif == null || motif.length() == 0)) {
            return Reponse.ko("motif obligatoire pour un refus");
        }
        portabiliteDao.changerStatut(reference, statut, motif);
        return Reponse.ok(reference);
    }

    @WebMethod
    public Reponse annulerDemande(@WebParam(name = "reference") String reference) {
        return changerStatut(reference, "ANNULEE", "annulation client");
    }

    private DemandePortaInfo versInfo(DemandePortabilite d) {
        DemandePortaInfo i = new DemandePortaInfo();
        i.setReference(d.getRefDemande());
        i.setMsisdn(d.getMsisdn());
        i.setOperateur(d.getOperateurDonneur());
        i.setRio(d.getCodeRio());
        Client c = clientDao.trouver(d.getIdClient());
        if (c != null) {
            i.setCodeClient(c.getCodeClient());
            i.setCin(c.getCin());
        }
        i.setDateDemande(DateUtil.versFr(d.getDateDemande()));
        i.setDatePortageSouhaitee(DateUtil.versFr(d.getDatePortage()));
        i.setStatut(d.getStatut());
        i.setMotifRefus(d.getMotifRefus());
        i.setCommentaire(d.getCommentaire());
        i.setSaisiPar(d.getSaisiPar());
        return i;
    }

    @WebMethod(exclude = true)
    public void setPortabiliteDao(PortabiliteDao portabiliteDao) {
        this.portabiliteDao = portabiliteDao;
    }

    @WebMethod(exclude = true)
    public void setClientDao(ClientDao clientDao) {
        this.clientDao = clientDao;
    }

    @WebMethod(exclude = true)
    public void setAbonnementDao(AbonnementDao abonnementDao) {
        this.abonnementDao = abonnementDao;
    }

    @WebMethod(exclude = true)
    public void setNumeroteur(NumeroteurHelper numeroteur) {
        this.numeroteur = numeroteur;
    }
}
