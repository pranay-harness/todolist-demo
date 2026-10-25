package login;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.SafeRedirect;

/**
 * Servlet implementation class Login
 */

public class Login extends HttpServlet {
  private static final long serialVersionUID = 1L;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    SafeRedirect.redirect(response, request.getContextPath() + "/login.jsp");
  }
}
