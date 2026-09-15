package inside;

import db.ConnectionManager;
import validation.RequestValidation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class Delete
 */

public class Delete extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // The task key is validated as it is read: a value that is missing or does not have the
    // allowlisted createDate token shape is null here and never reaches the statement, so the
    // request fails closed to the task list instead of running with unchecked input.
    String date = RequestValidation.createDate(request.getParameter("date"));
    //		System.out.println("the parameter is " + date);
    if (date == null) {
      response.sendRedirect("/inside/display");
      return;
    }
    try {
      Connection connection = ConnectionManager.getConnection();


      String queryString = "delete from task where createDate = ?";
      PreparedStatement statement = connection.prepareStatement(queryString);
      statement.setString(1, date);
      statement.executeUpdate();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace(System.out);
    }
    response.sendRedirect("/inside/display");
  }
}
