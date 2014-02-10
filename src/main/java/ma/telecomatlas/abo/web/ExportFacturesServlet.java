package ma.telecomatlas.abo.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.telecomatlas.abo.outils.SpringContext;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Export CSV des factures d'un mois (pour le controle de gestion).
 * /abo/export?mois=2019-09
 *
 * @author nfassi 2014
 */
public class ExportFacturesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String mois = req.getParameter("mois");
        if (mois == null || mois.length() != 7) {
            resp.sendError(400, "parametre mois (yyyy-MM)");
            return;
        }
        JdbcTemplate jt = (JdbcTemplate) SpringContext.getBean("jdbcTemplate");
        List<Map<String, Object>> l = jt.queryForList("SELECT f.NUM_FACTURE, f.TYPE_DOC, c.CODE_CLIENT, c.NOM, c.PRENOM, c.RAISON_SOCIALE, c.VILLE, a.NUM_CONTRAT, "
                + "a.MSISDN, a.CODE_OFFRE, o.LIBELLE_COMMERCIAL, f.MONTANT_HT, f.MONTANT_REMISE, f.MONTANT_TVA, f.DROIT_TIMBRE, f.MONTANT_TTC, f.STATUT "
                + "FROM FACTURE f JOIN CLIENT c ON c.ID_CLIENT = f.ID_CLIENT LEFT JOIN ABONNEMENT a ON a.ID_ABONNEMENT = f.ID_ABONNEMENT "
                + "LEFT JOIN OFFRE_BOUTIQUE o ON o.CODE_OFFRE = a.CODE_OFFRE AND o.ID_BOUTIQUE IS NULL WHERE f.MOIS = ? ORDER BY f.NUM_FACTURE", mois);
        resp.setContentType("text/csv; charset=ISO-8859-1");
        resp.setHeader("Content-Disposition", "attachment; filename=factures_" + mois + ".csv");
        PrintWriter out = resp.getWriter();
        out.println("numero;type;client;nom;ville;contrat;ligne;offre;libelle offre;HT;remise;TVA;timbre;TTC;statut");
        for (int i = 0; i < l.size(); i++) {
            Map<String, Object> m = l.get(i);
            String nom = m.get("RAISON_SOCIALE") != null ? (String) m.get("RAISON_SOCIALE") : m.get("NOM") + " " + m.get("PRENOM");
            out.print(m.get("NUM_FACTURE") + ";" + m.get("TYPE_DOC") + ";" + m.get("CODE_CLIENT") + ";" + nom + ";" + m.get("VILLE") + ";");
            out.print(m.get("NUM_CONTRAT") + ";" + m.get("MSISDN") + ";" + m.get("CODE_OFFRE") + ";" + m.get("LIBELLE_COMMERCIAL") + ";");
            out.print(virgule(m.get("MONTANT_HT")) + ";" + virgule(m.get("MONTANT_REMISE")) + ";" + virgule(m.get("MONTANT_TVA")) + ";");
            out.println(virgule(m.get("DROIT_TIMBRE")) + ";" + virgule(m.get("MONTANT_TTC")) + ";" + m.get("STATUT"));
        }
        out.flush();
    }

    // Excel francais
    private String virgule(Object o) {
        return o == null ? "" : o.toString().replace('.', ',');
    }
}
