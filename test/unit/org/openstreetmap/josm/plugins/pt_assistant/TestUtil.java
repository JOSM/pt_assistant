// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
    // JOSM core uses net.trajano.commons.testing.UtilityClassTestUtil, but that is not available from the test jar (gradle)
    try {
      // class must be final
      assertTrue(Modifier.isFinal(c.getModifiers()), "Class " + c.getName() + " should be final!");
      // with exactly one constructor
      assertEquals(1, c.getDeclaredConstructors().length, "Class " + c.getName() + " should have exactly one constructor!");
      final Constructor<?> constructor = c.getDeclaredConstructors()[0];
      // constructor has to be private
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "The constructor of " + c.getName() + " should be private!");
      constructor.setAccessible(true);
      // Call private constructor for code coverage
      constructor.newInstance();
      for (Method m : c.getMethods()) {
        // Check if all methods are static
        assertTrue(m.getDeclaringClass() != c || Modifier.isStatic(m.getModifiers()), "All methods of " + c.getName() + " should be static (" + m.getName() + " isn't)!");
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail(e.getLocalizedMessage());
    }
  }

  public static <T, O> O invokeHiddenMethod(
      final T object,
      final Class<T> clazz,
      final String methodName,
      final Class<O> returnType,
      final Class<?>[] parameterTypes,
      final Object... parameters
  ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
      final Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
      method.setAccessible(true);
      return returnType.cast(method.invoke(object, parameters));
  }
}
