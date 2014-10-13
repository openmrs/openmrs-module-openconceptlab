
<script type="text/javascript">
	jq = jQuery;
	jq(document).ready(function () {
		jq('#automatically-subscribe :input').prop('disabled', true);
		jq("#auto-A-S").click(function () {
			jq('input:radio[name=manualS]').attr('checked', false);
			jq('#automatically-subscribe :input').prop('disabled', false);
		});

		jq("#manual-M-S").click(function () {
			jq('input:radio[name=autoS]').attr('checked', false);
			jq('#automatically-subscribe :input').prop('disabled', true);
		});
	});

</script>

<form id="subscribe-to-ocl-form" method="post" action="#">
	<table id="parent-table" cellpadding="5" cellspacing="10">
		<tr>
			<td>
				<div id="subscription-to-be-shown">
					<fieldset>
						<legend>Subscribe to Open Concept Lab</legend>
						<table id="subscribe-to-ocl" cellpadding="5" cellspacing="10">
							<tr>
								<td>Subscription URL:<input type="text" id="url-sub-id" name="urlSub" size="50"></td>
							</tr>
							<tr>
								<td valign="top">
									<input type="radio" name="option" value="M" id="manual-M-S" checked ="checked" /> Install updates manually<br /><br />
									<input type="radio" name="option" value="A" id="auto-A-S" /> Install updates automatically<br /><br />
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
									<input type="submit" id="subscribe-sub-id" value="Subscribe" name="subscribeSub"/>
									<input type="reset" id="cancel-sub-id" value="Cancel" name="cancelSub" />
								</td>
							</tr>
						</table>
					</fieldset>
				</div>
			</td>
		</tr>
	</table>
</form>