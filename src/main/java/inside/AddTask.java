package inside;

import db.ConnectionManager;
import validation.RequestParameters;

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

    // Validate both untrusted parameters at the trust boundary before they reach the database;
    // invalid input returns to the task list exactly as an empty task or a non-numeric priority
    // already did.
    if (!RequestParameters.isValidTask(task) || !RequestParameters.isValidPriority(pri))
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
