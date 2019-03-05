package ma.telecomatlas.abo.outils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Manipulation des dates.
 *
 * Attention : SimpleDateFormat n'est pas thread-safe, on en cree un a chaque appel.
 *
 * @author achraibi
 */
public class DateUtil {

	public static final String FMT_FR = "dd/MM/yyyy";
	public static final String FMT_SQL = "yyyy-MM-dd";
	public static final String FMT_MOIS = "yyyy-MM";
	public static final String FMT_HEURE = "dd/MM/yyyy HH:mm";

	public static String format(Date d, String fmt) {
		if (d == null) {
			return "";
		}
		return new SimpleDateFormat(fmt).format(d);
	}

	public static String versFr(Date d) {
		return format(d, FMT_FR);
	}

	/**
	 * Date -> "yyyy-MM-dd" pour les requetes. On ne passe plus de java.sql.Date
	 * au driver depuis la migration (decalage d'un jour, voir G.E. 03/2019).
	 */
	public static String versSql(Date d) {
		if (d == null) {
			return null;
		}
		return new SimpleDateFormat(FMT_SQL).format(d);
	}

	public static Date lire(String s, String fmt) {
		if (s == null || s.trim().length() == 0) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(fmt);
		sdf.setLenient(false);
		try {
			return sdf.parse(s.trim());
		} catch (ParseException e) {
			System.out.println("DateUtil : date invalide " + s + " (" + fmt + ")");
			return null;
		}
	}

	/** accepte dd/MM/yyyy (boutiques) ou yyyy-MM-dd (base) */
	public static Date lireFr(String s) {
		if (s == null) {
			return null;
		}
		if (s.length() >= 10 && s.charAt(4) == '-') {
			return lire(s.substring(0, 10), FMT_SQL);
		}
		return lire(s, FMT_FR);
	}

	/** colonne DATE lue en chaine (rs.getString) */
	public static Date depuisSql(String s) {
		if (s == null) {
			return null;
		}
		if (s.length() > 10) {
			return lire(s.substring(0, 19), "yyyy-MM-dd HH:mm:ss");
		}
		return lire(s, FMT_SQL);
	}

	public static String moisCourant() {
		return format(new Date(), FMT_MOIS);
	}

	public static String moisPrecedent() {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -1);
		return format(c.getTime(), FMT_MOIS);
	}

	public static Date premierJourMois(String mois) {
		return lire(mois + "-01", FMT_SQL);
	}

	public static Date dernierJourMois(String mois) {
		Calendar c = Calendar.getInstance();
		c.setTime(premierJourMois(mois));
		c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
		return c.getTime();
	}

	public static int nbJoursMois(String mois) {
		Calendar c = Calendar.getInstance();
		c.setTime(premierJourMois(mois));
		return c.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	public static int annee(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.YEAR);
	}

	public static int jour(Date d) {
		Calendar c = Calendar.getInstance();
		c.setTime(d);
		return c.get(Calendar.DAY_OF_MONTH);
	}

	public static Date ajouterJours(Date d, int n) {
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		c.add(Calendar.DATE, n);
		return c.getTime();
	}

	public static Date ajouterMois(Date d, int n) {
		Calendar c = new GregorianCalendar();
		c.setTime(d);
		c.add(Calendar.MONTH, n);
		return c.getTime();
	}

	/** nb de mois pleins entre deux dates */
	public static int moisEntre(Date debut, Date fin) {
		if (debut == null || fin == null) {
			return 0;
		}
		Calendar a = Calendar.getInstance();
		a.setTime(debut);
		Calendar b = Calendar.getInstance();
		b.setTime(fin);
		int n = (b.get(Calendar.YEAR) - a.get(Calendar.YEAR)) * 12 + b.get(Calendar.MONTH) - a.get(Calendar.MONTH);
		if (b.get(Calendar.DAY_OF_MONTH) < a.get(Calendar.DAY_OF_MONTH)) {
			n--;
		}
		return n;
	}

	public static int joursEntre(Date debut, Date fin) {
		long ms = fin.getTime() - debut.getTime();
		return (int) Math.round(ms / 86400000.0);
	}

	public static boolean memeMois(Date d, String mois) {
		return d != null && mois.equals(format(d, FMT_MOIS));
	}

	// utilise par l'ancien ecran de saisie (2012)
	public static String aujourdhui() {
		return format(new Date(), FMT_FR);
	}
}
