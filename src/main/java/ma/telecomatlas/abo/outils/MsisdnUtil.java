package ma.telecomatlas.abo.outils;

/**
 * Numeros mobiles : on stocke toujours le format national 0XXXXXXXXX (10 chiffres).
 * La plateforme reseau veut le format international 212XXXXXXXXX.
 */
public class MsisdnUtil {

    public static String normaliser(String num) {
        if (num == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < num.length(); i++) {
            char c = num.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        String n = sb.toString();
        if (n.startsWith("00212")) {
            n = "0" + n.substring(5);
        } else if (n.startsWith("212") && n.length() == 12) {
            n = "0" + n.substring(3);
        }
        return n;
    }

    public static boolean estMobile(String num) {
        String n = normaliser(num);
        return n != null && n.length() == 10 && (n.startsWith("06") || n.startsWith("07"));
    }

    public static String international(String num) {
        String n = normaliser(num);
        if (n == null || n.length() != 10) {
            return n;
        }
        return "212" + n.substring(1);
    }

    /** 06 12 34 56 78 */
    public static String affichage(String num) {
        String n = normaliser(num);
        if (n == null || n.length() != 10) {
            return num;
        }
        return n.substring(0, 2) + " " + n.substring(2, 4) + " " + n.substring(4, 6) + " " + n.substring(6, 8) + " " + n.substring(8);
    }
}
