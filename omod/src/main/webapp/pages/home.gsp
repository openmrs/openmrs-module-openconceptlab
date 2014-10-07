<%
	ui.decorateWith("openconceptlab", "standardPage")
%>
<div class="ke-page-content">
	<% apps.eachWithIndex { app, i ->
		def onClick = "ui.navigate('/" + contextPath + "/" + app.url + "')"
		def iconTokens = app.icon.split(":")
		def iconProvider, icon
		if (iconTokens.length == 2) {
			iconProvider = iconTokens[0]
			icon = "images/" + iconTokens[1]
		}
	%>
	<div style="float: left; margin: 5px;" >
		<button type="button" class="ke-app" onclick="${ onClick }"><img src="${ ui.resourceLink(iconProvider, icon) }" />${ app.label }</button>
	</div>
	<% } %>
</div>