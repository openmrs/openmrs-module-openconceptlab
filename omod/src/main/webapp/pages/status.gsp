<%
	ui.decorateWith("appui", "standardEmrPage")
%>
<% if (checkIfSubscribed) { %>
	<% if (checkIfUpdatesIsRunning) { %>
		${ ui.includeFragment("openconceptlab", "status/updatesInProgress")}
	<% } else { %>
		${ ui.includeFragment("openconceptlab", "status/nextUpdates")}
	<% } %>

	${ ui.includeFragment("openconceptlab", "status/previousUpdates")}
<% } else { %>
	<p>
		You are not subscribed to Open Concept Lab. <a href="configure.page">Please go to the configuration page to setup the subscription</a>
	</p>
<% } %>