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
package org.sonarlint.cli.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonarlint.cli.util.Logger;

import java.io.PrintStream;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LoggerTest {
  @Mock
  private PrintStream stdOut;

  @Mock
  private PrintStream stdErr;

  private Logger logger;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    logger = new Logger(stdOut, stdErr);
  }

  @Test
  public void testInfo() {
    logger.info("info");
    verify(stdOut).println("INFO: info");
    verifyNoMoreInteractions(stdOut, stdErr);
  }

  @Test
  public void testError() {
    Exception e = new NullPointerException("exception");
    logger.setDisplayStackTrace(false);
    logger.error("error1");
    verify(stdErr).println("ERROR: error1");

    logger.error("error2", e);
    verify(stdErr).println("ERROR: error2");

    verifyNoMoreInteractions(stdOut, stdErr);

    logger.setDisplayStackTrace(true);
    logger.error("error3", e);
    verify(stdErr).println("ERROR: error3");
    // other interactions to print the exception..
  }

  @Test
  public void testDebugThrowableWithStack() {
    Throwable t = mock(Throwable.class);
    logger.setDebugEnabled(true);

    logger.setDebugEnabled(true);
    logger.setDisplayStackTrace(true);
    logger.debug("debug", t);
    verify(stdErr).println("DEBUG: debug");
    verify(t).printStackTrace(stdErr);

    logger.setDebugEnabled(false);
    logger.debug("debug");
    verifyNoMoreInteractions(stdOut, stdErr);
  }

  @Test
  public void testDebugThrowableWithoutStack() {
    Throwable t = mock(Throwable.class);
    logger.setDebugEnabled(true);
    logger.setDisplayStackTrace(false);

    logger.debug("debug", t);
    verify(stdErr).println("DEBUG: debug");
    verifyZeroInteractions(t);
  }

  @Test
  public void testDebug() {
    logger.setDebugEnabled(true);

    logger.debug("debug");
    verify(stdOut).println("DEBUG: debug");

    logger.setDebugEnabled(false);
    logger.debug("debug");
    verifyNoMoreInteractions(stdOut, stdErr);
  }
}
