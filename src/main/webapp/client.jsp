<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*, org.springframework.jdbc.core.JdbcTemplate, ma.telecomatlas.abo.outils.*, ma.telecomatlas.abo.dao.*, ma.telecomatlas.abo.domaine.*" %>
<%@ include file="inc/entete.jsp" %>
<%
	JdbcTemplate jt = (JdbcTemplate) SpringContext.getBean("jdbcTemplate");
	ClientDao clientDao = (ClientDao) SpringContext.getBean("clientDao");
	int id = 0;
	try { id = Integer.parseInt(request.getParameter("id")); } catch (Exception e) { }
	Client c = clientDao.trouver(id);
	if (c == null) {
%>
<p class="erreur">Client introuvable</p>
<%
	} else {
		// abonnements + libelle du catalogue boutique + iccid
		List<Map<String, Object>> abos = jt.queryForList("select a.*, ob.LIBELLE_COMMERCIAL, ob.PRIX_AFFICHE, s.ICCID, s.STATUT as STATUT_SIM "
			+ "from ABONNEMENT a left join OFFRE_BOUTIQUE ob on ob.CODE_OFFRE = a.CODE_OFFRE and ob.ID_BOUTIQUE is null "
			+ "left join SIM s on s.ID_SIM = a.ID_SIM where a.ID_CLIENT = ? order by a.DATE_DEBUT", id);
%>
<h2><%= c.getNomComplet() %> <span class="petit">(<%= c.getCodeClient() %> - <%= c.getStatut() %>)</span></h2>
<table class="fiche">
<tr><td>Type</td><td><%= c.isEntreprise() ? "Entreprise" : "Particulier" %></td><td>CIN / ICE</td><td><%= c.isEntreprise() ? c.getIce() : c.getCin() %></td></tr>
<tr><td>Adresse</td><td><%= c.getAdresse() %>, <%= c.getVille() %></td><td>T&eacute;l. contact</td><td><%= c.getTelContact() == null ? "" : c.getTelContact() %></td></tr>
<tr><td>E-mail</td><td><%= c.getEmail() == null ? "-" : c.getEmail() %></td><td>Client depuis</td><td><%= DateUtil.versFr(c.getClientDepuis()) %></td></tr>
</table>

<h3>Lignes</h3>
<table class="liste">
<tr><th>Contrat</th><th>N&deg; ligne</th><th>Offre</th><th>Prix</th><th>D&eacute;but</th><th>Fin engagement</th><th>SIM</th><th>Statut</th></tr>
<% for (int i = 0; i < abos.size(); i++) {
	Map<String, Object> a = abos.get(i);
	String lib = a.get("LIBELLE_COMMERCIAL") != null ? (String) a.get("LIBELLE_COMMERCIAL") : (String) a.get("CODE_OFFRE");
%>
<tr class="<%= i % 2 == 0 ? "pair" : "impair" %>">
  <td><%= a.get("NUM_CONTRAT") %></td>
  <td><%= MsisdnUtil.affichage((String) a.get("MSISDN")) %></td>
  <td><%= lib %><% if (a.get("ANCIENNE_OFFRE") != null) { %><br><span class="petit">avant : <%= a.get("ANCIENNE_OFFRE") %></span><% } %></td>
  <td align="right"><%= a.get("PRIX_AFFICHE") == null ? "" : a.get("PRIX_AFFICHE") + " DH" %></td>
  <td><%= DateUtil.versFr(DateUtil.depuisSql(a.get("DATE_DEBUT") == null ? null : a.get("DATE_DEBUT").toString())) %></td>
  <td><%= a.get("DATE_FIN_ENGAGEMENT") == null ? "" : DateUtil.versFr(DateUtil.depuisSql(a.get("DATE_FIN_ENGAGEMENT").toString())) %></td>
  <td class="petit"><%= a.get("ICCID") %><br><%= a.get("STATUT_SIM") %></td>
  <td><%= a.get("STATUT") %></td>
</tr>
<% } %>
</table>

<%
		System.out.println("[client.jsp] " + "fiche " + c.getCodeClient() + " par " + session.getAttribute("boutique"));
	}
%>
<%@ include file="inc/pied.jsp" %>
