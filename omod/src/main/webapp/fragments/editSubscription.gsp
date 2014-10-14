<script type="text/javascript">
	jq = jQuery;
	jq(document).ready(function () {
		jq('#automatically-edit :input').prop('disabled', true);
		jq("#auto-A-E").click(function () {
			jq('input:radio[name=manualE]').attr('checked', false);
			jq('#automatically-edit :input').prop('disabled', false);
		});

		jq("#manual-M-E").click(function () {
			jq('input:radio[name=autoE]').attr('checked', false);
			jq('#automatically-edit :input').prop('disabled', true);
		});
	});

</script>

<form id="subscribe-to-ocl-form" method="post" action="#">
	<table id="parent-table" cellpadding="5" cellspacing="10">
		<tr>
			<td>
				<div id="edits-to-be-shown">
					<fieldset>
						<legend>Edit Open Concept Lab subscription</legend>
						<table id="update-olc" cellpadding="5" cellspacing="10">
							<tr>
								<td>Subscription URL:<input type="text" id="url-edit-id" name="urlEdit" size="50"></td>
							</tr>
							<tr>
								<td valign="top">
									<input type="radio" name="optionsE" value="ME" id="manual-M-E" checked ="checked" /> Install updates manually<br /><br />
									<input type="radio" name="optionsE" value="AE" id="auto-A-E" /> Install updates automatically<br /><br />
									<div id="automatically-edit">
										&nbsp;Install updates every
										<select id="days-edit-id" name="daysEdit">
											<% populateDays.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select> days at
										<select id="hours-edit-id" name="hoursEdit">
											<% populateHrs.each { %>
											<option value="${ it }">${ it }</option>
											<% } %>
										</select>:
										<select id="minutes-edit-id" name="minutesEdit">
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
									<input type="submit" id="subscribe-id-edit" value="Save changes" name="saveEdit"/>
									<input type="submit" id="cancel-id-edit" value="Cancel" name="cancelEdit" />
								</td>
							</tr>
						</table>
					</fieldset>
				</div>
			</td>
		</tr>
	</table>
</form>