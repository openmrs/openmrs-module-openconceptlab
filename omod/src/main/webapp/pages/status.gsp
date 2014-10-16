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
		if(!isSubscribed){
		   ui.navigate('openconceptlab', 'configure');
	}
}

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