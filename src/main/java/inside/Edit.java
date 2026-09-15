package inside;

import db.ConnectionManager;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.regex.Pattern;
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
   * Task create dates are generated server-side from Date#toString() with blanks replaced by
   * underscores (e.g. "Mon_Sep_15_12:34:56_UTC_2026"), so only these characters are accepted.
   * Anything else (path separators, scheme/host characters, encoded escapes) is rejected.
   */
  private static final Pattern SAFE_DATE = Pattern.compile("[A-Za-z0-9 _:+-]{1,64}");

  /**
   * Priority is stored in an integer column, and the edit form offers 1-10 (see
   * inside/showEditTask.jsp). Accept the same positive-integer shape AddTask accepts, bounded so the
   * value always fits the column.
   */
  private static final Pattern SAFE_PRIORITY = Pattern.compile("[1-9][0-9]{0,8}");

  /**
   * Task text is free form but is stored in {@code task.thing varchar(60)}. Control characters
   * (CR/LF, NUL, ...) are rejected so request data can never forge log records or break the stored
   * value.
   */
  private static final Pattern SAFE_TASK = Pattern.compile("[^\\p{Cntrl}]{1,60}");

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
      if (date != null && SAFE_DATE.matcher(date).matches()) {
        link = EDIT_TASK_PAGE + "?date=" + URLEncoder.encode(date, "UTF-8");
      }
      //			System.out.println("the link is " + link);
      response.sendRedirect(link);
    } else if (!SAFE_TASK.matcher(task).matches()
        || !SAFE_PRIORITY.matcher(priority).matches()
        || date == null
        || !SAFE_DATE.matcher(date).matches()) {
      // Fail closed: unvalidated request data never reaches the update logic. Redirect back to the
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
