package inside;

import db.ConnectionManager;
import validation.RequestValidation;

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
    // Both untrusted values are validated as they are read: the task text must be present,
    // non-blank and fit the task.thing column, and the priority must be a small positive integer
    // that fits the task.priority column. Anything else is null here and the request fails closed
    // to the task list instead of being inserted or throwing.
    String task = RequestValidation.taskText(request.getParameter("task"));
    String pri = RequestValidation.priority(request.getParameter("priority"));
    int priority;

    if (task == null || pri == null)
      response.sendRedirect("/inside/display");
    else {
      // Safe: the priority was already restricted to 1-4 digits, so the parse cannot throw.
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
