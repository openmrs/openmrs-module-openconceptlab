<%
	ui.decorateWith("openconceptlab", "standardPage")
%>
<script type="text/javascript">
	jq = jQuery;
	function checkIfSubscribed() {
		var subscribed = ${ subscription.subscribed };
		if(subscribed) {
			//toggle labels and buttons text
			jq('#subscribe-sub-id').html("Save Changes");
			jq('#title').html("Edit Open Concept Lab Subscription");
			jq('#unsubscribe').show();
		}
	}
	jq(document).ready(function () {
		checkIfSubscribed();
		
		jq("#auto-A-S").click(function () {
			jq('input:radio[name=manualS]').attr('checked', false);
			jq('#automatically-subscribe :input').prop('disabled', false);
		});

		jq("#manual-M-S").click(function () {
			jq('input:radio[name=autoS]').attr('checked', false);
			jq('#automatically-subscribe :input').prop('disabled', true);
		});
		
		var manual = ${subscription.manual};
		if (manual) {
			jq("#manual-M-S").click();
		} else {
			jq("#auto-A-S").click();
		}
	});

</script>

<form id="subscribe-to-ocl-form" method="post" action="configure.page">
	<table id="parent-table" cellpadding="5" cellspacing="10">
		<tr>
			<td>
				<div id="subscription-to-be-shown">
					<fieldset>
						<legend id="title">Subscribe to Open Concept Lab</legend>
						<table id="subscribe-to-ocl" cellpadding="5" cellspacing="10">
							<tr>
								<td>Subscription URL:<input type="text" id="url-sub-id" name="subscription.url" value="${subscription.url}" size="50"></td>
							</tr>
							<tr>
								<td>Token:<input type="text" id="token-id" name="subscription.token" value="${subscription.token}" size="50"></td>
							</tr>
							<tr>
								<td valign="top">
									<input type="radio" name="option" value="M" id="manual-M-S" checked ="checked" /> Install updates manually<br /><br />
									<input type="radio" name="option" value="A" id="auto-A-S" /> Install updates automatically<br /><br />
									<div id="automatically-subscribe">
										&nbsp;Install updates every
										<input type="number" id="subscription.days" name="subscription.days" value="${subscription.days}" min="0" max="365" /> days at
										<input type="number" id="subscription.hours" name="subscription.hours" value="${subscription.hours}" min="0" max="23" size="2" />:
										<input type="number" id="subscription.minutes" name="subscription.minutes" value="${subscription.minutes}" min="0" max="59" size="2" />
									</div>
									<br />
									<div id="message-sub">
										<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
									</div>
								</td>
							</tr>
							<tr>
								<td align="right">
									<button type="submit" id="subscribe-sub-id" name="subscribeSub">Subscribe</button>
									<button type="reset" id="cancel-sub-id" value="Cancel" name="cancelSub">Cancel</button>
								</td>
							</tr>
						</table>
					</fieldset>
				</div>
			</td>
		</tr>
	</table>
</form>

<div id="unsubscribe" style="display: none">
	<form id="unsubscribe-to-ocl-form" method="post" action="configure.page">
		<table id="parent-table" cellpadding="5" cellspacing="10">
			<tr>
				<td>
					<fieldset>
						<legend>Unsubscribe</legend>
						<table cellpadding="5" cellspacing="10">
							<tr>
								<td>
									<input type="hidden" name="unsubscribe" value="true" />
									<input type="submit" id="unsubscribe-btn" value="Unsubscribe"/>
									<br />
								</td>
							</tr>
							<tr>
								<td>
									<i>
										If you unsubscribe, no concepts will be deleted nor changed.<br /> All information about the subscription will be deleted from from your system
									</i>
								</td>
							</tr>
						</table>
					</fieldset>
				</td>
			</tr>
		</table>
	</form>
</div>