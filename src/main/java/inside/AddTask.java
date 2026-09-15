package inside;

import db.ConnectionManager;
import validation.RequestValidator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class AddTask
 */
public class AddTask extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String task = request.getParameter("task");
    String pri = request.getParameter("priority");
    int priority;

    // Validate both parameters at the trust boundary: the task text has to fit the
    // task.thing varchar(60) column and carry no control characters, and the priority has to be a
    // bounded positive integer (a missing or oversized value used to slip through and fail later).
    // Invalid input fails closed to the task list, as before.
    if (!RequestValidator.isTaskText(task) || !RequestValidator.isPriority(pri))
      response.sendRedirect("/inside/display");
    else {
      priority = Integer.parseInt(pri);
      String name = (String) (request.getSession(false).getAttribute("name"));
      Date date = new Date();

      try {
        Connection connection = ConnectionManager.getConnection();
        String queryString = "insert into task values(?,?,?,?)";
        PreparedStatement statement = connection.prepareStatement(queryString);
        statement.setString(1, name);
        statement.setString(2, task);
        statement.setString(3, Integer.toString(priority));
        statement.setString(4, date.toString().replace(' ', '_'));
        statement.executeUpdate();
        statement.close();
      } catch (SQLException e) {
        e.printStackTrace(System.out);
      }
      response.sendRedirect("/inside/display");
    }
  }

}
