package ma.telecomatlas.abo.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class CharsetFilter implements Filter {

	private String encodage = "UTF-8";

	public void init(FilterConfig fc) throws ServletException {
		if (fc.getInitParameter("encodage") != null) {
			encodage = fc.getInitParameter("encodage");
		}
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		req.setCharacterEncoding(encodage);
		chain.doFilter(req, resp);
	}

	public void destroy() {
	}
}
