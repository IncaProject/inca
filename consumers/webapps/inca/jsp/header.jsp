<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="inca" tagdir="/WEB-INF/tags/inca" %>

<c:if test="${empty param.noHeader}">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
  <inca:getUrl var="url"/>
  <link href="${url}/css/nav.css" rel="stylesheet" type="text/css"/>
  <link href="${url}/css/inca.css" rel="stylesheet" type="text/css"/>
  <script type="text/javascript" src="${url}/js/graph.js"></script>
  <script type="text/javascript" src="${url}/js/check-box.js"></script>
  <script type="text/javascript" src="${url}/js/sorttable.js"></script>
  <script type="text/javascript" src="${url}/js/prototype.js"></script>
  <title>${empty param.title ? 'Inca Status Pages' : param.title}</title>
</head>
<body>
  <inca:executeXslTemplate name="header" url="${url}"/>
</c:if>
