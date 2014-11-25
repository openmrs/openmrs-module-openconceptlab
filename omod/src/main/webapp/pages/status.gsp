<%
	ui.decorateWith("appui", "standardEmrPage")
%>
<script type="text/javascript" >
	jq = jQuery;
	var isRunning = ${ checkIfUpdatesIsRunning };
	var isSubscribed = ${ checkIfSubscribed };
	function checkIfRunning() {
		if(isRunning) {
			jq('#updates-in-progress').show();
		}
		else {
			jq('#updates-next').show();
		}
	}
	function checkIfSubscribed() {
		if (!isSubscribed) {
			jq('#not-subscribed').show();
			jq('#updates-in-progress').hide();
			jq('#updates-next').hide();
			jq('#previous-update').hide();
		}
	}

	jq(document).ready(function () {
		checkIfRunning();
		checkIfSubscribed()
	});

</script>
<div id="updates-in-progress" style="display: none">
	${ ui.includeFragment("openconceptlab", "status/updatesInProgress")}
</div>
<div id="updates-next" style="display: none">
	${ ui.includeFragment("openconceptlab", "status/nextUpdates")}
</div>
<div id="previous-update">
	${ ui.includeFragment("openconceptlab", "status/previousUpdates")}
</div>
<div id="not-subscribed" style="display: none">
	<p>
		You are not subscribed to Open Concept Lab. <a href="configure.page">Please go to the configuration page to setup the subscription</a>
	</p>
</div>