package util;

import javax.servlet.http.HttpServletRequest;
import junit.framework.TestCase;

/**
 * Regression tests for the redirect allowlist (CWE-601).
 */
public class SafeRedirectTest extends TestCase {

  /** No container in a unit test: a null request simply yields an empty context path. */
  private static final HttpServletRequest NO_REQUEST = null;

  public void testAllowlistedTargetsAreKept() {
    assertEquals(SafeRedirect.LOGIN, SafeRedirect.resolve(SafeRedirect.LOGIN));
    assertEquals(SafeRedirect.LOGIN_FAULT, SafeRedirect.resolve(SafeRedirect.LOGIN_FAULT));
    assertEquals(SafeRedirect.WRONG_REGISTER, SafeRedirect.resolve(SafeRedirect.WRONG_REGISTER));
    assertEquals(SafeRedirect.USER_EXISTS, SafeRedirect.resolve(SafeRedirect.USER_EXISTS));
    assertEquals(SafeRedirect.INSIDE_DISPLAY, SafeRedirect.resolve(SafeRedirect.INSIDE_DISPLAY));
    assertEquals(SafeRedirect.INSIDE_EDIT_TASK, SafeRedirect.resolve(SafeRedirect.INSIDE_EDIT_TASK));
    assertTrue(SafeRedirect.isAllowed(SafeRedirect.LOGIN));
  }

  public void testExternalTargetsAreRejected() {
    String[] hostile = {
        "http://evil.example/",
        "https://evil.example/login.jsp",
        "//evil.example",
        "/\\evil.example",
        "\\\\evil.example",
        "/inside/../../etc/passwd",
        "/login.jsp/../../evil",
        "javascript:alert(1)",
        "/login.jsp\r\nSet-Cookie: a=b",
        "",
        null,
    };
    for (int i = 0; i < hostile.length; i++) {
      assertFalse("must not be allowlisted: " + hostile[i], SafeRedirect.isAllowed(hostile[i]));
      assertEquals("must fall back to the login page: " + hostile[i], SafeRedirect.LOGIN, SafeRedirect.resolve(hostile[i]));
      assertEquals(SafeRedirect.LOGIN, SafeRedirect.location(NO_REQUEST, hostile[i]));
    }
  }

  public void testLocationIsApplicationRelative() {
    assertEquals(SafeRedirect.INSIDE_DISPLAY, SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_DISPLAY));
  }

  public void testQueryValueIsEncodedAndCannotChangeDestination() {
    assertEquals("/inside/showEditTask.jsp?date=2026-01-01+10%3A00%3A00",
        SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_EDIT_TASK, "date", "2026-01-01 10:00:00"));
    assertEquals("/inside/showEditTask.jsp?date=%2F%2Fevil.example",
        SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_EDIT_TASK, "date", "//evil.example"));
    assertEquals("/inside/showEditTask.jsp?date=%0D%0ASet-Cookie%3A+a%3Db",
        SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_EDIT_TASK, "date", "\r\nSet-Cookie: a=b"));
  }

  public void testMissingOrHostileParameterIsDropped() {
    assertEquals(SafeRedirect.INSIDE_EDIT_TASK,
        SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_EDIT_TASK, "date", null));
    assertEquals(SafeRedirect.INSIDE_EDIT_TASK,
        SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_EDIT_TASK, "da te", "2026-01-01"));
    assertEquals(SafeRedirect.INSIDE_EDIT_TASK,
        SafeRedirect.location(NO_REQUEST, SafeRedirect.INSIDE_EDIT_TASK, null, "2026-01-01"));
  }
}
