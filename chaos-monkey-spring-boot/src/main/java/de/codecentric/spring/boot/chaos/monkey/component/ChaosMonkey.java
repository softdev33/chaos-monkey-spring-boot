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

import de.codecentric.spring.boot.chaos.monkey.assaults.ExceptionAssault;
import de.codecentric.spring.boot.chaos.monkey.assaults.KillAppAssault;
import de.codecentric.spring.boot.chaos.monkey.assaults.LatencyAssault;
import de.codecentric.spring.boot.chaos.monkey.configuration.ChaosMonkeySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;

/**
 * @author Benjamin Wilms
 */
public class ChaosMonkey {

    private ChaosMonkeySettings chaosMonkeySettings;

    @Autowired
    private ApplicationContext context;

    private LatencyAssault latencyAssault;
    private ExceptionAssault exceptionAssault;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosMonkey.class);


    public ChaosMonkey(ChaosMonkeySettings chaosMonkeySettings, LatencyAssault assault, ExceptionAssault exceptionAssault, KillAppAssault killAppAssault) {
        this.chaosMonkeySettings = chaosMonkeySettings;
        this.latencyAssault = assault;
        this.exceptionAssault = exceptionAssault;
    }

    public void callChaosMonkey() {
        if (isTrouble() && isEnabled()) {
            // TODO: Refactoring to Assault Management!
            int exceptionRand = chaosMonkeySettings.getAssaultProperties().chooseAssault(3);

            if (allAssaultsActive()) {

                switch (exceptionRand) {
                    case 1:
                        latencyAssault.attack();
                        break;
                    case 2:
                        exceptionAssault.attack();
                        break;
                    case 3:
                        killTheBossApp();
                        break;
                }
            } else if (isLatencyAndExceptionActive()) {
                exceptionRand = chaosMonkeySettings.getAssaultProperties().chooseAssault(2);
                switch (exceptionRand) {
                    case 1:
                        latencyAssault.attack();
                        break;
                    case 2:
                        exceptionAssault.attack();
                        break;
                }

            } else if (isLatencyAndKillAppActive()) {
                exceptionRand = chaosMonkeySettings.getAssaultProperties().chooseAssault(2);
                switch (exceptionRand) {
                    case 1:
                        latencyAssault.attack();
                        break;
                    case 2:
                        killTheBossApp();
                        break;
                }

            } else if (isExceptionAndKillAppActive()) {
                exceptionRand = chaosMonkeySettings.getAssaultProperties().chooseAssault(2);
                switch (exceptionRand) {
                    case 1:
                        exceptionAssault.attack();
                        break;
                    case 2:
                        killTheBossApp();
                        break;
                }

            } else if (latencyAssault.isActive()) {
                latencyAssault.attack();
            } else if (exceptionAssault.isActive()) {
                exceptionAssault.attack();
            } else if (chaosMonkeySettings.getAssaultProperties().isKillApplicationActive()) {
                killTheBossApp();
            }
        }

    }

    private boolean isLatencyAndKillAppActive() {
        return latencyAssault.isActive() && !chaosMonkeySettings.getAssaultProperties().isExceptionsActive() &&
                chaosMonkeySettings.getAssaultProperties().isKillApplicationActive();
    }

    private boolean isExceptionAndKillAppActive() {
        return !latencyAssault.isActive() && exceptionAssault.isActive() &&
                chaosMonkeySettings.getAssaultProperties().isKillApplicationActive();
    }

    private boolean isLatencyAndExceptionActive() {
        return latencyAssault.isActive() && exceptionAssault.isActive() &&
                !chaosMonkeySettings.getAssaultProperties().isKillApplicationActive();
    }


    private boolean allAssaultsActive() {
        return latencyAssault.isActive() && exceptionAssault.isActive() && chaosMonkeySettings.getAssaultProperties().isKillApplicationActive();
    }

    private boolean isTrouble() {
        return chaosMonkeySettings.getAssaultProperties().getTroubleRandom() >= chaosMonkeySettings.getAssaultProperties().getLevel();
    }

    private boolean isEnabled() {
        return this.chaosMonkeySettings.getChaosMonkeyProperties().isEnabled();
    }

    private void killTheBossApp() {

        try {
            LOGGER.info("Chaos Monkey - I am killing your Application!");

            int exit = SpringApplication.exit(context, new ExitCodeGenerator() {
                public int getExitCode() {
                    return 0;
                }
            });
            System.exit(exit);
        } catch (Exception e) {
            LOGGER.info("Chaos Monkey - Unable to kill the App, I am not the BOSS!");
        }
    }

}