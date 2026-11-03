<%@page import = "java.io.IOException" %>
<%@page import = "java.sql.Connection" %>
<%@page import = "java.sql.DriverManager" %>
<%@page import = "java.sql.PreparedStatement" %>
<%@page import = "java.sql.ResultSet" %>
<%@page import = "java.sql.SQLException" %>
<%@page import = "java.sql.Statement" %>
<%@page import = "db.ConnectionManager" %>
<%@page import = "org.apache.commons.text.StringEscapeUtils" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>



<!DOCTYPE html>
<html lang="en">

	<head>
		<meta charset="utf-8">
		<title>Harness Sample To-Do List</title>
		<link rel="stylesheet" href="../css/bootstrap.min.css">
		
		<style type="text/css">
			body {
				padding-top: 40px;
				padding-bottom: 40px;
				background-color: #f5f5f5;
			}
		</style>
		
	</head>
	
	<body>
	    <div class="navbar navbar-inverse navbar-fixed-top">
	      <div class="navbar-inner">
	        <div class="container-fluid">
	          <button type="button" class="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
	            <span class="icon-bar"></span>
	            <span class="icon-bar"></span>
	            <span class="icon-bar"></span>
	          </button>
	          <a class="brand" href="">Harness Sample To-Do List</a>
	          <div class="nav-collapse collapse">
	            <p class="navbar-text pull-right">
	              <a href="/login" class="navbar-link">Logout</a>
	            </p>
	            <ul class="nav">
	              <li><a href="/inside/display">My Harness Sample To-Do List</a></li>
	            </ul>
	          </div><!--/.nav-collapse -->
	        </div>
	      </div>
	    </div>
	    
	    
		<div class = "container">
		
			<div class = "row">
					<div class = "span8 offset2">
											
						<%String name = (String)session.getAttribute("name");
						  name = StringEscapeUtils.escapeHtml4(name);%>
						<%out.println("<div class=\"page-header text-center\"><h1>"
						+ name + "'s Harness Sample To-Do List</h1></div>");%>
				
					</div>
			</div>
			
			<div class = "row">
				<div class = "span10 offset1">
					<table class="table table-striped table-bordered table-hover" border="1">
						<thead>
							<tr>
								<th style='text-align:center;vertical-align:middle'>Task</th>
								<th style='text-align:center;vertical-align:middle'>Priority</th>
								<th style='text-align:center;vertical-align:middle'>Created Date</th>								
							</tr>
						</thead>
						<tbody>
						
						<%		String date = request.getParameter("date");

								try {
									Connection connection = ConnectionManager.getConnection();
									
									String queryString = "select thing, priority " +
											"from task where name = ? and createDate = ?";
									PreparedStatement statement = connection.prepareStatement(queryString);
									statement.setString(1,name);
									statement.setString(2,date);
									
									ResultSet rset = statement.executeQuery();
									
									while(rset.next()){
										String task = rset.getString(1);
										String priority = rset.getString(2);
										date = date.replace(' ','_');
										String escapedTask = StringEscapeUtils.escapeHtml4(task);
										String escapedPriority = StringEscapeUtils.escapeHtml4(priority);
										String escapedDate = StringEscapeUtils.escapeHtml4(date);
										out.println("<tr>");
										out.println("<td style='text-align:center;'>" + escapedTask +"</td>");
										out.println("<td style='text-align:center;'>" + escapedPriority +"</td>");
										out.println("<td style='text-align:center;'>" + escapedDate +"</td>");
										out.println("</tr>");
									}
									rset.close();
									statement.close();
								} catch (SQLException e) {
									e.printStackTrace(System.out);
								}
								
						%>
						</tbody>
					</table> 
				</div>
			</div>
			
						
			<div class = "row">
				<div class = "span10 offset1">
						<form class = "form-inline pull-center" style = "margin-left:15px;margin-right:15px;" action = "/inside/editTask">
						    <input type="text" class="input-xxlarge" placeholder="Task" name = "task">
						    <input type="text" class="input-small" placeholder="Priority(1-10)" name = "priority">
						    <%out.println("<input class='input-xlarge' name = 'date' type='text' value='" + StringEscapeUtils.escapeHtml4(date) + "' readonly='readonly'>"); %>
						    <br><br>
						    <button type="submit" class="btn-block btn-large btn-success">OK</button>
						    <a class = "btn btn-block btn-large" href = "/inside/display">Back</a>
						    </form>
				</div>
			</div>

		</div>
		
	</body>
	
</html>
