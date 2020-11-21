package org.openstreetmap.josm.plugins.pt_assistant.utils;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.plugins.pt_assistant.TestUtil;
import org.openstreetmap.josm.testutils.JOSMTestRules;

public class UtilityClassesTest {

    @Rule
    public JOSMTestRules rules = new JOSMTestRules();

    @Test
    public void testAllUtilityClasses() {
        TestUtil.testUtilityClass(BoundsUtils.class);
        TestUtil.testUtilityClass(ColorPalette.class);
        TestUtil.testUtilityClass(DialogUtils.class);
        TestUtil.testUtilityClass(GeometryUtils.class);
        TestUtil.testUtilityClass(GuiUtils.class);
        TestUtil.testUtilityClass(NodeUtils.class);
        TestUtil.testUtilityClass(NotificationUtils.class);
        TestUtil.testUtilityClass(PrimitiveUtils.class);
        TestUtil.testUtilityClass(PTIcons.class);
        TestUtil.testUtilityClass(PTProperties.class);
        TestUtil.testUtilityClass(RouteUtils.class);
        // StopToWayAssigner does not match the criteria
        TestUtil.testUtilityClass(StopUtils.class);
        TestUtil.testUtilityClass(WayUtils.class);
    }
}
