/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.tools.eclipse.test.it;

import static io.openliberty.tools.eclipse.test.it.utils.LibertyPluginTestUtils.validateApplicationOutcomeCustom;
import static io.openliberty.tools.eclipse.test.it.utils.LibertyPluginTestUtils.validateApplicationStopped;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.eclipse.test.it.utils.SWTBotPluginOperations;
import io.openliberty.tools.eclipse.ui.dashboard.DashboardView;
import io.openliberty.tools.eclipse.ui.launch.LaunchConfigurationDelegateLauncher;

/**
 * Tests Open Liberty Eclipse plugin functions.
 */
public class LibertyPluginSWTBotMultiModMavenTest extends AbstractLibertyPluginSWTBotTest {

    /**
     * Application name.
     */
    static final String MVN_APP_NAME = "guide-maven-multimodules-pom";

    /**
     * Path to import from, in this case the multi-module root
     */
    static final Path project1Path = Paths.get("resources", "applications", "maven", "maven-multi-module", "typeJ");
    static final Path serverModule1Path = project1Path.resolve("pom");

    /**
     * Expected menu items.
     */
    static String[] mvnMenuItems = new String[] { DashboardView.APP_MENU_ACTION_START, DashboardView.APP_MENU_ACTION_START_CONFIG,
            DashboardView.APP_MENU_ACTION_START_IN_CONTAINER, DashboardView.APP_MENU_ACTION_STOP, DashboardView.APP_MENU_ACTION_RUN_TESTS,
            DashboardView.APP_MENU_ACTION_VIEW_MVN_IT_REPORT, DashboardView.APP_MENU_ACTION_VIEW_MVN_UT_REPORT };

    /**
     * Run As configuration menu items.
     */
    static String[] runAsShortcuts = new String[] { LaunchConfigurationDelegateLauncher.LAUNCH_SHORTCUT_START,
            LaunchConfigurationDelegateLauncher.LAUNCH_SHORTCUT_START_CONTAINER, LaunchConfigurationDelegateLauncher.LAUNCH_SHORTCUT_STOP,
            LaunchConfigurationDelegateLauncher.LAUNCH_SHORTCUT_RUN_TESTS,
            LaunchConfigurationDelegateLauncher.LAUNCH_SHORTCUT_MVN_VIEW_IT_REPORT,
            LaunchConfigurationDelegateLauncher.LAUNCH_SHORTCUT_MVN_VIEW_UT_REPORT, };

    static File workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();

    /**
     * Setup.
     */
    @BeforeAll
    public static void setup() {

        commonSetup();

        List<String> projectPaths = new ArrayList<String>();
        projectPaths.add(project1Path.resolve("pom").toString());
        projectPaths.add(project1Path.resolve("jar").toString());
        projectPaths.add(project1Path.resolve("war1").toString());
        projectPaths.add(project1Path.resolve("war2").toString());

        // Could be an interesting variation to stop with the above, which would cause the execution to run
        // in a single module context, since the aggregator POM wouldn't be present. The rest of the test class
        // isn't factored now to complete this idea, so leaving this as an idea if we want to expand in this direction later.
        projectPaths.add(project1Path.toString());

        importMavenProjects(workspaceRoot, projectPaths);

        // Check basic plugin artifacts are functioning before running tests.
        validateBeforeTestRun();
    }

    /**
     * Makes sure that some basics actions can be performed before running the tests:
     * 
     * <pre>
     * 1. The dashboard can be opened and its content retrieved. 
     * 2. The dashboard contains the expected applications. 
     * 3. The dashboard menu associated with a selected application contains the required actions. 
     * 4. The Run As menu for the respective application contains the required shortcut actions. 
     * 5. The Run As configuration view contains the Liberty entry for creating a configuration.
     * 6. The Debug As configuration view contains the Liberty entry for creating a configuration.
     * </pre>
     */
    public static final void validateBeforeTestRun() {
        dashboard = SWTBotPluginOperations.openDashboardUsingToolbar(bot);

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check that the dashboard can be opened and its content retrieved.
        List<String> projectList = SWTBotPluginOperations.getDashboardContent(bot, dashboard);

        // Check that dashboard contains the expected applications.
        boolean foundApp = false;
        for (String project : projectList) {
            if (MVN_APP_NAME.equals(project)) {
                foundApp = true;
                break;
            }
        }
        Assertions.assertTrue(foundApp, () -> "The dashboard does not contain expected application: " + MVN_APP_NAME);

        // Check that the menu that the application in the dashboard contains the required actions.
        List<String> menuItems = SWTBotPluginOperations.getDashboardItemMenuActions(bot, dashboard, MVN_APP_NAME);
        Assertions.assertTrue(menuItems.size() == mvnMenuItems.length,
                () -> "Maven application " + MVN_APP_NAME + " does not contain the expected number of menu items: " + mvnMenuItems.length);
        Assertions.assertTrue(menuItems.containsAll(Arrays.asList(mvnMenuItems)),
                () -> "Maven application " + MVN_APP_NAME + " does not contain the expected menu items: " + mvnMenuItems);

        // Check that the Run As menu contains the expected shortcut
        SWTBotMenu runAsMenu = SWTBotPluginOperations.getAppRunAsMenu(bot, MVN_APP_NAME);
        Assertions.assertTrue(runAsMenu != null, "The runAs menu associated with project: " + MVN_APP_NAME + " is null.");
        List<String> runAsMenuItems = runAsMenu.menuItems();
        Assertions.assertTrue(runAsMenuItems != null && !runAsMenuItems.isEmpty(),
                "The runAs menu associated with project: " + MVN_APP_NAME + " is null or empty.");
        int foundItems = 0;

        for (String expectedItem : runAsShortcuts) {
            for (String item : runAsMenuItems) {
                if (item.contains(expectedItem)) {
                    foundItems++;
                    break;
                }
            }
        }

        Assertions.assertTrue(foundItems == runAsShortcuts.length,
                "The runAs menu associated with project: " + MVN_APP_NAME
                        + " does not contain one or more expected entries. Expected number of entries: " + runAsShortcuts.length
                        + "Found entry count: " + foundItems + ". Found menu entries: " + runAsMenuItems);

        // Check that the Run As -> Run Configurations... contains the Liberty entry in the menu.
        SWTBotPluginOperations.launchConfigurationsDialog(bot, MVN_APP_NAME, "run");
        SWTBotTreeItem runAslibertyToolsEntry = SWTBotPluginOperations.getLibertyToolsConfigMenuItem(bot);
        Assertions.assertTrue(runAslibertyToolsEntry != null, "Liberty entry in Run Configurations view was not found.");
        bot.button("Close").click();

        // Check that the Debug As -> Debug Configurations... contains the Liberty entry in the menu.
        SWTBotPluginOperations.launchConfigurationsDialog(bot, MVN_APP_NAME, "debug");
        SWTBotTreeItem debugAslibertyToolsEntry = SWTBotPluginOperations.getLibertyToolsConfigMenuItem(bot);
        Assertions.assertTrue(debugAslibertyToolsEntry != null, "Liberty entry in Debug Configurations view was not found.");
        bot.button("Close").click();
    }

    /**
     * Tests the start menu action on a dashboard listed application.
     */
    @Test
    public void testDashboardStartAction() {

        // set the preferences
        SWTBotPluginOperations.setBuildCmdPathInPreferences(bot, "Maven");

        // Start dev mode.
        SWTBotPluginOperations.launchStartWithDashboardAction(bot, dashboard, MVN_APP_NAME);
        SWTBotView terminal = bot.viewByTitle("Terminal");
        terminal.show();

        validateApplicationOutcomeCustom("http://localhost:9080/converter1/heights.jsp?heightCm=10", true, "Height in feet and inches",
                serverModule1Path + "/target/liberty");
        validateApplicationOutcomeCustom("http://localhost:9080/converter2/heights.jsp?heightCm=20", true, "Height in feet and inches",
                serverModule1Path + "/target/liberty");

        // Stop dev mode.
        SWTBotPluginOperations.launchStopWithDashboardAction(bot, dashboard, MVN_APP_NAME);
        terminal.show();

        // Validate application stopped.
        validateApplicationStopped(serverModule1Path + "/target/liberty");

        // unset the preferences
        SWTBotPluginOperations.unsetBuildCmdPathInPreferences(bot, "Maven");

        // Close the terminal.
        terminal.close();
    }

    /**
     * Tests the start action initiated through: project -> Run As -> Run Configurations -> Liberty -> New configuration (default) ->
     * Run.
     */
    @Test
    public void testStartWithDefaultRunAsConfig() {

        // set the preferences
        SWTBotPluginOperations.setBuildCmdPathInPreferences(bot, "Maven");

        // Delete any previously created configs.
        SWTBotPluginOperations.deleteLibertyToolsConfigEntries(bot, MVN_APP_NAME, "run");

        // Start dev mode.
        SWTBotPluginOperations.launchStartWithDefaultConfig(bot, MVN_APP_NAME, "run");
        SWTBotView terminal = bot.viewByTitle("Terminal");
        terminal.show();

        // Validate application is up and running.
        validateApplicationOutcomeCustom("http://localhost:9080/converter1/heights.jsp?heightCm=30", true, "Height in feet and inches",
                serverModule1Path + "/target/liberty");

        // Stop dev mode.
        SWTBotPluginOperations.launchStopWithRunDebugAsShortcut(bot, MVN_APP_NAME, "run");
        terminal.show();

        // Validate application stopped.
        validateApplicationStopped(serverModule1Path + "/target/liberty");

        // unset the preferences
        SWTBotPluginOperations.unsetBuildCmdPathInPreferences(bot, "Maven");

        // Close the terminal.
        terminal.close();
    }
}
