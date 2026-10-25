package login;

import db.ConnectionManager;
import util.Passwords;
import util.Usernames;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    // The submitted name is untrusted input and must be validated before it is used to
    // authenticate: an unchecked parameter placed in the session would mix untrusted data
    // into a trusted structure (CWE-501). Fails closed - an unexpected name never logs in.
    if (password == null || password.isEmpty() || !Usernames.isValid(name)) {
      response.sendRedirect(request.getContextPath() + "/loginFault.jsp");
    } else {
      try {

        Connection connection = ConnectionManager.getConnection();

        //				System.out.println("connection done");

        Statement statement = connection.createStatement();

        //				System.out.println("WTF?");

        ResultSet resultSet = statement.executeQuery("select name, password from accounts");

        //				System.out.println("nima");

        String authenticatedName = null;

        while (resultSet.next()) {
          String accountName = resultSet.getString(1);
          if (accountName.equals(name)) {
            // Constant-time comparison: String.equals leaks how many leading characters
            // matched through its running time (CWE-208).
            if (Passwords.matches(password, resultSet.getString(2))) {
              // Keep the name as held by the account record: that is the value the
              // application itself resolved and authenticated, not the raw parameter.
              authenticatedName = accountName;
              success = true;
              break;
            }
          }
        }
        resultSet.close();
        statement.close();
        if (success && Usernames.isValid(authenticatedName)) {
          // Only the validated, application-resolved account name crosses into the session.
          request.getSession().setAttribute("name", authenticatedName);
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
