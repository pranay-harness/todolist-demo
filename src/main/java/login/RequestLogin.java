package login;

import db.ConnectionManager;
import db.StoredPassword;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class RequestLogin
 */
@WebServlet("/RequestLogin")
public class RequestLogin extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /**
   * The shape an account name is allowed to have. Accounts are held in a {@code varchar(32)}
   * column, so anything longer, empty or containing characters outside this set cannot be a name
   * this application issued and is never allowed to become session state.
   */
  private static final Pattern ACCOUNT_NAME = Pattern.compile("[A-Za-z0-9._@ -]{1,32}");

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String name = request.getParameter("name");
    String password = request.getParameter("password");
    String remember = request.getParameter("remember");
    // Holds the identity read back from the account record that authentication succeeded against.
    // It stays null until a password actually matches, so no unauthenticated path can reach the
    // session below.
    String authenticatedName = null;
    if (password == null || password.isEmpty() || name == null || name.isEmpty()) {
      response.sendRedirect(request.getContextPath() + "/loginFault.jsp");
    } else {
      try {

        Connection connection = ConnectionManager.getConnection();

        //				System.out.println("connection done");

        // The account name is bound as a parameter so it can never become part of the SQL text.
        // The stored name is selected as well so that the identity kept in the session is the one
        // recorded on the account, not the string the client sent.
        String queryString = "select name, password from accounts where name = ?";
        PreparedStatement statement = connection.prepareStatement(queryString);
        statement.setString(1, name);

        //				System.out.println("WTF?");

        ResultSet resultSet = statement.executeQuery();

        //				System.out.println("nima");

        while (resultSet.next()) {
          if (passwordMatches(resultSet.getString(2), password)) {
            authenticatedName = trustedAccountName(resultSet.getString(1));
            break;
          }
        }
        resultSet.close();
        statement.close();
        if (authenticatedName != null) {
          // Only the validated identity from the authenticated account record crosses into session
          // state; the raw request parameter never does. A name that does not have the expected
          // shape leaves authenticatedName null above and the request fails closed below.
          // The pre-login session is discarded first so a session identifier chosen by an attacker
          // cannot become an authenticated one (session fixation).
          HttpSession previousSession = request.getSession(false);
          if (previousSession != null) {
            previousSession.invalidate();
          }
          HttpSession session = request.getSession(true);
          session.setAttribute("name", authenticatedName);
          if (remember == null) {
            session.setMaxInactiveInterval(1200);
          } else {
            session.setMaxInactiveInterval(86400 * 7);
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

  /**
   * Returns the account name when it has the shape an account name is stored with, otherwise null.
   * Everything downstream (the task queries and the pages that render the logged in user) reads this
   * value out of the session and treats it as trusted, so only a value that has been confirmed
   * against an account record and checked here is allowed to be stored.
   */
  private static String trustedAccountName(String accountName) {
    if (accountName == null || !ACCOUNT_NAME.matcher(accountName).matches()) {
      return null;
    }
    return accountName;
  }
}
