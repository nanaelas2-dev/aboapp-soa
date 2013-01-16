/*
 * Copyright (c) 2013 Telecom Atlas Mobile SA - Rabat
 */
package ma.telecomatlas.abo.batch;

import java.util.ArrayList;
import java.util.List;

import ma.telecomatlas.abo.dao.AbonnementDao;
import ma.telecomatlas.abo.dao.ParamDao;
import ma.telecomatlas.abo.dao.SimDAO;
import ma.telecomatlas.abo.domaine.Abonnement;
import ma.telecomatlas.abo.domaine.CarteSim;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Batch lance au demarrage de l'appli (init-method) :
 *  - SIM A_ACTIVER -> fichier pour la plateforme reseau -> ACTIVE
 *
 * @author ksaidi
 */
public class ActivationSimJob extends Thread {

    private static Logger log = Logger.getLogger(ActivationSimJob.class);

    private SimDAO simDao;
    private AbonnementDao abonnementDao;
    private ParamDao paramDao;
    private JdbcTemplate jdbcTemplate;
    private FichierActivationReseau fichierReseau;

    private int periode = 30;
    private int max = 200;
    private boolean actif = true;
    private volatile boolean arret = false;

    public void demarrer() {
        if (!actif) {
            log.info("batch activation desactive (batch.activation.actif)");
            return;
        }
        setName("batch-activation-sim");
        setDaemon(true);
        start();
        System.out.println("*** batch activation SIM demarre, periode " + periode + " s");
    }

    public void arreter() {
        arret = true;
        interrupt();
    }

    public void run() {
        while (!arret) {
            try {
                Thread.sleep(periode * 1000L);
            } catch (InterruptedException e) {
                if (arret) {
                    break;
                }
            }
            try {
                cycle();
            } catch (Exception e) {
                log.error("erreur cycle batch", e);
                e.printStackTrace();
            }
        }
        System.out.println("*** batch activation SIM arrete");
    }

    void cycle() throws Exception {

        List<CarteSim> sims = simDao.listerAActiver(max);
        if (!sims.isEmpty()) {
            List<String> offres = new ArrayList<String>();
            for (int i = 0; i < sims.size(); i++) {
                List<String> l = jdbcTemplate.queryForList("SELECT CODE_OFFRE FROM ABONNEMENT WHERE ID_SIM = ? ORDER BY ID_ABONNEMENT DESC", String.class, sims.get(i).getIdSim());
                offres.add(l.isEmpty() ? null : l.get(0));
            }
            String lot = fichierReseau.ecrire(sims, offres);
            int nbAbo = 0;
            for (int i = 0; i < sims.size(); i++) {
                CarteSim s = sims.get(i);
                simDao.marquerActive(s.getIdSim(), lot);
                nbAbo += abonnementDao.activerParSim(s.getIdSim());
            }
            paramDao.ecrire("RESEAU_DERNIER_LOT", lot);
            log.info("activation : " + sims.size() + " SIM, " + nbAbo + " abonnements, fichier " + lot);
        } else {
            log.debug("rien a activer");
        }

    }

    // utilise quand on reactivait les SIM une par une (2013)
    void activerUne(CarteSim s) {
        simDao.marquerActive(s.getIdSim(), "MANUEL");
        Abonnement a = abonnementDao.trouverParMsisdn(s.getMsisdn());
        if (a != null) {
            abonnementDao.changerStatut(a.getIdAbonnement(), "EN_COURS");
        }
    }

    public void setSimDao(SimDAO simDao) {
        this.simDao = simDao;
    }

    public void setAbonnementDao(AbonnementDao abonnementDao) {
        this.abonnementDao = abonnementDao;
    }

    public void setParamDao(ParamDao paramDao) {
        this.paramDao = paramDao;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setFichierReseau(FichierActivationReseau fichierReseau) {
        this.fichierReseau = fichierReseau;
    }

    public void setPeriode(int periode) {
        this.periode = periode;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}
