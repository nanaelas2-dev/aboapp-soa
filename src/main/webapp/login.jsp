<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>AboApp - connexion</title>
<link rel="stylesheet" type="text/css" href="css/abo.css">
</head>
<body>
<div id="bandeau"><span class="logo">Telecom Atlas Mobile</span> &nbsp;|&nbsp; Gestion des abonnés</div>
<div id="login">
<h2>Connexion</h2>
<% if (request.getParameter("erreur") != null) { %>
  <p class="erreur">Mot de passe incorrect</p>
<% } %>
<form method="post" action="connexion">
<table>
<tr><td>Code boutique</td><td><input type="text" name="boutique" size="8" maxlength="6"></td></tr>
<tr><td>Mot de passe</td><td><input type="password" name="motdepasse" size="15"></td></tr>
<tr><td></td><td><input type="submit" value="Entrer"></td></tr>
</table>
</form>
<p class="petit">Mot de passe oubli&eacute; : appeler le support DSI (poste 4410)</p>
</div>
</body>
</html>
