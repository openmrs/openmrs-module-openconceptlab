<%
	def userMenuItems = []

	if (context.authenticatedUser) {

		userMenuItems << """<span>Logged in as <i>${ context.authenticatedUser.personName }</i></span>"""
		userMenuItems << """<a href="javascript:ke_logout()">Log Out</a>"""
	}
%>

<div class="ke-toolbar">

	<div class="ke-usertoolbar">
		<% userMenuItems.each { item -> %><div class="ke-toolbar-item">${ item }</div><% } %>
	</div>
	<div style="clear: both"></div>
</div>
<script type="text/javascript">
	function ke_logout() {
		kenyaui.openConfirmDialog({ heading: 'Logout', message: 'Logout and end session?', okCallback: function() {
			ui.navigate('/${ contextPath }/logout');
		}});
	}
</script>