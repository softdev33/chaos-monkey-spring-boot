/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.spring.boot.chaos.monkey.component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import de.codecentric.spring.boot.chaos.monkey.assaults.ExceptionAssault;
import de.codecentric.spring.boot.chaos.monkey.assaults.LatencyAssault;
import de.codecentric.spring.boot.chaos.monkey.configuration.AssaultProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeyProperties;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * @author Benjamin Wilms
 */
@RunWith(MockitoJUnitRunner.class)
public class ChaosMonkeyTest {

    private ChaosMonkey chaosMonkey;

    @Mock
    private Appender mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> captorLoggingEvent;


    @Mock
    private AssaultProperties assaultProperties;

    @Mock
    private ChaosMonkeyProperties chaosMonkeyProperties;

    @Mock
    private ChaosMonkeySettings  chaosMonkeySettings;

    @Mock
    private LatencyAssault latencyAssault;

    @Mock
    private ExceptionAssault exceptionAssault;

    @Before
    public void setUp() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        captorLoggingEvent = ArgumentCaptor.forClass(LoggingEvent.class);

        given(this.assaultProperties.getLevel()).willReturn(1);
        given(this.assaultProperties.getTroubleRandom()).willReturn(10);
        given(this.chaosMonkeyProperties.isEnabled()).willReturn(true);
        given(this.assaultProperties.getLevel()).willReturn(1);
        given(this.assaultProperties.getTroubleRandom()).willReturn(5);
        given(this.chaosMonkeyProperties.isEnabled()).willReturn(true);
        given(this.chaosMonkeySettings.getAssaultProperties()).willReturn(this.assaultProperties);
        given(this.chaosMonkeySettings.getChaosMonkeyProperties()).willReturn(this.chaosMonkeyProperties);

        chaosMonkey = new ChaosMonkey(chaosMonkeySettings, latencyAssault, exceptionAssault);

    }

    @Test
    public void allAssaultsActiveExpectLatencyAttack() {
        given(this.exceptionAssault.isActive()).willReturn(true);
        given(this.latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(3)).willReturn(1);

        chaosMonkey.callChaosMonkey();

        verify(latencyAssault, times(1)).attack();
    }

    @Test
    public void allAssaultsActiveExpectExceptionLogging() {
        given(this.exceptionAssault.isActive()).willReturn(true);
        given(this.latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(3)).willReturn(2);

        chaosMonkey.callChaosMonkey();

        verify(exceptionAssault, times(1)).attack();
    }

    @Test
    public void allAssaultsActiveExpectKillLogging() {
        given(this.exceptionAssault.isActive()).willReturn(true);
        given(this.latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(3)).willReturn(3);

        chaosMonkey.callChaosMonkey();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(0).getLevel());
        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(1).getLevel());
        assertEquals("Chaos Monkey - I am killing your Application!", captorLoggingEvent.getAllValues().get(0).getMessage());
        assertEquals("Chaos Monkey - Unable to kill the App, I am not the BOSS!", captorLoggingEvent.getAllValues().get(1).getMessage());
    }

    @Test
    public void isKillAppAssaultActive() {

        given(this.exceptionAssault.isActive()).willReturn(false);
        given(this.latencyAssault.isActive()).willReturn(false);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.getLevel()).willReturn(1);
        given(this.assaultProperties.getTroubleRandom()).willReturn(5);
        given(this.chaosMonkeyProperties.isEnabled()).willReturn(true);


        chaosMonkey.callChaosMonkey();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(0).getLevel());
        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(1).getLevel());
        assertEquals("Chaos Monkey - I am killing your Application!", captorLoggingEvent.getAllValues().get(0).getMessage());
        assertEquals("Chaos Monkey - Unable to kill the App, I am not the BOSS!", captorLoggingEvent.getAllValues().get(1).getMessage());

    }

    @Test
    public void isLatencyAssaultActive() {

        given(this.latencyAssault.isActive()).willReturn(true);
        given(this.exceptionAssault.isActive()).willReturn(false);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(false);

        chaosMonkey.callChaosMonkey();

        verify(latencyAssault, times(1)).attack();
    }

    @Test
    public void isExceptionAssaultActive() {
        given(this.exceptionAssault.isActive()).willReturn(true);
        given(this.latencyAssault.isActive()).willReturn(false);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(false);

        chaosMonkey.callChaosMonkey();

        verify(exceptionAssault, times(1)).attack();
    }

    @Test
    public void isExceptionAndLatencyAssaultActiveExpectExceptionLogging() {
        given(this.exceptionAssault.isActive()).willReturn(true);
        given(this.latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(false);
        given(this.assaultProperties.chooseAssault(2)).willReturn(2);

        chaosMonkey.callChaosMonkey();

        verify(exceptionAssault, times(1)).attack();
    }

    @Test
    public void isExceptionAndLatencyAssaultActiveExpectLatencyAttack() {

        given(this.exceptionAssault.isActive()).willReturn(true);
        given(this.latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(false);
        given(this.assaultProperties.chooseAssault(2)).willReturn(1);

        chaosMonkey.callChaosMonkey();

        verify(latencyAssault, times(1)).attack();
    }

    @Test
    public void isExceptionAndKillAssaultActiveExpectExceptionLogging() {
        given(exceptionAssault.isActive()).willReturn(true);
        given(latencyAssault.isActive()).willReturn(false);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(2)).willReturn(1);

        chaosMonkey.callChaosMonkey();

        verify(exceptionAssault, times(1)).attack();
    }

    @Test
    public void isExceptionAndKillAssaultActiveExpectKillLogging() {
        given(exceptionAssault.isActive()).willReturn(true);
        given(latencyAssault.isActive()).willReturn(false);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(2)).willReturn(2);

        chaosMonkey.callChaosMonkey();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(0).getLevel());
        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(1).getLevel());
        assertEquals("Chaos Monkey - I am killing your Application!", captorLoggingEvent.getAllValues().get(0).getMessage());
        assertEquals("Chaos Monkey - Unable to kill the App, I am not the BOSS!", captorLoggingEvent.getAllValues().get(1).getMessage());
    }

    @Test
    public void isLatencyAndKillAssaultActiveExpectLatencyAttack() {
        given(exceptionAssault.isActive()).willReturn(false);
        given(latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(2)).willReturn(1);

        chaosMonkey.callChaosMonkey();

        verify(latencyAssault, times(1)).attack();
    }

    @Test
    public void isLatencyAndKillAssaultActiveExpectKillLogging() {
        given(exceptionAssault.isActive()).willReturn(false);
        given(latencyAssault.isActive()).willReturn(true);
        given(this.assaultProperties.isKillApplicationActive()).willReturn(true);
        given(this.assaultProperties.chooseAssault(2)).willReturn(2);

        chaosMonkey.callChaosMonkey();

        verify(mockAppender, times(2)).doAppend(captorLoggingEvent.capture());

        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(0).getLevel());
        assertEquals(Level.INFO, captorLoggingEvent.getAllValues().get(1).getLevel());
        assertEquals("Chaos Monkey - I am killing your Application!", captorLoggingEvent.getAllValues().get(0).getMessage());
        assertEquals("Chaos Monkey - Unable to kill the App, I am not the BOSS!", captorLoggingEvent.getAllValues().get(1).getMessage());
    }

    @Test
    public void givenNoAssaultsActiveExpectNoLogging() {
        chaosMonkey.callChaosMonkey();

        verify(mockAppender, never()).doAppend(captorLoggingEvent.capture());


    }

    @Test
    public void givenAssaultLevelTooHighExpectNoLogging() {
        given(this.assaultProperties.getLevel()).willReturn(10);
        given(this.assaultProperties.getTroubleRandom()).willReturn(9);
        given(this.latencyAssault.isActive()).willReturn(true);

        chaosMonkey.callChaosMonkey();

        verify(mockAppender, never()).doAppend(captorLoggingEvent.capture());


    }

    @Test
    public void isChaosMonkeyExecutionDisabled() {
        given(this.chaosMonkeyProperties.isEnabled()).willReturn(false);

        chaosMonkey.callChaosMonkey();

        verify(mockAppender, never()).doAppend(captorLoggingEvent.capture());


    }
}