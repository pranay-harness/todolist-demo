package inside;

import db.ConnectionManager;

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
    String name = (String) request.getSession(false).getAttribute("name");
    try {
      Connection connection = ConnectionManager.getConnection();


      String queryString = "delete from task where createDate = ? and name = ?";
      PreparedStatement statement = connection.prepareStatement(queryString);
      statement.setString(1, date);
      statement.setString(2, name);
      statement.executeUpdate();
      statement.close();
    } catch (SQLException e) {
      e.printStackTrace(System.out);
    }
    response.sendRedirect("/inside/display");
  }
}
