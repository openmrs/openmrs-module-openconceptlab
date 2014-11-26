<script type="text/javascript">
	jq = jQuery;
	jq(document).ready(function () {
		jq("#previousDetails").dataTable({
			"jQueryUI": true
		});
	});
</script>
<table width="50%" cellspacing="0" cellpadding="0" border="0">
	<tr>
		<td>
			<fieldset id="updates-previous">
				<legend id="previous-title">Previous updates</legend>
				<table width="100%" cellspacing="0" cellpadding="0" border="0" id="previousDetails">
					<thead>
						<tr>
							<th>Date</th>
							<th>Duration</th>
							<th>Status</th>
						</tr>
					</thead>
					<tbody>
						<% summaryList.each { summary -> %>
							<tr>
								<td><a href="details.page?updateId=${summary.updateId }"> ${ summary.startDate}</a></td>
								<td>${ summary.duration } minutes</td>
								<td>${ summary.status }</td>
							</tr>
						<% } %>
					</tbody>
				</table>
			</fieldset>
		</td>
	</tr>
</table>
