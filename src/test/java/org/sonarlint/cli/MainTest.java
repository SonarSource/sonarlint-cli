/*
 * SonarLint CLI
 * Copyright (C) 2016 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonarlint.cli;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MainTest {
  private Main main;
  private SonarLint sonarLint;
  private Options opts;
  private ByteArrayOutputStream out;
  private ByteArrayOutputStream err;

  @Before
  public void setUp() {
    opts = mock(Options.class);
    when(opts.properties()).thenReturn(new Properties());
    createLogger();
    sonarLint = mock(SonarLint.class);
    main = new Main(opts, Logger.get(), sonarLint);
  }

  private Logger createLogger() {
    out = new ByteArrayOutputStream();
    PrintStream outStream = new PrintStream(out);
    err = new ByteArrayOutputStream();
    PrintStream errStream = new PrintStream(err);
    Logger.set(outStream, errStream);
    return Logger.get();
  }

  private String getLogs(ByteArrayOutputStream stream) {
    return new String(stream.toByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  public void testMain() {
    assertThat(main.run()).isEqualTo(Main.SUCCESS);

    verify(sonarLint).validate(any(Properties.class));
    verify(sonarLint).setDefaults(any(Properties.class), eq(false));
    verify(sonarLint).start(any(Properties.class));
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
  public void errorAnalysis() {
    Exception e = new IllegalArgumentException("dummy");
    doThrow(e).when(sonarLint).start(any(Properties.class));
    assertThat(main.run()).isEqualTo(Main.ERROR);
    assertThat(getLogs(out)).contains("EXECUTION FAILURE");
    assertThat(getLogs(err)).contains("ERROR: dummy");
  }
}
