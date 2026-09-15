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

  /**
   * Fixed in-application page used when the requested task date is missing or untrusted.
   */
  private static final String DEFAULT_PAGE = "/inside/display";

  /**
   * Allowlisted shape of the createDate token stored by AddTask (e.g. Mon_Sep_15_18:19:00_UTC_2026).
   * Rejects any scheme, "//" prefix, backslash, CR/LF or host, so the destination stays in-app.
   */
  private static final Pattern SAFE_DATE = Pattern.compile("[A-Za-z0-9:_.+-]{1,64}");

  /**
   * Allowlisted priority: digits only, 1-9999, so the value is always a well-formed
   * number that fits the task.priority integer column (see Register#init).
   */
  private static final Pattern SAFE_PRIORITY = Pattern.compile("[1-9][0-9]{0,3}");

  /** Width of the task.thing column declared in Register#init (varchar(60)). */
  private static final int MAX_TASK_LENGTH = 60;

  /** Width of the task.name column declared in Register#init (varchar(32)). */
  private static final int MAX_NAME_LENGTH = 32;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String date = request.getParameter("date");
    //		System.out.println("the parameter is " + date);
    HttpSession session = request.getSession(false);
    String name = (session == null) ? null : (String) session.getAttribute("name");
    //		System.out.println("the name is " + date);
    String priority = request.getParameter("priority");
    String task = request.getParameter("task");
    if (task == null || priority == null || task.isEmpty() || priority.isEmpty()) {
      //			System.out.println("nimei!");
      // The redirect target is a fixed relative in-application path; the request
      // parameter is only used as an allowlisted, URL-encoded query value, so it
      // cannot point the redirect at an external host. Unknown input fails closed
      // to the task list page.
      String link = DEFAULT_PAGE;
      if (date != null && SAFE_DATE.matcher(date).matches()) {
        link = "/inside/showEditTask.jsp?date=" + URLEncoder.encode(date, "UTF-8");
      }
      //			System.out.println("the link is " + link);
      response.sendRedirect(link);
    } else if (!isValidUpdate(date, name, task, priority)) {
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
        // Safe: SAFE_PRIORITY already restricted the value to 1-4 digits, so the parse
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

  /**
   * Validates the untrusted edit inputs at the trust boundary before they are used.
   * The priority must be a small positive integer, the task text and owner name must be
   * non-blank and within the column widths declared in Register#init, and the task key
   * must match the allowlisted createDate shape.
   *
   * @return true only when every value is well-formed and within range
   */
  private static boolean isValidUpdate(String date, String name, String task, String priority) {
    if (name == null || name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
      return false;
    }
    if (task.trim().isEmpty() || task.length() > MAX_TASK_LENGTH) {
      return false;
    }
    if (!SAFE_PRIORITY.matcher(priority).matches()) {
      return false;
    }
    return date != null && SAFE_DATE.matcher(date).matches();
  }

}
