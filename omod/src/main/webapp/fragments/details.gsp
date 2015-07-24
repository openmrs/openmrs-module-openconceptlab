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
			There were <span class="formatFields">${ allItemsUpdatedSize }</span> objects updated and <span class="formatFieldsErrors">${ allErrorItems } in error
			<% if (ignoredErrorsCount > 0) { %>
			(ignored ${ignoredErrorsCount} errors)
			<% } %>
			</span>
		</p>
		<% if (allErrorItems > 0) { %>
		<p>
		Please fix all errors so that you can continue to get updates. It may require fixing concepts locally (e.g. creating concept classes, changing concept names to eliminate duplicates) or contacting OCL and asking them to for fixes.
			<form method="POST">
				<input type="hidden" name="updateId" value="${updateId}" />
				<input type="hidden" name="ignoreAllErrors" value="true" />
				<input type="submit" value="Ignore all errors (testing only)" />
			</form>
		</p>
		<p>&nbsp;</p>
		<% } %>
			<table width="100%" cellspacing="0" cellpadding="0" border="0" id="mainTbDetails">
				<thead>
					<tr>
						<th>Errors (listing limited to first 1000 errors)</th>
					</tr>
				</thead>
				<tbody>
					<% allItemsUpdated.each { item -> %>
					<tr>
						<td class="small">${ item.type } 
							(${ item.uuid }) 
							<a href="${ item.versionUrl }">View in OCL</a>\n
							${ item.status}
						</td>
					</tr>
					<% } %>
				</tbody>
			</table>
