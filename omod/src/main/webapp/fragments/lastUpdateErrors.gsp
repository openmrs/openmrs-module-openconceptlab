<style type="text/css">
	.formatFields{
		color: #97afd4;
	}
	.formatFieldsErrors{
		color: red;
	}
</style>
<table width="50%" cellspacing="5" cellpadding="5" border="0">
	<tr>
		<td>
			The update started on<span class="formatFields"> ${ startDate } </span>at<span class="formatFields"> ${ timeStarted }</span>. It completed in ${ duration }
		</td>
	</tr>
	<tr>
		<td>
			There were <span class="formatFields">${ allItemsUpdatedSize }</span> and <span class="formatFieldsErrors">${ allErrorItems } in error queue</span>
		</td>
	</tr>
</table>
<table width="50%" cellspacing="5" cellpadding="5" border="0">
	<thead>
	<tr>
		<th>Type</th>
		<th>Name Description</th>
		<th>Status</th>
		<th>Action</th>
	</tr>
	</thead>
	<tbody>
	<% allItemsUpdated.each { items -> %>
	<tr>
		<td>${ items.update.localDateStarted}</td>
		<td>${ items.update.localDateStopped}</td>
		<td>${ items.update.oclDateStarted}</td>
		<td>
			<table>
				<tr>
					<td><a href="#">View Locally</a> </td>
					<td><a href="#">View previous in OCL</a> </td>
					<td><a href="#">View in OCL</a> </td>
				</tr>
			</table>
		</td>
	</tr>
	<% } %>
	</tbody>
</table>