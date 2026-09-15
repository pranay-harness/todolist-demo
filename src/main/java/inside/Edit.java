package inside;

import db.ConnectionManager;
import validation.RequestValidation;

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

  /**
   * Fixed in-application page used when the requested task date is missing or untrusted.
   */
  private static final String DEFAULT_PAGE = "/inside/display";

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // Each untrusted value is validated as it is read, at the point it enters the servlet:
    // RequestValidation returns null for anything missing, over-long, out of range or outside the
    // allowlisted format, so only well-formed values can reach the redirect or the update below.
    // The task key must match the createDate token shape, the priority must be a small positive
    // integer, the task text must be non-blank and fit its column, and the owner name taken from
    // the session must still have the shape an account name is stored with.
    String date = RequestValidation.createDate(request.getParameter("date"));
    //		System.out.println("the parameter is " + date);
    HttpSession session = request.getSession(false);
    String name = RequestValidation.accountName((session == null) ? null : (String) session.getAttribute("name"));
    //		System.out.println("the name is " + date);
    String priority = RequestValidation.priority(request.getParameter("priority"));
    String task = RequestValidation.taskText(request.getParameter("task"));
    if (task == null || priority == null) {
      //			System.out.println("nimei!");
      // No usable task text or priority was submitted yet, so show the edit page for the requested
      // task. The redirect target is a fixed relative in-application path; the validated task key
      // is only used as a URL-encoded query value, so it cannot point the redirect at an external
      // host. An unusable key fails closed to the task list page.
      String link = DEFAULT_PAGE;
      if (date != null) {
        link = "/inside/showEditTask.jsp?date=" + URLEncoder.encode(date, "UTF-8");
      }
      //			System.out.println("the link is " + link);
      response.sendRedirect(link);
    } else if (date == null || name == null) {
      // Input that fails the type/range/length/format checks never reaches the update:
      // fail closed to the task list instead of throwing or storing an over-long value.
      response.sendRedirect(DEFAULT_PAGE);
    } else {
      //			System.out.println("gan!");

      try {
        Connection connection = ConnectionManager.getConnection();
        String queryString = "update task set thing = ?, priority = ? where createDate = ? and name = ?";
        PreparedStatement statement = connection.prepareStatement(queryString);
        statement.setString(1, task);
        // Safe: the priority was already restricted to 1-4 digits when it was read, so the parse
        // cannot throw and the value fits the integer column.
        statement.setInt(2, Integer.parseInt(priority));
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
