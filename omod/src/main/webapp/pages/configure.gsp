<%
	ui.decorateWith("openconceptlab", "standardPage")
%>


<script type="text/javascript">
	jq = jQuery;
	function checkIfSubscribed() {
		var status = ${ checkIfSubscribed } ;
		 if(status == false) {
			 //show dialog box for editing
			 jq('#unsubscribe-edit').hide();
			 jq('#edits-to-be-shown').hide()
		 }
		else {
			 //show the dialog box for subscription
			 jq('#subscription-to-be-shown').hide();
		 }
	}
	jq(document).ready(function () {
		//switch views
		checkIfSubscribed();
		jq('#automatically-subscribe :input').prop('disabled', true);
		jq('#automatically-edit :input').prop('disabled', true);
		jq("#auto-A-S").click(function() {
			jq('input:radio[name=manual]').attr('checked',false);
			jq('#automatically-subscribe :input').prop('disabled', false);
		});

		jq("#manual-M-S").click(function() {
			jq('input:radio[name=auto]').attr('checked',false);
			jq('#automatically-subscribe :input').prop('disabled', true);
		});

		jq("#manual-M-E").click(function() {
			jq('input:radio[name=auto-E]').attr('checked',false);
			jq('#automatically-edit :input').prop('disabled', true);
		});

		jq("#auto-A-E").click(function() {
			jq('input:radio[name=manual-E]').attr('checked',false);
			jq('#automatically-edit :input').prop('disabled', false);
		});
	});
</script>

<form id="configure-ocl-form" method="post" action="configure.page">
	<table id="parent-table" cellpadding="5" cellspacing="10">
		<tr>
			<td>
				<div id="subscription-to-be-shown">
					<fieldset>
						<legend>Subscribe to Open Concept Lab</legend>
						<table id="subscribe-to-ocl" cellpadding="5" cellspacing="10">
							<tr>
								<td>Subscription URL:<input type="text" id="url-sub-id" name="url-sub" size="50"></td>
							</tr>
							<tr>
								<td valign="top">
									<input type="radio" name="manualS" value="M" id="manual-M-S" checked ="checked" /> Install updates manually<br /><br />
									<input type="radio" name="autoS" value="A" id="auto-A-S" /> Install updates automatically<br /><br />
									<div id="automatically-subscribe">
										&nbsp;Install updates every
										<select id="days-sub-id" name="daysSub">
											<% populateDays.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select> days at
										<select id="hours-sub-id" name="hoursSub">
											<% populateHrs.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select>:
										<select id="minutes-sub-id" name="minutesSub">
											<% populateMinutes.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select>
									</div>
									<br />
									<div id="message-sub">
										<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
									</div>
								</td>
							</tr>
							<tr>
								<td align="right">
									<input type="button" id="cancel-sub-id" value="Cancel" name="cancel-sub"/>
									<input type="button" id="subscribe-sub-id" value="Subscribe" name="subscribe-sub" />
								</td>
							</tr>
						</table>
					</fieldset>
				</div>
			</td>
			<td>
				<div id="edits-to-be-shown">
					<fieldset>
						<legend>Edit Open Concept Lab subscription</legend>
						<table id="update-olc" cellpadding="5" cellspacing="10">
							<tr>
								<td>Subscription URL:<input type="text" id="url-edit-id" name="url-edit" size="50"></td>
							</tr>
							<tr>
								<td valign="top">
									<input type="radio" name="manual-E" value="M" id="manual-M-E" checked ="checked" /> Install updates manually<br /><br />
									<input type="radio" name="auto-E" value="A" id="auto-A-E" /> Install updates automatically<br /><br />
									<div id="automatically-edit">
										&nbsp;Install updates every
										<select id="days-edit-id" name="days-edit">
											<% populateDays.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select> days at
										<select id="hours-edit-id" name="hours-edit">
											<% populateHrs.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select>:
										<select id="minutes-edit-id" name="minutes-edit">
											<% populateMinutes.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select>
									</div>
									<br />
									<div id="message-edit">
										<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
									</div>
								</td>
							</tr>
							<tr>
								<td align="right">
									<input type="button" id="cancel-id-edit" value="Save changes" name="save-edit"/>
									<input type="button" id="subscribe-id-edit" value="Cancel" name="cancel-edit" />
								</td>
							</tr>
						</table>
					</fieldset>
				</div>
			</td>
		</tr>
		<tr>
			<td></td>
			<td>
				<div id="unsubscribe-edit">
					<input type="button" id="unsubscribe-btn" value="UnSubscribe" name="unsubscribe" />
					<i>
						If you unsubscribe, no concepts will be deleted nor changed.<br /> All information about subscription will bw deleted from from your system
					</i>
				</div>
			</td>
		</tr>
	</table>
</form>