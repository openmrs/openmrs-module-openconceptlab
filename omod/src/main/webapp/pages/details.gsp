<%
	ui.decorateWith("appui", "standardEmrPage")
%>
<div class="ke-page-content">
	${ ui.includeFragment("openconceptlab", "details", [ updateId: updateId]) }
</div>
