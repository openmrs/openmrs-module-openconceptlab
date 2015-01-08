<script type="text/javascript">
	jq = jQuery;
	jq(function() {
		var progressbar = jq( "#progressbar" );
		progressbar.progressbar();
		
		var updateProgressbar = function() {
			jq.getJSON('${ui.actionLink("openconceptlab", "status", "getUpdateProgress")}', function(result) {
				jq('#completed').html(result.progress);
				jq('#time').html(result.time);
				var progressbar = jq( "#progressbar" );
				progressbar.progressbar( "value", result.progress);	
				if (result.progress == 100) {
					setTimeout(function() { window.location.reload(); }, 1000);
				}
			});
			
			setTimeout(updateProgressbar, 1000);
		};
		
		updateProgressbar();
	});
</script>
<style type="text/css">
	.progress-label {
		position: absolute;
		text-align: center;
		font-weight: bold;
		left: 20%;
		text-shadow: 1px 1px 0 #fff;
}
</style>

<fieldset id="updates-progressing">
	<legend id="progress-title">Update in progress</legend>
		<p>
			Upgrade is in progress for <span id="time"></span> seconds now
		</p>
		<p>
				Completed <span id="completed"></span> %
		</p>
		<p>
			<div id="progressbar">
			</div>
		</p>
</fieldset>

