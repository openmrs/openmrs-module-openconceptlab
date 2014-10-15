<form id="unsubscribe-to-ocl-form" method="post" action="${ ui.pageLink("openconceptlab", "configure") }">
	<table cellpadding="5" cellspacing="10">
		<tr>
			<td>
				<button type="submit" id="unsubscribe-btn" name="unsubscribe">UnSubscribe</button>
					<br />
			</td>
		</tr>
		<tr>
			<td>
				<i>
					If you un subscribe, no concepts will be deleted nor changed.<br /> All information about subscription will be deleted from from your system
				</i>
			</td>
		</tr>
	</table>
</form>
<div id="dialog-confirm"></div>

<script type="text/javascript">
	jq(function() {
		jq('#unsubscribe-btn').click(function() {
			jq('#dialog-confirm').html('All information about subscription will be deleted');
			jq("#dialog-confirm").dialog({
				resizable: false,
				modal: true,
				title: "Unsubscribe",
				height: 250,
				width: 400,
				buttons: {
					"Yes": function () {
						jq(this).dialog('close');
						callback(true);
					},
					"No": function () {
						jq(this).dialog('close');
						callback(false);
					}
				}
			});
		});
	});
	function callback(data) {
	  //code to reset subscription values to null
	}
</script>
