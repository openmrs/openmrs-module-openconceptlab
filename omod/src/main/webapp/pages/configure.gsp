<%
	ui.decorateWith("appui", "standardEmrPage")
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
			jq('#status-box').show();
		}
	}
	jq(document).ready(function () {
		checkIfSubscribed();
		
		jq("#auto-A-S").click(function () {
			jq('input:radio[name=manualS]').attr('checked', false);
			jq('#automatically-subscribe').show();
		});

		jq("#manual-M-S").click(function () {
			jq('input:radio[name=autoS]').attr('checked', false);
			jq('#automatically-subscribe').hide();
		});
		
		var manual = ${subscription.manual};
		if (manual) {
			jq("#manual-M-S").click();
		} else {
			jq("#auto-A-S").click();
		}
	});

</script>
<br/>
<form id="subscribe-to-ocl-form" method="post" action="configure.page">
	<div id="subscription-to-be-shown">
		<fieldset>
			<legend id="title">Subscribe to Open Concept Lab</legend>

					<p>Subscription URL:<input type="text" id="url-sub-id" name="subscription.url" value="${subscription.url}" size="50"></p>
					<p>Token:<input type="text" id="token-id" name="subscription.token" value="${subscription.token}" size="50"></p>
					<p>
						<input type="radio" name="option" value="M" id="manual-M-S" checked ="checked" /> Install updates manually<br /><br />
						<input type="radio" name="option" value="A" id="auto-A-S" /> Install updates automatically<br /><br />
						<div id="automatically-subscribe">
							&nbsp;Install updates every
							<input type="number" id="subscription.days" name="subscription.days" value="${subscription.days}" min="0" max="365" style="min-width: 5%; display: inline" /> days at
							<input type="number" id="subscription.hours" name="subscription.hours" value="${subscription.hours}" min="0" max="23" style="min-width: 5%; display: inline" />:
							<input type="number" id="subscription.minutes" name="subscription.minutes" value="${subscription.minutes}" min="0" max="59" style="min-width: 5%; display: inline" />
						</div>
						<br />
						<div id="message-sub">
							<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
						</div>
					</p>

					<div align="right">
						<button type="submit" id="subscribe-sub-id" name="subscribeSub">Subscribe</button>
						<input type="button" value="Cancel Changes" onclick="window.location.href='status.page'" />
					</div>
		</fieldset>
	</div>
</form>

<div id="unsubscribe" style="display: none">
	<form id="unsubscribe-to-ocl-form" method="post" action="configure.page">

		<fieldset>
			<legend>Unsubscribe</legend>
					<p>
						<input type="hidden" name="unsubscribe" value="true" />
						<input type="submit" id="unsubscribe-btn" value="Unsubscribe"/>
						<br />
					</p>
					<p>
						<i>
							If you unsubscribe, no concepts will be deleted nor changed.<br /> All information about the subscription will be deleted from your system
						</i>
					</p>
		</fieldset>
	</form>
</div>