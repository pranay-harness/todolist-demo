package inside;

import db.ConnectionManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

  /** Only in-application destination this servlet may redirect back to for editing. */
  private static final String EDIT_TASK_PATH = "/inside/showEditTask.jsp";

  /** Safe in-application fallback used whenever the requested target is not allowed. */
  private static final String DEFAULT_REDIRECT_PATH = "/inside/display";

  /**
   * Shape of a task createDate: AddTask stores Date.toString() with spaces replaced by '_'
   * (e.g. Mon_Sep_15_12:00:00_UTC_2026). Anything else is rejected.
   */
  private static final Pattern SAFE_DATE = Pattern.compile("[A-Za-z0-9_:.+-]{1,64}");

  /**
   * Shape of a task priority: AddTask only ever stores a positive integer without leading zeros
   * (see the "Priority(1-10)" inputs in the JSPs), so the same shape is accepted here. The length
   * bound keeps the value inside the range of the integer priority column.
   */
  private static final Pattern SAFE_PRIORITY = Pattern.compile("[1-9][0-9]{0,8}");

  /**
   * Shape of a task description: the thing column is varchar(60), and control characters
   * (CR/LF/NUL) are rejected so an untrusted value can never forge extra log records.
   */
  private static final Pattern SAFE_TASK = Pattern.compile("[^\\p{Cntrl}]{1,60}");

  /**
   * Validates an untrusted request parameter at the trust boundary: a missing value, or a value
   * that does not match the expected shape, is rejected so it never reaches the database, the
   * redirect target or the application log.
   */
  private static boolean isValidParameter(String value, Pattern shape) {
    return value != null && shape.matcher(value).matches();
  }

  /**
   * Builds the redirect target for the edit form. The path is always a compile-time constant
   * in-application path, and the untrusted date value is only appended when it matches the
   * expected createDate shape and after URL encoding. Absolute URLs, protocol-relative
   * targets, backslash tricks and encoded variants can therefore never reach sendRedirect.
   */
  private static String buildEditTaskRedirect(String dateParam) {
    if (dateParam == null || !SAFE_DATE.matcher(dateParam).matches()) {
      return DEFAULT_REDIRECT_PATH;
    }
    try {
      return EDIT_TASK_PATH + "?date=" + URLEncoder.encode(dateParam, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return DEFAULT_REDIRECT_PATH;
    }
  }

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
    // Validate every untrusted parameter before it is used; invalid input falls back to the edit
    // form exactly as a missing task or priority already did.
    if (!isValidParameter(task, SAFE_TASK)
        || !isValidParameter(priority, SAFE_PRIORITY)
        || !isValidParameter(date, SAFE_DATE)) {
      //			System.out.println("nimei!");
      String link = buildEditTaskRedirect(date);
      //			System.out.println("the link is " + link);
      response.sendRedirect(link);
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
        System.out.println("task is " + task);
        System.out.println("p is " + priority);
        System.out.println("time is " + date);
        System.out.println("name is " + name);
        statement.executeUpdate();
        statement.close();
        response.sendRedirect("/inside/display");
      } catch (SQLException e) {
        e.printStackTrace(System.out);
      }
    }
  }

}
