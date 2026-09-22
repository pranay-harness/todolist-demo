<%@page import = "java.io.IOException" %>
<%@page import = "java.sql.Connection" %>
<%@page import = "java.sql.DriverManager" %>
<%@page import = "java.sql.PreparedStatement" %>
<%@page import = "java.sql.ResultSet" %>
<%@page import = "java.sql.SQLException" %>
<%@page import = "java.sql.Statement" %>
<%@page import = "db.ConnectionManager" %>

<%!
	// Helper method to escape HTML special characters
	private String escapeHtml(String text) {
		if (text == null) return "";
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}
%>



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
	          </div><!--/.nav-collapse -->
	        </div>
	      </div>
	    </div>
	    
	    
		<div class = "container">
		
			<div class = "row">
					<div class = "span8 offset2">
											
						<%String name = (String)session.getAttribute("name");%>
						<%out.println("<div class=\"page-header text-center\"><h1>"
						+ name + "'s To-Do List</h1></div>");%>
				
					</div>
			</div>
			
			<div class = "row">
				<div class = "span10 offset1">
						<form  style="float:left;" class = "form-inline pull-center" action = "/inside/addTask">
						    <input type="text" class="input-xlarge" placeholder="Add your task here" name = "task">
						    <input type="text" class="input-medium" placeholder="Priority(1-10)" name = "priority">
						    <button type="submit" class="btn-success">Add New Task</button>
					    </form>
						<form style="float:right;" class="form-search" action = "/inside/searchResult.jsp">
						    <input type="text" placeholder="Search by name" class="input-large search-query" name = "task">
						    <button type="submit" class="btn">Search</button>
					    </form>	
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
								<th style='text-align:center;vertical-align:middle'>Actions</th>							
							</tr>
						</thead>
						<tbody>
						
						<%
								int pageNumber = 0;
								boolean isSort = false;
								boolean hasNext = false;
								String pageParameter = request.getParameter("page");
								String sort = request.getParameter("sort");
								if(sort == null || sort.isEmpty()){
									isSort = false;
								} else {
									isSort = Boolean.parseBoolean(sort);
								}
								if(pageParameter == null || pageParameter.isEmpty()){
									pageNumber = 0;
								} else {
									pageNumber = Integer.parseInt(pageParameter);
								}

								try {
									Connection connection = ConnectionManager.getConnection();
									String queryString;
									if(!isSort){
										queryString = "select thing, priority, createDate from task where name = ?";
									} else {
										queryString = "select thing, priority, createDate from task where name = ? order by priority desc, createDate asc";
									}
									PreparedStatement statement = connection.prepareStatement(queryString);
									statement.setString(1,name);
									ResultSet rset = statement.executeQuery();
									int count = 0;
									while(rset.next()){
										if((count / 6) > pageNumber){
											hasNext = true;
											break;
										}
										if((count / 6) == pageNumber){
											String task = rset.getString(1);
											String priority = rset.getString(2);
											String date = rset.getString(3);
											date = date.replace(' ','_');
											out.println("<tr>");
											out.println("<td style='text-align:center;'>" + escapeHtml(task) +"</td>");
											out.println("<td style='text-align:center;'>" + escapeHtml(priority) +"</td>");
											out.println("<td style='text-align:center;'>" + escapeHtml(date) +"</td>");
											out.println("<td>");
											out.print("<a class = 'btn-small btn-info' style = 'float:left;' method = 'post'");
											out.print("href = '/inside/deleteTask?date=" + escapeHtml(date) + "'>");
											out.println("Done</a>");
											out.print("<a class = 'btn-small btn-primary' style = 'float:left;' method = 'post'");
											out.print("href = '/inside/showEditTask.jsp?date=" + escapeHtml(date) + "'>");
											out.println("Edit</a>");
											out.print("<a class = 'btn-small btn-danger' style = 'float:right;' method = 'post'");
											out.print("href = '/inside/deleteTask?date=" + escapeHtml(date) + "'>");
											out.println("Delete</a>");
											out.println("</td>");
											out.println("</tr>");
										}
										count++;
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
					<%	//System.out.println("hasNext?" + hasNext);
						if(hasNext){
							int nextPage = pageNumber + 1;
							out.print("<a class = 'btn-large btn-link btn' style = 'float:right;' method = 'post'");
							if(isSort){
								out.print("href = '/inside/display.jsp?sort=true&page=" + nextPage + "'>");
							} else {
								out.print("href = '/inside/display.jsp?page=" + nextPage + "'>");
							}
							out.println("NextPage</a>");
						}

						if(pageNumber > 0){
							int lastPage = pageNumber - 1;
							out.print("<a class = 'btn-large btn-link btn' style = 'float:left;' method = 'post'");
							if(isSort){
								out.print("href = '/inside/display.jsp?sort=true&page=" + lastPage + "'>");
							} else {
								out.print("href = '/inside/display.jsp?page=" + lastPage + "'>");
							}

							out.println("LastPage</a>");
						}
						
						%>
				</div>
			</div>
			
			<div class = "row">
				<div class = "span10 offset1">
						<a class = 'btn-warning btn btn-block' style = 'float:center;' href = '/inside/display.jsp?sort=true'>Sort By Priority</a>
				</div>
			</div>
			
		</div>
		
	</body>
	
</html>
