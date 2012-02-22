package ma.telecomatlas.abo.outils;

import java.io.InputStream;
import java.util.Properties;

/**
 * Lecture de config.properties pour les classes qui ne sont pas des beans Spring.
 */
public class ConfigUtil {

	private static Properties props;

	private static synchronized void charger() {
		if (props != null) {
			return;
		}
		props = new Properties();
		InputStream in = null;
		try {
			in = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties");
			props.load(in);
		} catch (Exception e) {
			System.out.println("!! config.properties introuvable");
			e.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e) {
			}
		}
	}

	public static String get(String cle) {
		charger();
		return props.getProperty(cle);
	}

	public static String get(String cle, String defaut) {
		String v = get(cle);
		return v == null ? defaut : v.trim();
	}

	public static int getInt(String cle, int defaut) {
		try {
			return Integer.parseInt(get(cle, String.valueOf(defaut)));
		} catch (NumberFormatException e) {
			return defaut;
		}
	}

	public static double getDouble(String cle, double defaut) {
		try {
			return Double.parseDouble(get(cle, String.valueOf(defaut)));
		} catch (NumberFormatException e) {
			return defaut;
		}
	}

	public static boolean getBool(String cle) {
		return "true".equalsIgnoreCase(get(cle, "false"));
	}
}
