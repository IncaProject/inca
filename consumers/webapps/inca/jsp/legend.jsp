<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<c:import var="defaultxml" url="/xml/default.xml"/>
<x:parse var="defaultconfig" xml="${defaultxml}"/>

<html>
<head>
  <link href="/inca/css/inca.css" rel="stylesheet" type="text/css"/>
</head>
<body>

<table cellpadding="1" class="subheader">
  <tr><th align="center" colspan="2"><b>&#160; Legend</b></th></tr>
  <tr><td colspan="2"><b>Icons</b></td></tr>
  <x:forEach select="$defaultconfig/default/incaResult/primaryState" var="state" varStatus="i">
    <x:set var="img" select="string($state/@img)"/>
    <x:set var="bgcolor" select="string($state/@bgcolor)"/>
    <tr valign="top">
      <td bgcolor="${bgcolor}"><img src="/inca/img/${img}"/></td>
      <td class="clear">
        <font color="black"><x:out select="string($state/@description)"/></font>
      </td>
    </tr>
  </x:forEach>
  <tr><td colspan="2"><b>Background Colors</b></td></tr>
  <x:forEach select="$defaultconfig/default/incaResult/secondaryState" var="state" varStatus="i">
    <x:set var="bgcolor" select="string($state/@bgcolor)"/>
    <tr valign="top">
      <td bgcolor="${bgcolor}">&#160;</td>
      <td class="clear">
        <font color="black"><x:out select="string($state/@description)"/></font>
      </td>
    </tr>
  </x:forEach>
</table>   
</body>
</html>
