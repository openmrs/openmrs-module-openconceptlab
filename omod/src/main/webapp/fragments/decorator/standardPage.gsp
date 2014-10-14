<%
	config.beforeContent = ui.includeFragment("openconceptlab", "header/pageHeader", config)

	config.beforeContent += ui.includeFragment("openconceptlab", "header/headerMenu", config)

	config.pageTitle = "OpenConceptLab"
%>

<!-- Override content layout from kenyaui based on the layout config value -->

<style type="text/css">

<% if (config.layout == "sidebar") { %>
	html {
		background: #FFF url('${ ui.resourceLink("kenyaui", "images/background.png") }') repeat-y;
	}
<% } %>

</style>

<%= config.content %>
