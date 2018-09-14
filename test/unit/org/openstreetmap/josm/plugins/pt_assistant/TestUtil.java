// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pt_assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.PlatformManager;

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
   * Initializes the main class of JOSM and the mapillary plugin with
   * the preferences from test/data/preferences.
   */
  public static synchronized void initPlugin() {
    if (!isInitialized) {
      System.setProperty("josm.home", "test/data/preferences");
      Preferences.main().enableSaveOnPut(false);
      I18n.init();
      PlatformManager.getPlatform().preStartupHook();
      Preferences.main().init(false);
      Config.setPreferencesInstance(Preferences.main());
      Config.setBaseDirectoriesProvider(JosmBaseDirectories.getInstance());
      I18n.set(Config.getPref().get("language", "en"));
      ProjectionRegistry.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator
      isInitialized = true;
    }
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
      assertTrue(Modifier.isFinal(c.getModifiers()));
      // with exactly one constructor
      assertEquals(1, c.getDeclaredConstructors().length);
      final Constructor<?> constructor = c.getDeclaredConstructors()[0];
      // constructor has to be private
      assertTrue(Modifier.isPrivate(constructor.getModifiers()));
      constructor.setAccessible(true);
      // Call private constructor for code coverage
      constructor.newInstance();
      for (Method m : c.getMethods()) {
        // Check if all methods are static
        assertTrue(m.getDeclaringClass() != c || Modifier.isStatic(m.getModifiers()));
      }
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail(e.getLocalizedMessage());
    }
  }
}
