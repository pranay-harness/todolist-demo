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
    if (date == null || !date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
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
