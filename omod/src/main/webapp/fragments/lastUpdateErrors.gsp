<table width="50%" cellspacing="5" cellpadding="5" border="0">
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
	<% allErrorItems.each { items -> %>
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