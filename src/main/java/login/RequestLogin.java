package login;

import db.ConnectionManager;
import db.StoredPassword;

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

        // The account name is bound as a parameter so it can never become part of the SQL text.
        String queryString = "select password from accounts where name = ?";
        PreparedStatement statement = connection.prepareStatement(queryString);
        statement.setString(1, name);

        //				System.out.println("WTF?");

        ResultSet resultSet = statement.executeQuery();

        //				System.out.println("nima");

        while (resultSet.next()) {
          if (passwordMatches(resultSet.getString(1), password)) {
            success = true;
            break;
          }
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

  /**
   * Verifies the supplied password against the stored one without leaking timing information. The
   * stored value is a salted one-way hash, so the supplied password is re-hashed with the recorded
   * parameters and the resulting hashes are compared in constant time.
   */
  private static boolean passwordMatches(String storedPassword, String suppliedPassword) {
    return StoredPassword.matches(storedPassword, suppliedPassword);
  }
}
