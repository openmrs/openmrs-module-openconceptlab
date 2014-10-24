<%
	ui.includeJavascript("openconceptlab", "jquery-1.10.2.min.js", 50)
	ui.includeJavascript("openconceptlab", "jquery-ui-1.10.3.min.js", 50)
	ui.includeJavascript("openconceptlab", "jquery.dataTables.min.js", 49)

	ui.includeCss("openconceptlab", "jquery-ui-1.10.3.custom.css", 50)
	ui.includeCss("openconceptlab", "jquery.dataTables.min.css", 50)

	config.beforeContent = ui.includeFragment("openconceptlab", "header/pageHeader", config)

	config.pageTitle = "OpenConceptLab"
%>

<!DOCTYPE html>
<html>
	<head>
		<meta http-equiv="content-type" content="text/html; charset=UTF-8" />
<% if (config.pageTitle) { %><title>${ config.pageTitle }</title><% } %>
<% if (config.faviconIco) { %><link rel="shortcut icon" type="image/ico" href="${ config.faviconIco }"><% } %>
<% if (config.faviconPng) { %><link rel="icon" type="image/png" href="${ config.faviconPng }"><% } %>

<%= ui.resourceLinks() %>
</head>
<body>
	<script type="text/javascript">
		var OPENMRS_CONTEXT_PATH = '${ contextPath }';
	</script>

<% if (config.beforeContent) { %>
${ config.beforeContent }
<% } %>

<div class="ke-page-container">
<%= config.content %>
</div>
</body>
</html>

