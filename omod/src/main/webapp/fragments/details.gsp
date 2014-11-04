<style type="text/css">
	.formatFields{
		color: #97afd4;
	}
	.formatFieldsErrors{
		color: red;
	}
</style>
<script type="text/javascript">
	var OPENMRS_CONTEXT_PATH = '${ contextPath }';
	jq = jQuery;
	jq(document).ready(function () {
		jq("#mainTbDetails").dataTable({
			"jQueryUI": true
		});
	});
</script>
<br /><br />
<table width="50%" cellspacing="0" cellpadding="0" border="0">
	<tr>
		<td>
			The update started on<span class="formatFields"> ${ startDate } </span>at<span class="formatFields"> ${ timeStarted }</span>. It completed in <span class="formatFields">${ duration }</span>
		</td>
	</tr>
	<tr>
		<td>
			There were <span class="formatFields">${ allItemsUpdatedSize }</span> objects updated and <span class="formatFieldsErrors">${ allErrorItems } in error queue</span>
		</td>
	</tr>
</table>
<br />
<table width="50%" cellspacing="0" cellpadding="0" border="0">
	<tr>
		<td>
			<table width="100%" cellspacing="0" cellpadding="0" border="0" id="mainTbDetails">
				<thead>
				<tr>
					<th>Type</th>
					<th>Name</th>
					<th>Description</th>
					<th>Status</th>
					<th>Action</th>
				</tr>
				</thead>
				<tbody>
				<% allItemsUpdated.each { item -> %>
				<tr>
					<td>${ item.type }</td>
					<td>${ item.name }</td>
					<td>${ item.description }</td>
					<td>${ item.status}</td>
					<td>
						<table>
							<tr>
								<td>
									<button onclick="window.location='/' + OPENMRS_CONTEXT_PATH + '/dictionary/concept.htm?conceptId=${ item.conceptId }'">View Locally</button>
								</td>
								<td>
									<button onclick="window.location='http://www.maternalconceptlab.com/search.php?q=${ item.conceptId }'">View previous in OCL</button>
								</td>
								<td>
									<button onclick="window.location='http://www.maternalconceptlab.com/search.php?q=${ item.conceptId }'">View in OCL</button>
								</td>
							</tr>
						</table>
					</td>
				</tr>
				<% } %>
				</tbody>
			</table>
		</td>
	</tr>
</table>