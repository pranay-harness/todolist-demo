package login;

import db.ConnectionManager;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class RequestLogin
 */
@WebServlet("/RequestLogin")
public class RequestLogin extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String name = request.getParameter("name");
    String password = request.getParameter("password");
    String remember = request.getParameter("remember");
    boolean success = false;
    if (password == null || password.isEmpty() || name == null || name.isEmpty()) {
      response.sendRedirect(request.getContextPath() + "/loginFault.jsp");
    } else {
      try {

        Connection connection = ConnectionManager.getConnection();

        //				System.out.println("connection done");

        PreparedStatement statement = connection.prepareStatement(
            "select name, password from accounts where name = ? and password = ?");
        statement.setString(1, name);
        statement.setString(2, password);

        //				System.out.println("WTF?");

        ResultSet resultSet = statement.executeQuery();

        //				System.out.println("nima");

        if (resultSet.next()) {
          success = true;
        }
        resultSet.close();
        statement.close();
        if (success) {
          request.getSession().setAttribute("name", name);
          if (remember == null) {
            request.getSession().setMaxInactiveInterval(1200);
          } else {
            request.getSession().setMaxInactiveInterval(86400 * 7);
          }
          response.sendRedirect(request.getContextPath() + "/inside/display");
        } else {
          response.sendRedirect(request.getContextPath() + "/loginFault.jsp");
        }
      } catch (SQLException e) {
        e.printStackTrace(System.out);
      } catch (Exception e) {
        e.printStackTrace(System.out);
      }
    }
  }
}
