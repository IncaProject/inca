<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="x" uri="http://java.sun.com/jsp/jstl/xml" %>

<html>
<head>
  <link href="/inca/css/inca.css" rel="stylesheet" type="text/css"/>
</head>
<body>

<table cellpadding="1" class="subheader">
  <tr><td colspan="2"><b>Column Descriptions</b></td></tr>
  <tr valign="top">
    <td class="clear">Name</td>
    <td class="clear">The nickname of the reporter series, a particular configuration (arguments and environment) of a reporter script when executed on a resource.</td>
  </tr><tr>
    <td class="clear">Suite</td>
    <td class="clear">A collection of related reporter series.</td>
  </tr><tr>
    <td class="clear">Resource(s)</td>
    <td class="clear">The name of the resource where the series is executed.  Multiple resources are specified when the series is executed on more than one resource.</td>
  </tr><tr>
    <td class="clear">Target(s)</td>
    <td class="clear">The name of a resource that is tested remotely.  A common use of this field is for centralized or cross-site tests.</td>
  </tr><tr>
    <td class="clear">Script</td>
    <td class="clear">An executable program that tests or measures some aspect of the system or installed software.</td>
  </tr><tr>
    <td class="clear">Frequency</td>
    <td class="clear">The frequency at which the reporter series is executed on the resource.</td>
  </tr><tr>
    <td class="clear">Notification</td>
    <td class="clear">Indicates whether a reporter series has a email notification configured for it or not.</td>
  </tr><tr>
    <td class="clear">Email</td>
    <td class="clear">The email addresses that get notified upon change of status of a reporter series (e.g., a previously succeeding test goes to failed state).</td>
  </tr><tr>
    <td class="clear">Deployed</td>
    <td class="clear">The date at which the reporter series was deployed.</td>
  </tr><tr>
    <td class="clear">Last Run</td>
    <td class="clear">The date at which the reporter series last executed.</td>
  </tr><tr>
    <td class="clear">Description</td>
    <td class="clear">A short description of the reporter script.</td>
  </tr>    
</table>   
</body>
</html>
