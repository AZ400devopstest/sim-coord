package net.neology.tolling.tzc.simulator.configuration;

import lombok.RequiredArgsConstructor;
import net.neology.tolling.tzc.simulator.exception.handler.ScheduledExceptionHandler;
import org.springframework.boot.task.ThreadPoolTaskSchedulerCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@RequiredArgsConstructor
public class SchedulingConfiguration implements ThreadPoolTaskSchedulerCustomizer {

    private final ScheduledExceptionHandler handler;

    @Override
    public void customize(ThreadPoolTaskScheduler taskScheduler) {
        taskScheduler.setErrorHandler(handler);
    }
}
