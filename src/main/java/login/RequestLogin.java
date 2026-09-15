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
import javax.servlet.http.HttpSession;

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
    // Canonical user name as stored in the accounts table; only server-derived data is put in the
    // session, never the raw request parameter.
    String accountName = null;
    if (password == null || password.isEmpty() || name == null || name.isEmpty()) {
      response.sendRedirect(request.getContextPath() + "/loginFault.jsp");
    } else {
      try {

        Connection connection = ConnectionManager.getConnection();

        // The account is looked up with a bound parameter so the submitted name is never part of
        // the SQL text, and only the matching row is fetched instead of scanning every account.
        try (PreparedStatement statement = connection.prepareStatement("select name, password from accounts where name = ?")) {
          statement.setString(1, name);

          try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
              if (resultSet.getString(1).equals(name)) {
                if (resultSet.getString(2).equals(password)) {
                  accountName = resultSet.getString(1);
                  success = true;
                  break;
                }
              }
            }
          }
        }
        if (success && accountName != null) {
          // Session fixation: drop the pre-login session (it only ever holds a null "name"
          // placeholder set by login.jsp/loginFirst.jsp) and start a fresh one for the
          // authenticated user.
          HttpSession oldSession = request.getSession(false);
          if (oldSession != null) {
            oldSession.invalidate();
          }
          HttpSession session = request.getSession(true);
          // Store the account name loaded from the authenticated database row, not the raw
          // request parameter, so the session only carries trusted server-side state.
          session.setAttribute("name", accountName);
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
}
