package inside;

import db.ConnectionManager;
import validation.RequestParameters;

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
    String date = request.getParameter("date");
    //		System.out.println("the parameter is " + date);
    // Validate the untrusted createDate at the trust boundary; a value that does not have the
    // expected shape deletes nothing and simply returns to the task list.
    if (RequestParameters.isValidDate(date)) {
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
    }
    response.sendRedirect("/inside/display");
  }
}
