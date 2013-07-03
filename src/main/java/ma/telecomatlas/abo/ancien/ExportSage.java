package ma.telecomatlas.abo.ancien;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Export des factures vers Sage, format 2013 (remplace par le fichier VT_ du lot de facturation).
 * @deprecated
 */
public class ExportSage {

    private JdbcTemplate jdbcTemplate;

    public void exporter(String mois, String fichier) throws IOException {
        List<Map<String, Object>> l = jdbcTemplate.queryForList("SELECT NUM_FACTURE, DATE_EMISSION, MONTANT_TTC, MONTANT_TVA FROM FACTURE WHERE MOIS = ?", mois);
        FileWriter fw = new FileWriter(fichier);
        try {
            for (Map<String, Object> m : l) {
                fw.write("VT\t" + m.get("DATE_EMISSION") + "\t3421\t" + m.get("NUM_FACTURE") + "\t" + m.get("MONTANT_TTC") + "\t\r\n");
                fw.write("VT\t" + m.get("DATE_EMISSION") + "\t4455\t" + m.get("NUM_FACTURE") + "\t\t" + m.get("MONTANT_TVA") + "\r\n");
            }
        } finally {
            fw.close();
        }
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
}
