package inside;

import db.ConnectionManager;
import validation.RequestValidator;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class Edit
 */

public class Edit extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /** Fixed, server-side controlled redirect targets. Never built from request data. */
  private static final String EDIT_TASK_PAGE = "/inside/showEditTask.jsp";
  private static final String DISPLAY_PAGE = "/inside/display";

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String date = request.getParameter("date");
    //		System.out.println("the parameter is " + date);
    HttpSession session = request.getSession(false);
    String name = (String) session.getAttribute("name");
    //		System.out.println("the name is " + date);
    String priority = request.getParameter("priority");
    String task = request.getParameter("task");
    if (task == null || priority == null || task.isEmpty() || priority.isEmpty()) {
      //			System.out.println("nimei!");
      // The destination is a fixed in-application page; the request value is only ever carried
      // as an allowlisted, URL-encoded query parameter so it cannot alter scheme, host or path.
      String link = DISPLAY_PAGE;
      if (RequestValidator.isCreateDate(date)) {
        link = EDIT_TASK_PAGE + "?date=" + URLEncoder.encode(date, "UTF-8");
      }
      //			System.out.println("the link is " + link);
      response.sendRedirect(link);
    } else if (!RequestValidator.isTaskText(task)
        || !RequestValidator.isPriority(priority)
        || !RequestValidator.isCreateDate(date)) {
      // Fail closed: unvalidated request data never reaches the update logic. The allowlists live in
      // validation.RequestValidator - the single definition every servlet uses - so the task text,
      // priority and create date are validated here exactly as AddTask and Delete validate them,
      // including the stored width of the columns they are written to. Redirect back to the
      // in-application list page, the same way the rest of the app handles rejected input.
      response.sendRedirect(DISPLAY_PAGE);
    } else {
      //			System.out.println("gan!");

      try {
        Connection connection = ConnectionManager.getConnection();
        String queryString = "update task set thing = ?, priority = ? where createDate = ? and name = ?";
        PreparedStatement statement = connection.prepareStatement(queryString);
        statement.setString(1, task);
        statement.setString(2, priority);
        statement.setString(3, date);
        statement.setString(4, name);
        statement.executeUpdate();
        statement.close();
        response.sendRedirect("/inside/display");
      } catch (SQLException e) {
        e.printStackTrace(System.out);
      }
    }
  }

}
