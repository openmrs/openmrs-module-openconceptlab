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

		<p>
			The update started on<span class="formatFields"> ${ startDate } </span>at<span class="formatFields"> ${ timeStarted }</span>. It completed in <span class="formatFields">${ duration }</span>
		</p>

		<p>
			There were <span class="formatFields">${ allItemsUpdatedSize }</span> objects updated and <span class="formatFieldsErrors">${ allErrorItems } in error.</span>
		</p>
			<table width="100%" cellspacing="0" cellpadding="0" border="0" id="mainTbDetails">
				<thead>
					<tr>
						<th>Type (UUID)</th>
						<th>Status</th>
					</tr>
				</thead>
				<tbody>
					<% allItemsUpdated.each { item -> %>
					<tr>
						<td class="small">${ item.type }\n
							(${ item.uuid })\n
							<a href="${ item.versionUrl }">View in OCL</a>
						</td>
						<td>${ item.status}</td>
					</tr>
					<% } %>
				</tbody>
			</table>
