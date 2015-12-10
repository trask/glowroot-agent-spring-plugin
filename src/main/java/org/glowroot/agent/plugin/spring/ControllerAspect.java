/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glowroot.agent.plugin.spring;

import org.glowroot.agent.plugin.api.Agent;
import org.glowroot.agent.plugin.api.config.ConfigService;
import org.glowroot.agent.plugin.api.transaction.MessageSupplier;
import org.glowroot.agent.plugin.api.transaction.TimerName;
import org.glowroot.agent.plugin.api.transaction.TraceEntry;
import org.glowroot.agent.plugin.api.transaction.TransactionService;
import org.glowroot.agent.plugin.api.weaving.BindMethodMeta;
import org.glowroot.agent.plugin.api.weaving.BindThrowable;
import org.glowroot.agent.plugin.api.weaving.BindTraveler;
import org.glowroot.agent.plugin.api.weaving.IsEnabled;
import org.glowroot.agent.plugin.api.weaving.OnBefore;
import org.glowroot.agent.plugin.api.weaving.OnReturn;
import org.glowroot.agent.plugin.api.weaving.OnThrow;
import org.glowroot.agent.plugin.api.weaving.Pointcut;

public class ControllerAspect {

    private static final TransactionService transactionService = Agent.getTransactionService();
    private static final ConfigService configService = Agent.getConfigService("spring");

    @Pointcut(classAnnotation = "org.springframework.stereotype.Controller",
            methodAnnotation = "org.springframework.web.bind.annotation.RequestMapping",
            methodParameterTypes = {".."}, timerName = "spring controller")
    public static class ControllerAdvice {
        private static final TimerName timerName =
                transactionService.getTimerName(ControllerAdvice.class);
        @IsEnabled
        public static boolean isEnabled() {
            return configService.isEnabled();
        }
        @OnBefore
        public static TraceEntry onBefore(
                @BindMethodMeta ControllerMethodMeta controllerMethodMeta) {
            transactionService.setTransactionName(controllerMethodMeta.getPath());
            return transactionService.startTraceEntry(
                    MessageSupplier.from("spring controller: {}.{}()",
                            controllerMethodMeta.getDeclaredClassSimpleName(),
                            controllerMethodMeta.getMethodName()),
                    timerName);
        }
        @OnReturn
        public static void onReturn(@BindTraveler TraceEntry traceEntry) {
            traceEntry.end();
        }
        @OnThrow
        public static void onThrow(@BindThrowable Throwable throwable,
                @BindTraveler TraceEntry traceEntry) {
            traceEntry.endWithError(throwable);
        }
    }
}
