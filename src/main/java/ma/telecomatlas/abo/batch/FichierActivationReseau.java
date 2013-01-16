package ma.telecomatlas.abo.batch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ma.telecomatlas.abo.domaine.CarteSim;
import ma.telecomatlas.abo.outils.MsisdnUtil;

/**
 * Fichier d'activation pour la plateforme reseau (HLR), largeur fixe, 80 colonnes.
 *
 * H TAM01 20190312101500 000003                 en-tete
 * D ACT 8921209xxxxxxxxxxxx 60409xxxxxxxxxx 2126xxxxxxxx PO-NJOUM99 P     detail
 * T 000003                                      fin
 *
 * Voir echanges/exemples/LISEZMOI.txt
 */
public class FichierActivationReseau {

    private String dossierSortie = "echanges/reseau/sortie";
    private String codeOperateur = "TAM01";

    public String ecrire(List<CarteSim> sims, List<String> offres) throws IOException {
        File dir = new File(dossierSortie);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Date maintenant = new Date();
        String horo = new SimpleDateFormat("yyyyMMdd_HHmmss").format(maintenant);
        String nom = "ACT_" + codeOperateur + "_" + horo + ".dat";
        File tmp = new File(dir, nom);
        PrintWriter pw = new PrintWriter(new FileWriter(tmp));
        try {
            pw.print(pad("H" + codeOperateur + new SimpleDateFormat("yyyyMMddHHmmss").format(maintenant) + zeros(sims.size(), 6), 80));
            pw.print("\r\n");
            for (int i = 0; i < sims.size(); i++) {
                CarteSim s = sims.get(i);
                String offre = offres.get(i) == null ? "" : offres.get(i);
                StringBuffer l = new StringBuffer();
                l.append("D");
                l.append("ACT");
                l.append(pad(s.getIccid(), 20));
                l.append(pad(s.getImsi(), 15));
                l.append(pad(MsisdnUtil.international(s.getMsisdn()), 12));
                l.append(pad(offre, 15));
                l.append(typeProfil(offre));
                pw.print(pad(l.toString(), 80));
                pw.print("\r\n");
            }
            pw.print(pad("T" + zeros(sims.size(), 6), 80));
            pw.print("\r\n");
        } finally {
            pw.close();
        }
        return nom;
    }

    // P = prepaye, A = abonnement, E = entreprise
    private String typeProfil(String offre) {
        if (offre.startsWith("PP-")) {
            return "P";
        }
        if (offre.startsWith("EN-")) {
            return "E";
        }
        return "A";
    }

    static String pad(String s, int n) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= n) {
            return s.substring(0, n);
        }
        StringBuffer sb = new StringBuffer(s);
        while (sb.length() < n) {
            sb.append(' ');
        }
        return sb.toString();
    }

    static String zeros(int v, int n) {
        String s = String.valueOf(v);
        while (s.length() < n) {
            s = "0" + s;
        }
        return s;
    }

    public void setDossierSortie(String dossierSortie) {
        this.dossierSortie = dossierSortie;
    }

    public void setCodeOperateur(String codeOperateur) {
        this.codeOperateur = codeOperateur;
    }
}
