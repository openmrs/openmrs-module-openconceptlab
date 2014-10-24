<style type="text/css">
	.formatFields{
		color: #97afd4;
	}
	.formatFieldsErrors{
		color: red;
	}
	.formatalink{
		table-layout: fixed;
	}
	.formatalink tr td a{
		box-shadow: 10px 10px 5px #888888;
	}
</style>
<script type="text/javascript">
	jq = jQuery;
	jq(document).ready(function () {
		jq("#mainTbDetails").dataTable({
			"scrollY": 200,
			"scrollCollapse": true,
			"jQueryUI": true
		});
	});
</script>
<table width="50%" cellspacing="5" cellpadding="5" border="0">
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
<table width="50%" cellspacing="0" cellpadding="0" border="0" id="mainTbDetails">
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
			<table class="formatalink">
				<tr>
					<td><a href="#">|View Locally|</a> </td>
					<td><a href="#">View previous in OCL|</a> </td>
					<td><a href="#">View in OCL|</a> </td>
				</tr>
			</table>
		</td>
	</tr>
	<% } %>
	</tbody>
</table>