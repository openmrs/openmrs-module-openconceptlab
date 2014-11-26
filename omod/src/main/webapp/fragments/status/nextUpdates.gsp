<style type="text/css">
.formatFields{
	color: #97afd4;
}
.formatFieldsErrors{
	color: red;
}
</style>
<form id="next-updates-form" method="post" action="${ ui.actionLink("openconceptlab", "status", "runUpdates") }">
	<fieldset id="updates-next">
		<legend id="next-title">Next updates</legend>
			<p>
					<% if ( errorItemSize > 0) { %>
						There were <span class="formatFieldsErrors">${ errorItemSize } errors</span> in the last update. <a href="lastUpdateDetails.page">See details</a>
					<% } %>
			</p>
			<p>

				<% if ( manual ) { %>
						No update is scheduled <a href="configure.page">Adjust schedule</a>
				<% } else { %>
						The next update is planned on<span class="formatFields"> ${ nextUpdateDate }</span> at <span class="formatFields">${ nextUpdateTime }</span>. <a href="configure.page">Adjust schedule</a>
				<% } %>
			</p>
			<p>
				<button type="submit" id="update-now">Update now</button>
				<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
			</p>
	</fieldset>
</form>
