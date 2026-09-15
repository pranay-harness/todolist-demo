package util;

import junit.framework.TestCase;

/**
 * Unit tests for {@link Encode}, the output encoder used by the JSP views.
 *
 * <p>Written in the JUnit 3 style ({@code junit.framework.TestCase}) so it compiles and
 * runs with either JUnit version declared in the build.</p>
 */
public class EncodeTest extends TestCase {

  public void testHtmlLeavesPlainTextUnchanged() {
    assertEquals("Buy milk", Encode.html("Buy milk"));
    assertEquals("2026-03-27_10:15:00", Encode.html("2026-03-27_10:15:00"));
  }

  public void testHtmlEncodesElementContentMetacharacters() {
    assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;",
        Encode.html("<script>alert(1)</script>"));
    assertEquals("a &amp; b", Encode.html("a & b"));
  }

  public void testHtmlEncodesAttributeQuotes() {
    assertEquals("&#39; onmouseover=&#39;alert(1)", Encode.html("' onmouseover='alert(1)"));
    assertEquals("&quot; onmouseover=&quot;alert(1)", Encode.html("\" onmouseover=\"alert(1)"));
  }

  public void testHtmlHandlesNull() {
    assertEquals("", Encode.html(null));
  }

  public void testUrlParamEncodesForQueryString() {
    assertEquals("2026-03-27_10%3A15%3A00", Encode.urlParam("2026-03-27_10:15:00"));
  }

  public void testUrlParamCannotBreakOutOfAttributeOrTag() {
    String encoded = Encode.urlParam("' onmouseover='alert(1)");
    assertEquals(-1, encoded.indexOf('\''));
    assertEquals(-1, encoded.indexOf('<'));
    assertEquals(-1, encoded.indexOf('>'));
    assertEquals(-1, encoded.indexOf('"'));
  }

  public void testUrlParamHandlesNull() {
    assertEquals("", Encode.urlParam(null));
  }
}
