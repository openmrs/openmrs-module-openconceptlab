<%
	ui.decorateWith("openconceptlab", "standardPage")
%>

<script type="text/javascript">
	jq = jQuery;
	function checkIfSubscribed() {
		var status = ${ checkIfSubscribed } ;
		 if(status == false) {
			 //show dialog box for editing
			 jq('#edit').hide();
			 jq('#unsubscribe').hide()
		 }
		else {
			 //show the dialog box for subscription
			 jq('#subscribe').hide();
		 }
	}
	jq(document).ready(function () {
		//switch views
		checkIfSubscribed();
	});

</script>

<div id="subscribe">
	${ ui.includeFragment("openconceptlab", "subscribeToOcl") }
</div>
<div id="edit">
	${ ui.includeFragment("openconceptlab", "editSubscription") }
</div>
<div id="unsubscribe">
	${ ui.includeFragment("openconceptlab", "unsubscribe") }
</div>