package ma.telecomatlas.abo.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirige vers login.jsp si pas de session. Les services web ne passent pas ici.
 */
public class AuthFilter implements Filter {

    public void init(FilterConfig fc) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        if (uri.endsWith("/login.jsp") || uri.endsWith("/erreur.jsp") || uri.indexOf("/css/") >= 0) {
            chain.doFilter(request, response);
            return;
        }
        if (req.getSession(false) == null || req.getSession(false).getAttribute("boutique") == null) {
            ((HttpServletResponse) response).sendRedirect(req.getContextPath() + "/login.jsp");
            return;
        }
        chain.doFilter(request, response);
    }

    public void destroy() {
    }
}
