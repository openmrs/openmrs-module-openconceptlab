<script type="text/javascript">
	jq = jQuery;

	jq(document).ready(function () {
		jq("#previousDetails").dataTable({
			"scrollY": "200px",
			"scrollCollapse": true,
			"paging": false
		});
	});
</script>
<table width="50%" cellspacing="5" cellpadding="5" border="0">
	<tr>
		<td>
			<fieldset id="updates-previous">
				<legend id="previous-title">Previous updates</legend>
				<table width="100%" cellspacing="5" cellpadding="5" border="0" id="previousDetails">
					<thead>
						<tr>
							<th>Date</th>
							<th>Duration</th>
							<th>Objects updated</th>
							<th>Status</th>
						</tr>
					</thead>
					<tbody>
					<% summaryList.each { summary -> %>
						<tr id="${summary.updateId }">
							<td><a href="details.page?updateId=${summary.updateId }"> ${ summary.startDate}</a></td>
							<td>${ summary.duration } minutes</td>
							<td>${ summary.items }</td>
							<td>${ summary.status }</td>
						</tr>
					<% } %>
					</tbody>
				</table>
			</fieldset>
		</td>
	</tr>
</table>
