package ma.telecomatlas.abo.outils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Pour recuperer les beans dans les JSP et les servlets.
 */
public class SpringContext implements ApplicationContextAware {

	private static ApplicationContext contexte;

	public void setApplicationContext(ApplicationContext ctx) {
		contexte = ctx;
	}

	public static Object getBean(String nom) {
		return contexte.getBean(nom);
	}

	public static ApplicationContext get() {
		return contexte;
	}
}
