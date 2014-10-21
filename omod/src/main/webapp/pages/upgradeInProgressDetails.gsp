<%
	ui.decorateWith("openconceptlab", "standardPage")
%>
<table cellpadding="5" cellspacing="10">
	<thead>
		<tr>
			<th>Local Date Started</th>
			<th>Local Date Stopped</th>
			<th>OCL Date Started</th>
			<th>Type</th>
			<th>Version Url</th>
			<th>State</th>
		</tr>
	</thead>
	<tbody>
	<% updatedItems.each { items -> %>
		<tr>
			<td>${ items.update.localDateStarted}</td>
			<td>${ items.update.localDateStopped}</td>
			<td>${ items.update.oclDateStarted}</td>
			<td>${ items.type}</td>
			<td>${ items.versionUrl}</td>
			<td>${ items.state}</td>
		</tr>
	<% } %>
	</tbody>
</table>