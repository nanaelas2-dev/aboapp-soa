<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<html>
<head><title>Erreur</title><link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/abo.css"></head>
<body>
<div id="bandeau"><span class="logo">Telecom Atlas Mobile</span></div>
<div id="contenu">
<h2>Une erreur est survenue</h2>
<p>Merci de r&eacute;essayer. Si le probl&egrave;me persiste, appeler le support DSI (poste 4410).</p>
<% if (exception != null) { ma.telecomatlas.abo.outils.TraceJsp.erreur("erreur.jsp", new Exception(exception)); %>
<p class="petit"><%= exception.getClass().getName() %> : <%= exception.getMessage() %></p>
<% } %>
<p><a href="<%= request.getContextPath() %>/index.jsp">Retour</a></p>
</div>
</body>
</html>
