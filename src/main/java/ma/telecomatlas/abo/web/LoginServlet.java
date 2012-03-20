package ma.telecomatlas.abo.web;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ma.telecomatlas.abo.outils.ConfigUtil;

/**
 * Connexion aux ecrans : un mot de passe commun a toutes les boutiques (config.properties),
 * le code boutique sert juste a l'affichage.
 */
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.sendRedirect(req.getContextPath() + "/login.jsp");
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String boutique = req.getParameter("boutique");
		String mdp = req.getParameter("motdepasse");
		String attendu = ConfigUtil.get("web.motdepasse", "atlas");
		if (mdp != null && mdp.equals(attendu)) {
			HttpSession s = req.getSession(true);
			s.setAttribute("boutique", boutique == null || boutique.length() == 0 ? "SIEGE" : boutique.toUpperCase());
			s.setAttribute("connexion", new Date());
			s.setMaxInactiveInterval(ConfigUtil.getInt("web.session.minutes", 90) * 60);
			System.out.println("connexion ecrans boutique " + boutique + " depuis " + req.getRemoteAddr());
			resp.sendRedirect(req.getContextPath() + "/index.jsp");
		} else {
			System.out.println("mot de passe incorrect depuis " + req.getRemoteAddr());
			resp.sendRedirect(req.getContextPath() + "/login.jsp?erreur=1");
		}
	}
}
