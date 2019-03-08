package ma.telecomatlas.abo.outils;

import org.apache.log4j.Logger;

/**
 * Traces des pages JSP (le System.out ne compile plus dans les JSP sur le tomcat 9).
 * G.E. 03/2019
 */
public class TraceJsp {

	private static Logger log = Logger.getLogger("jsp");

	public static void info(String page, String msg) {
		log.info(page + " : " + msg);
	}

	public static void erreur(String page, Exception e) {
		log.error(page + " : " + e.getMessage(), e);
	}
}
