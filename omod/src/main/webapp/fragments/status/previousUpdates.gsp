<table width="50%" cellspacing="5" cellpadding="5" border="0">
	<tr>
		<td>
			<fieldset id="updates-previous">
				<legend id="previous-title">Previous updates</legend>
				<table width="100%" cellspacing="5" cellpadding="5" border="0">
					<thead>
						<tr>
							<th>Date</th>
							<th>Duration</th>
							<th>Objects updated</th>
							<th>Status</th>
						</tr>
					</thead>
					<tbody>
					<% items.each { item -> %>
						<tr>
							<td>${ item.update.localDateStarted}</td>
							<td>${ duration } minutes</td>
							<td>${ item.size()}</td>
							<td>${ errors }</td>
						</tr>
					<% } %>
					</tbody>
				</table>
			</fieldset>
		</td>
	</tr>
</table>
