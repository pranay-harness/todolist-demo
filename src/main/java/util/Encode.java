package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Contextual output encoding helpers shared by the JSP views.
 *
 * <p>Values that originate from an HTTP parameter or from the database must never be
 * written into a page as-is: they can close the surrounding tag or attribute and inject
 * script, which is a Cross-site Scripting flaw (CWE-79). Every such value has to be
 * encoded for the context it is rendered into.</p>
 *
 * <p>Implemented without extra dependencies so the plain servlet/JSP webapp keeps its
 * current dependency set.</p>
 */
public final class Encode {

  private Encode() {
    // utility class
  }

  /**
   * Encodes a value for HTML element content or for a quoted HTML attribute value.
   *
   * @param value untrusted value, may be null
   * @return the value with HTML metacharacters replaced by entities, never null
   */
  public static String html(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder encoded = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char character = value.charAt(i);
      switch (character) {
        case '&':
          encoded.append("&amp;");
          break;
        case '<':
          encoded.append("&lt;");
          break;
        case '>':
          encoded.append("&gt;");
          break;
        case '"':
          encoded.append("&quot;");
          break;
        case '\'':
          encoded.append("&#39;");
          break;
        default:
          encoded.append(character);
          break;
      }
    }
    return encoded.toString();
  }

  /**
   * Encodes a value for use as a URL query-string parameter value inside an HTML
   * attribute. The value is percent-encoded first, then HTML-encoded for the attribute
   * it is embedded in. The receiving servlet gets the original value back from
   * {@code request.getParameter(...)}.
   *
   * @param value untrusted value, may be null
   * @return the percent-encoded and HTML-encoded value, never null
   */
  public static String urlParam(String value) {
    if (value == null) {
      return "";
    }
    try {
      return html(URLEncoder.encode(value, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      // UTF-8 is required to be supported by every JVM.
      throw new IllegalStateException("UTF-8 is not available", e);
    }
  }
}
