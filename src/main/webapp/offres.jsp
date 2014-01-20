<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, org.springframework.jdbc.core.JdbcTemplate, ma.telecomatlas.abo.outils.*" %>
<%@ include file="inc/entete.jsp" %>
<%-- liste des offres : table OFFRE (la référence marketing), nb d'abonnés en cours --%>
<%
	JdbcTemplate jt = (JdbcTemplate) SpringContext.getBean("jdbcTemplate");
	String type = request.getParameter("type");
	String sql = "select o.*, (select count(*) from ABONNEMENT a where a.CODE_OFFRE = o.CODE_OFFRE and a.STATUT = 'EN_COURS') as NB "
		+ "from OFFRE o";
	List<Map<String, Object>> offres;
	if (type != null && type.length() > 0) {
		offres = jt.queryForList(sql + " where o.TYPE_OFFRE = ? order by o.PRIX_MENSUEL", type);
	} else {
		offres = jt.queryForList(sql + " order by o.TYPE_OFFRE, o.PRIX_MENSUEL");
	}
	boolean toutes = "1".equals(request.getParameter("toutes"));
%>
<h2>Offres</h2>
<p>
<a href="offres.jsp">Toutes</a> |
<a href="offres.jsp?type=PREPAYE">Pr&eacute;pay&eacute;</a> |
<a href="offres.jsp?type=POSTPAYE">Forfaits</a> |
<a href="offres.jsp?type=ENTREPRISE">Entreprises</a> |
<a href="offres.jsp?toutes=1">avec offres arr&ecirc;t&eacute;es</a>
</p>
<table class="liste">
<tr><th>Code</th><th>Libell&eacute;</th><th>Type</th><th>Prix/mois</th><th>Internet</th><th>Appels</th><th>SMS</th><th>Engagement</th><th>Lancement</th><th>Abonn&eacute;s</th></tr>
<%
	int n = 0;
	for (int i = 0; i < offres.size(); i++) {
		Map<String, Object> o = offres.get(i);
		boolean actif = ((Number) o.get("ACTIF")).intValue() == 1;
		if (!actif && !toutes) continue;
		int data = ((Number) o.get("DATA_MO")).intValue();
		int min = ((Number) o.get("MINUTES")).intValue();
		int sms = ((Number) o.get("NB_SMS")).intValue();
		n++;
%>
<tr class="<%= actif ? (n % 2 == 0 ? "pair" : "impair") : "inactif" %>">
  <td><%= o.get("CODE_OFFRE") %></td>
  <td><%= o.get("LIBELLE") %></td>
  <td><%= o.get("TYPE_OFFRE") %></td>
  <td align="right"><%= MontantUtil.affichage(((Number) o.get("PRIX_MENSUEL")).doubleValue()) %></td>
  <td align="right"><%= data >= 1000 ? (data / 1000) + " Go" : data + " Mo" %></td>
  <td align="right"><%= min < 0 ? "illimit&eacute;" : min + " min" %></td>
  <td align="right"><%= sms < 0 ? "illimit&eacute;" : String.valueOf(sms) %></td>
  <td align="center"><%= o.get("ENGAGEMENT_MOIS") %> mois</td>
  <td><%= o.get("DATE_LANCEMENT") %></td>
  <td align="right"><%= o.get("NB") %></td>
</tr>
<% } %>
</table>
<p class="petit"><%= n %> offre(s). Les prix affich&eacute;s en boutique peuvent comporter des promotions en cours.</p>
<%@ include file="inc/pied.jsp" %>
