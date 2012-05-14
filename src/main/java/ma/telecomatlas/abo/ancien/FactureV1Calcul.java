package ma.telecomatlas.abo.ancien;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Premier calcul de facture (avril 2012), lance a la main en ligne de commande
 * avant la mise en place du batch. Ne plus utiliser.
 *
 * java FactureV1Calcul 2012-05
 */
public class FactureV1Calcul {

	static String URL = "jdbc:mysql://192.168.1.20:3306/tam_abo";
	static String USER = "root";
	static String MDP = "tam";

	public static void main(String[] args) throws Exception {
		String mois = args.length > 0 ? args[0] : "2012-05";
		Class.forName("com.mysql.jdbc.Driver");
		Connection cx = DriverManager.getConnection(URL, USER, MDP);
		PreparedStatement ps = cx.prepareStatement("SELECT a.ID_ABONNEMENT, a.MSISDN, o.PRIX_MENSUEL, o.MINUTES FROM ABONNEMENT a, OFFRE o "
				+ "WHERE a.CODE_OFFRE = o.CODE_OFFRE AND a.STATUT = 'EN_COURS'");
		ResultSet rs = ps.executeQuery();
		int n = 0;
		while (rs.next()) {
			double prix = rs.getDouble(3);
			int minutes = rs.getInt(4);
			PreparedStatement p2 = cx.prepareStatement("SELECT SUM(QUANTITE) FROM CONSOMMATION WHERE MSISDN = ? AND MOIS = ? AND TYPE_CONSO = 'VOIX_NAT'");
			p2.setString(1, rs.getString(2));
			p2.setString(2, mois);
			ResultSet r2 = p2.executeQuery();
			double conso = 0;
			if (r2.next()) {
				conso = r2.getDouble(1);
			}
			r2.close();
			p2.close();
			double hf = conso > minutes ? (conso - minutes) * 1.20 : 0;
			double total = prix + hf;
			System.out.println(rs.getInt(1) + ";" + rs.getString(2) + ";" + prix + ";" + hf + ";" + total);
			n++;
		}
		rs.close();
		ps.close();
		cx.close();
		System.out.println(n + " abonnements");
	}
}
