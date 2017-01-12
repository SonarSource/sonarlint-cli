/*
 * SonarLint CLI
 * Copyright (C) 2016-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.cli.analysis.SonarLint;
import org.sonarlint.cli.analysis.SonarLintFactory;
import org.sonarlint.cli.report.ReportFactory;
import org.sonarlint.cli.util.Logger;
import org.sonarlint.cli.util.System2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonarlint.cli.SonarProperties.PROJECT_HOME;

public class MainTest {
  private Main main;
  private SonarLintFactory sonarLintFactory;
  private SonarLint sonarLint;
  private ReportFactory reportFactory;
  private InputFileFinder fileFinder;
  private Options opts;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    temp.newFolder("plugins");
    opts = mock(Options.class);
    reportFactory = mock(ReportFactory.class);
    when(opts.properties()).thenReturn(new Properties());
    setUpLogger();
    sonarLint = mock(SonarLint.class);
    sonarLintFactory = mock(SonarLintFactory.class);
    when(sonarLintFactory.createSonarLint(any(Path.class), anyBoolean(), anyBoolean())).thenReturn(sonarLint);
    fileFinder = new InputFileFinder(null, null, null, Charset.defaultCharset());
    Path projectHome = temp.newFolder().toPath();
    main = new Main(opts, sonarLintFactory, reportFactory, fileFinder, projectHome);
  }

  private void setUpLogger() {
    out = new ByteArrayOutputStream();
    PrintStream outStream = new PrintStream(out);
    err = new ByteArrayOutputStream();
    PrintStream errStream = new PrintStream(err);
    Logger.set(outStream, errStream);
  }

  private String getLogs(ByteArrayOutputStream stream) {
    return new String(stream.toByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  public void testMain() {
    assertThat(main.run()).isEqualTo(Main.SUCCESS);
    verify(sonarLint).stop();
  }

  @Test
  public void exitOnHelp() {
    when(opts.isHelp()).thenReturn(true);
    assertThat(main.run()).isEqualTo(Main.SUCCESS);
    verifyZeroInteractions(sonarLint);
  }

  @Test
  public void exitOnVersion() {
    when(opts.isVersion()).thenReturn(true);
    assertThat(main.run()).isEqualTo(Main.SUCCESS);
    verifyZeroInteractions(sonarLint);
  }

  @Test
  public void noPlugins() throws IOException {
    System2 sys = mock(System2.class);
    Path emptyDir = temp.newFolder().toPath();
    when(sys.getProperty(PROJECT_HOME)).thenReturn(emptyDir.toString());
    Main.execute(new String[0], sys);
    verify(sys).exit(Main.ERROR);
    assertThat(getLogs(err)).contains("Error loading plugins");
  }

  @Test
  public void errorStart() throws IOException {
    Exception e = createException("invalid operation", "analysis failed");
    doThrow(e).when(sonarLint).runAnalysis(anyMapOf(String.class, String.class), any(ReportFactory.class), any(InputFileFinder.class), any(Path.class));
    assertThat(main.run()).isEqualTo(Main.ERROR);
    assertThat(getLogs(out)).contains("EXECUTION FAILURE");
    assertThat(getLogs(err)).contains("invalid operation");
  }

  @Test
  public void invalidCharset() {
    System.setProperty("sonarlint.home", temp.getRoot().getAbsolutePath());

    System2 sys = mock(System2.class);
    String[] args = {"--charset", "invalid"};
    Main.execute(args, sys);

    verify(sys).exit(Main.ERROR);
    assertThat(getLogs(err)).contains("ERROR: Error creating charset: invalid");
  }

  @Test
  public void errorParsing() {
    System2 sys = mock(System2.class);
    String[] args = {"-invalid"};
    Main.execute(args, sys);

    verify(sys).exit(Main.ERROR);
    assertThat(getLogs(err)).contains("ERROR: Error parsing arguments: Unrecognized option: -invalid");
  }

  @Test
  public void runInteractive() throws IOException, InterruptedException {
    when(opts.isInteractive()).thenReturn(true);
    PipedOutputStream out = new PipedOutputStream();
    OutputStreamWriter writter = new OutputStreamWriter(out);
    PipedInputStream in = new PipedInputStream(out);

    final AtomicInteger mutableInt = new AtomicInteger(Main.ERROR);
    main.setIn(in);

    Thread t = new Thread(() -> mutableInt.set(main.run()));
    t.start();

    writter.write(System.lineSeparator());
    writter.close();
    t.join(20000);

    assertThat(mutableInt.get()).isEqualTo(Main.SUCCESS);
    verify(sonarLint, times(1)).stop();
    verify(sonarLint, times(2)).runAnalysis(anyMapOf(String.class, String.class), eq(reportFactory), eq(fileFinder), any(Path.class));
  }

  @Test
  public void verbose() {
    assertThat(main.run()).isEqualTo(Main.SUCCESS);
    assertThat(Logger.get().isDebugEnabled()).isFalse();

    when(opts.isVerbose()).thenReturn(true);
    assertThat(main.run()).isEqualTo(Main.SUCCESS);
    assertThat(Logger.get().isDebugEnabled()).isTrue();
  }

  @Test
  public void errorStop() {
    Exception e = createException("invalid operation", "analysis failed");
    doThrow(e).when(sonarLint).stop();
    assertThat(main.run()).isEqualTo(Main.ERROR);
    assertThat(getLogs(out)).contains("EXECUTION FAILURE");
    assertThat(getLogs(err)).contains("invalid operation");
  }

  @Test
  public void errorAnalysis() throws IOException {
    Exception e = createException("invalid operation", "analysis failed");
    doThrow(e).when(sonarLint).runAnalysis(anyMapOf(String.class, String.class), eq(reportFactory), eq(fileFinder), any(Path.class));
    assertThat(main.run()).isEqualTo(Main.ERROR);
    assertThat(getLogs(out)).contains("EXECUTION FAILURE");
    assertThat(getLogs(err)).contains("invalid operation");
  }

  @Test
  public void showStack() throws IOException {
    when(opts.showStack()).thenReturn(true);
    Exception e = createException("invalid operation", "analysis failed");
    doThrow(e).when(sonarLint).runAnalysis(anyMapOf(String.class, String.class), any(ReportFactory.class), any(InputFileFinder.class), any(Path.class));
    assertThat(main.run()).isEqualTo(Main.ERROR);
    assertThat(getLogs(out)).contains("EXECUTION FAILURE");
    assertThat(getLogs(err)).contains("invalid operation");
    assertThat(getLogs(err)).contains("analysis failed");
  }

  public Exception createException(String firstMsg, String secondMsg) {
    Exception wrapped = new NullPointerException(firstMsg);
    return new IllegalStateException(secondMsg, wrapped);
  }
}
