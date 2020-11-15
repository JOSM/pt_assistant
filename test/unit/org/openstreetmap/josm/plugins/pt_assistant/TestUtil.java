// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 *
 * Utilities for tests.
 *
 * @author floscher
 */
public final class TestUtil {
  private static boolean isInitialized;

  private TestUtil() {
    // Prevent instantiation
  }

  /**
   * This method tests utility classes for common coding standards (exactly one constructor that's private,
   * only static methods, …) and fails the current test if one of those standards is not met.
   * This is inspired by http://stackoverflow.com/questions/4520216 .
   * @param c the class under test
   */
  public static void testUtilityClass(final Class<?> c) {
    try {
      // class must be final
      assertTrue("Class " + c.getName() + " should be final!", Modifier.isFinal(c.getModifiers()));
      // with exactly one constructor
      assertEquals("Class " + c.getName() + " should have exactly one constructor!", 1, c.getDeclaredConstructors().length);
      final Constructor<?> constructor = c.getDeclaredConstructors()[0];
      // constructor has to be private
      assertTrue("The constructor of " + c.getName() + " should be private!", Modifier.isPrivate(constructor.getModifiers()));
      constructor.setAccessible(true);
      // Call private constructor for code coverage
      constructor.newInstance();
      for (Method m : c.getMethods()) {
        // Check if all methods are static
        assertTrue("All methods of " + c.getName() + " should be static (" + m.getName() + " isn't)!", m.getDeclaringClass() != c || Modifier.isStatic(m.getModifiers()));
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail(e.getLocalizedMessage());
    }
  }
}
