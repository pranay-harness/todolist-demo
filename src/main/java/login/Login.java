package login;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import security.RedirectValidator;

/**
 * Servlet implementation class Login
 */

public class Login extends HttpServlet {
  private static final long serialVersionUID = 1L;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String redirectUrl = request.getContextPath() + "/login.jsp";
    if (RedirectValidator.isValidRedirectUrl(redirectUrl)) {
      response.sendRedirect(redirectUrl);
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect URL");
    }
  }
}
