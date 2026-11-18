package db;

import java.sql.Connection;

import junit.framework.TestCase;

/**
 * Unit tests for {@link ConnectionManager}, which now reads the database url, user and
 * password from the environment instead of from hardcoded literals.
 *
 * <p>Written in the JUnit 3 style ({@code junit.framework.TestCase}) so it compiles and
 * runs with either JUnit version declared in the build.</p>
 */
public class ConnectionManagerTest extends TestCase {

  /**
   * With no configuration supplied the app must still start against the embedded
   * in-memory H2 database, exactly as it did before the credentials were externalised.
   */
  public void testConnectsToTheEmbeddedDefaultWithoutConfiguration() throws Exception {
    Connection connection = ConnectionManager.getConnection();
    assertNotNull(connection);
    assertFalse(connection.isClosed());
  }

  public void testGetConnectionReturnsTheSameSharedConnection() {
    assertSame(ConnectionManager.getConnection(), ConnectionManager.getConnection());
  }
}
