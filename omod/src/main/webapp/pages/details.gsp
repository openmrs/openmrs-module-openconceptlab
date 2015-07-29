<%
	ui.decorateWith("appui", "standardEmrPage")
%>
<div class="ke-page-content">
<p><a href = "status.page">Go to the status page</a></p>
<br/>
	${ ui.includeFragment("openconceptlab", "details", [ updateId: updateId, debug: debug]) }
</div>
