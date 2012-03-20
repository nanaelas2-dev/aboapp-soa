<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>AboApp - Telecom Atlas Mobile</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/css/abo.css">
</head>
<body>
<div id="bandeau">
  <span class="logo">Telecom Atlas Mobile</span> &nbsp;|&nbsp; Gestion des abonnés
  <span class="droite">
    Boutique : <b><%= session.getAttribute("boutique") %></b>
    &nbsp; <a href="<%= request.getContextPath() %>/index.jsp">Recherche</a>
    &nbsp; <a href="<%= request.getContextPath() %>/offres.jsp">Offres</a>
    &nbsp; <a href="<%= request.getContextPath() %>/deconnexion">Quitter</a>
  </span>
</div>
<div id="contenu">
