package ma.telecomatlas.abo.outils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Montants en dirhams.
 */
public final class MontantUtil {

    private MontantUtil() {
    }

    public static double arrondi(double v) {
        return new BigDecimal(String.valueOf(v)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static double arrondi(double v, int dec) {
        return new BigDecimal(String.valueOf(v)).setScale(dec, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /** "123.45" (format echange boutiques, point decimal) */
    public static String texte(double v) {
        DecimalFormat df = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));
        return df.format(v);
    }

    /** "1 234,50 DH" pour les ecrans */
    public static String affichage(double v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.FRANCE);
        s.setGroupingSeparator(' ');
        DecimalFormat df = new DecimalFormat("#,##0.00", s);
        return df.format(v) + " DH";
    }

    public static double lire(String s) {
        if (s == null || s.trim().equals("")) {
            return 0;
        }
        String t = s.trim().replace(" ", "").replace(",", ".").replace("DH", "").replace("dh", "");
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static double ht(double ttc, double tauxTva) {
        return arrondi(ttc / (1 + tauxTva / 100));
    }

    public static double ttc(double ht, double tauxTva) {
        return arrondi(ht * (1 + tauxTva / 100));
    }

    public static long enCentimes(double v) {
        return Math.round(v * 100);
    }
}
