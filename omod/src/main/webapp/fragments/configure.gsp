
<script type="text/javascript">
	jq = jQuery;
	jq(document).ready(function () {
		jq("#auto-A-S").click(function() {
			jq('input:radio[name=manual]').attr('checked',false);
		});

		jq("#manual-M-S").click(function() {
			jq('input:radio[name=auto]').attr('checked',false);
		});

		jq("#manual-M-U").click(function() {
			jq('input:radio[name=auto-U]').attr('checked',false);
		});

		jq("#auto-A-U").click(function() {
			jq('input:radio[name=manual-U]').attr('checked',false);
		});
	});
</script>

<form id="configure-ocl-form" method="post" action="configure.page">
	<table id="parent-table" cellpadding="5" cellspacing="10">
		<tr>
			<td>
				<fieldset>
					<legend>Subscribe to Open Concept Lab</legend>
						<table id="subscribe-to-ocl" cellpadding="5" cellspacing="10">
							<tr>
								<td>Subscription URL:${ ui.includeFragment("kenyaui", "widget/field", [ object: command, property: "url" ]) }</td>
							</tr>
							<tr>
								<td valign="top">
									<input type="radio" name="auto" value="A" id="auto-A-S" ${ command.install == 'A' ? 'checked="checked"' : '' }/> Install updates automatically<br />
										<div id="automatically-subscribe">
											 Install updates every
												 <select id="days">
													<% populateDays.each { %>
														<option value="${ it }">${ it }</option>
													<% } %>
												 </select> days at
												<select id="hours">
													<% populateHrs.each { %>
															<option value="${ it }">${ it }</option>
													<% } %>
												</select>:
												<select id="minutes">
													<% populateMinutes.each { %>
															<option value="${ it }">${ it }</option>
													<% } %>
												</select>
										</div>
										<br />
									<div id="message">
										<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
									</div>
										<br />
									<input type="radio" name="manual" value="M" id="manual-M-S" ${ command.install == 'M' ? 'checked="checked"' : '' }/> Install updates manually
								</td>
							</tr>
							<tr>
								<td align="right">
									<input type="button" id="cancel-id" value="Cancel" name="cancel"/>
									<input type="button" id="subscribe-id" value="Subscribe" name="subscribe" />
								</td>
							</tr>
						</table>
				</fieldset>
			</td>
			<td>
				<fieldset>
					<legend>Update Open Concept Lab subscription</legend>
					<table id="update-olc" cellpadding="5" cellspacing="10">
						<tr>
							<td>Subscription URL:${ ui.includeFragment("kenyaui", "widget/field", [ object: command, property: "url" ]) }</td>
						</tr>
						<tr>
							<td valign="top">
								<input type="radio" name="auto-U" value="A" id="auto-A-U" ${ command.install == 'A' ? 'checked="checked"' : '' }/> Install updates automatically<br />
								<div id="automatically-update">
									Install updates every
									<select id="days-update">
										<% populateDays.each { %>
										<option value="${ it }">${ it }</option>
										<% } %>
									</select> days at
									<select id="hours-update">
										<% populateHrs.each { %>
										<option value="${ it }">${ it }</option>
										<% } %>
									</select>:
									<select id="minutes-update">
										<% populateMinutes.each { %>
										<option value="${ it }">${ it }</option>
										<% } %>
									</select>
								</div>
								<br />
								<div id="message-update">
									<i>Avoid updating during data entry hours, because the <br />operation may significantly slow down the system</i>
								</div>
								<br />
								<input type="radio" name="manual-U" value="M" id="manual-M-U" ${ command.install == 'M' ? 'checked="checked"' : '' }/> Install updates manually
							</td>
						</tr>
						<tr>
							<td align="right">
								<input type="button" id="cancel-id-u" value="Discard Changes" name="cancel-U"/>
								<input type="button" id="subscribe-id-u" value="Update" name="subscribe-U" />
							</td>
						</tr>
					</table>
				</fieldset>
			</td>
		</tr>
		<tr>
			<td></td>
			<td>
				<input type="button" id="unsubscribe-btn" value="Unsubscribe" name="unsubscribe" />
				<i>
					If you unsubscribe, no concepts will be deleted nor changed.<br /> All information about subscription will bw deleted from from your system
				</i>
			</td>
		</tr>
	</table>
</form>