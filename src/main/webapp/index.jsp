<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, org.springframework.jdbc.core.JdbcTemplate, ma.telecomatlas.abo.outils.*" %>
<%@ include file="inc/entete.jsp" %>
<%
	JdbcTemplate jt = (JdbcTemplate) SpringContext.getBean("jdbcTemplate");
	String nom = request.getParameter("nom");
	String cin = request.getParameter("cin");
	String tel = request.getParameter("tel");
	String ville = request.getParameter("ville");
	if (nom == null) nom = "";
	if (cin == null) cin = "";
	if (tel == null) tel = "";
	if (ville == null) ville = "";

	List<String> msg = jt.queryForList("select VALEUR from PARAMETRE where CLE = 'MESSAGE_ACCUEIL'", String.class);
	List<String> villes = jt.queryForList("select distinct VILLE from CLIENT where VILLE is not null order by VILLE", String.class);
%>
<% if (!msg.isEmpty() && msg.get(0) != null && msg.get(0).length() > 0) { %>
<div class="info"><%= msg.get(0) %></div>
<% } %>

<h2>Recherche client</h2>
<form method="get" action="index.jsp">
Nom / raison sociale <input type="text" name="nom" value="<%= nom %>" size="20">
CIN <input type="text" name="cin" value="<%= cin %>" size="10">
N&deg; de ligne <input type="text" name="tel" value="<%= tel %>" size="12">
Ville <select name="ville"><option value=""></option>
<% for (int i = 0; i < villes.size(); i++) { %>
  <option <%= villes.get(i).equals(ville) ? "selected" : "" %>><%= villes.get(i) %></option>
<% } %>
</select>
<input type="submit" value="Chercher">
</form>
<%
	if (nom.length() > 0 || cin.length() > 0 || tel.length() > 0 || ville.length() > 0) {
		// recherche directe, plus rapide que le WS (Adil)
		StringBuffer sql = new StringBuffer("select c.ID_CLIENT, c.CODE_CLIENT, c.TYPE_CLIENT, c.NOM, c.PRENOM, c.RAISON_SOCIALE, c.CIN, c.VILLE, c.STATUT, "
			+ "(select count(*) from ABONNEMENT a where a.ID_CLIENT = c.ID_CLIENT and a.STATUT <> 'RESILIE') as NB_LIGNES "
			+ "from CLIENT c where 1=1");
		List<Object> p = new ArrayList<Object>();
		if (nom.length() > 0) { sql.append(" and (c.NOM like ? or c.RAISON_SOCIALE like ?)"); p.add("%" + nom + "%"); p.add("%" + nom + "%"); }
		if (cin.length() > 0) { sql.append(" and c.CIN = ?"); p.add(cin.toUpperCase()); }
		if (ville.length() > 0) { sql.append(" and c.VILLE = ?"); p.add(ville); }
		if (tel.length() > 0) {
			sql.append(" and c.ID_CLIENT in (select ID_CLIENT from ABONNEMENT where MSISDN = ?)");
			p.add(MsisdnUtil.normaliser(tel));
		}
		sql.append(" order by c.NOM limit 100");
		List<Map<String, Object>> res = jt.queryForList(sql.toString(), p.toArray());
		TraceJsp.info("index.jsp", "recherche nom=" + nom + " cin=" + cin + " tel=" + tel + " : " + res.size());
%>
<p><%= res.size() %> client(s)</p>
<table class="liste">
<tr><th>Code</th><th>Client</th><th>CIN</th><th>Ville</th><th>Lignes</th><th>Statut</th></tr>
<% for (int i = 0; i < res.size(); i++) {
	Map<String, Object> m = res.get(i);
	String libelle = "ENTR".equals(m.get("TYPE_CLIENT")) ? (String) m.get("RAISON_SOCIALE") : m.get("NOM") + " " + m.get("PRENOM");
%>
<tr class="<%= i % 2 == 0 ? "pair" : "impair" %>">
  <td><a href="client.jsp?id=<%= m.get("ID_CLIENT") %>"><%= m.get("CODE_CLIENT") %></a></td>
  <td><%= libelle %></td>
  <td><%= m.get("CIN") == null ? "" : m.get("CIN") %></td>
  <td><%= m.get("VILLE") %></td>
  <td align="center"><%= m.get("NB_LIGNES") %></td>
  <td><%= m.get("STATUT") %></td>
</tr>
<% } %>
</table>
<% } %>
<%@ include file="inc/pied.jsp" %>
