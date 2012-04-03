package ma.telecomatlas.abo.ancien;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import ma.telecomatlas.abo.domaine.Client;
import ma.telecomatlas.abo.outils.DateUtil;

/**
 * Reprise des clients du fichier Excel des boutiques (2012), enregistre en CSV (separateur ;).
 * Colonnes : CIN;NOM;PRENOM;DATE_NAISSANCE;ADRESSE;VILLE;TEL
 */
public class ImportClientsXls {

	public List<Client> lire(String fichier) {
		List<Client> res = new ArrayList<Client>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fichier), "Cp1252"));
			String l = br.readLine(); // entete
			int num = 1;
			while ((l = br.readLine()) != null) {
				num++;
				String[] t = l.split(";");
				if (t.length < 6) {
					System.out.println("ligne " + num + " ignoree");
					continue;
				}
				Client c = new Client();
				c.setCin(t[0].trim().toUpperCase());
				c.setNom(t[1].trim());
				c.setPrenom(t[2].trim());
				c.setDateNaissance(DateUtil.lire(t[3].trim(), DateUtil.FMT_FR));
				c.setAdresse(t[4].trim());
				c.setVille(t[5].trim());
				if (t.length > 6) {
					c.setTelContact(t[6].trim());
				}
				c.setTypeClient("PART");
				c.setStatut("ACTIF");
				res.add(c);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (Exception e) {
			}
		}
		return res;
	}
}
