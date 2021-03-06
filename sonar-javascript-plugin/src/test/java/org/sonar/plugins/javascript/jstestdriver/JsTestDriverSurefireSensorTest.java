/*
 * Sonar JavaScript Plugin
 * Copyright (C) 2011 Eriks Nukis and SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.javascript.jstestdriver;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.plugins.javascript.JavaScriptPlugin;
import org.sonar.plugins.javascript.core.JavaScript;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JsTestDriverSurefireSensorTest {

  private JsTestDriverSurefireSensor sensor;
  private SensorContext context;
  private JavaScript language;
  private Settings settings;

  @Before
  public void init() {
    settings = new Settings();
    language = new JavaScript(settings);
    sensor = new JsTestDriverSurefireSensor(language);
    context = mock(SensorContext.class);
  }

  @Test
  public void test_shouldExecuteOnProject() {
    Project project = mockProject();
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    project.setLanguage(language);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    settings.setProperty(JavaScriptPlugin.TEST_FRAMEWORK_KEY, "jstestdriver");
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void testAnalyseUnitTests() throws URISyntaxException {
    settings.setProperty(JavaScriptPlugin.JSTESTDRIVER_FOLDER_KEY, JavaScriptPlugin.JSTESTDRIVER_DEFAULT_FOLDER);
    settings.setProperty(JavaScriptPlugin.TEST_FRAMEWORK_KEY, "jstestdriver");

    final ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
    when(fileSystem.getSourceCharset()).thenReturn(Charset.defaultCharset());

    final File folder = new File(getClass().getResource("/org/sonar/plugins/javascript/jstestdriver/sensortests").toURI());
    when(fileSystem.getBasedir()).thenReturn(folder);

    File testDir = new File(getClass().getResource("/org/sonar/plugins/javascript/jstestdriver/sensortests/test").toURI());
    List<File> testDirectories = new ArrayList<File>();
    testDirectories.add(testDir);
    when(fileSystem.getTestDirs()).thenReturn(testDirectories);

    Project project = mockProject();
    project.setLanguage(language);
    project.setFileSystem(fileSystem);

    assertTrue(sensor.shouldExecuteOnProject(project));

    sensor.analyse(project, context);

    verify(context).saveMeasure((Resource) anyObject(), eq(CoreMetrics.TESTS), eq(2.0));
    verify(context).saveMeasure((Resource) anyObject(), eq(CoreMetrics.SKIPPED_TESTS), eq(0.0));
    verify(context).saveMeasure((Resource) anyObject(), eq(CoreMetrics.TEST_ERRORS), eq(0.0));
    verify(context).saveMeasure((Resource) anyObject(), eq(CoreMetrics.TEST_FAILURES), eq(0.0));
    verify(context).saveMeasure((Resource) anyObject(), eq(CoreMetrics.TEST_EXECUTION_TIME), eq(700.0));
    verify(context).saveMeasure((Resource) anyObject(), eq(CoreMetrics.TEST_SUCCESS_DENSITY), eq(100.0));

    verify(context).saveSource((Resource) anyObject(), eq("This is content for PersonTest.js JavaScript file used in unit tests."));
  }

  @Test
  public void testGetUnitTestFileName() {
    assertEquals("com/company/PersonTest.js", sensor.getUnitTestFileName("Chrome_16091263_Windows.com.company.PersonTest"));
    assertEquals("PersonTest.js", sensor.getUnitTestFileName("Chrome_16091263_Windows.PersonTest"));
    assertEquals("JsTestDriverSurefireSensor", sensor.toString());
  }

  private Project mockProject() {
    return new Project("mock");
  }


}
