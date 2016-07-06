<style type="text/css">
.formatFields{
	color: #97afd4;
}
.formatFieldsErrors{
	color: red;
}
.ui-widget-header,.ui-state-default, ui-button{
	background:#EEA616;
	border: 1px solid #b9cd6d;
	color: #FFFFFF;
	font-weight: bold;
}
</style>
<script type="text/javascript">
	jq = jQuery;
	jq(function() {
		jq( "#dialog" ).dialog({
               autoOpen: false, 
			   resizable: false,
			   draggable: false,
			   closeOnEscape: true
            });
		jq("#update").click(function(){
			if(!${isLastUpdateSuccessful}){
				jq( "#dialog" ).dialog("open");
			}else{
				jq.post('${ui.actionLink("openconceptlab", "status", "runUpdates")}', function(){
					location.reload();
				});
			}
		}); 
		jq("#previous").click(function(){
			jq.post('${ui.actionLink("openconceptlab", "status", "runUpdates")}', function(){
				location.reload();
			});
		});
		jq("#ignore").click(function(){
			jq.post('${ui.actionLink("openconceptlab", "status", "runUpdates", ["ignoreErrors": "true"])}', function(){
				location.reload();
			});
		});
	});
</script>
<form id="next-updates-form">
	<fieldset id="updates-next">
		<legend id="next-title">Next updates</legend>
			<p>
					<% if ( errorItemSize > 0) { %>
						There were <span class="formatFieldsErrors">${ errorItemSize } errors</span> in the last update. <a href="details.page?updateId=${lastUpdateId}">See details</a>
					<% } %>
			</p>
			<p>

				<% if ( manual ) { %>
						No update is scheduled <a href="configure.page">Adjust configuration</a>
				<% } else { %>
						The next update is planned on<span class="formatFields"> ${ nextUpdateDate }</span> at <span class="formatFields">${ nextUpdateTime }</span>. <a href="configure.page">Adjust configuration</a>
				<% } %>
			</p>
			<p>
				<button type="button" id="update">Update now</button>
				
				<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
				<% if ( errorItemSize > 0) { %>
					<br/>
					<i>All items from the last failing update will be imported again. Make sure all errors have been fixed locally or in the OpenConceptLab server.</i>			
				<% } %>
				<div id="dialog" title="Warning" class="dialog">
					<p>Do you want to try install previous updates or ignore them? </p>
					<button id="previous">update previous</button>
					<button id="ignore">ignore</button>
				</div>
			</p>			
	</fieldset>
</form>

