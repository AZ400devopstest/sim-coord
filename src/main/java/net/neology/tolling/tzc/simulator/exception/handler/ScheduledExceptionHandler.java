package net.neology.tolling.tzc.simulator.exception.handler;

import lombok.extern.log4j.Log4j2;
import net.neology.tolling.tzc.simulator.exception.utils.ExceptionUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ErrorHandler;

@Log4j2
@Component
public class ScheduledExceptionHandler implements ErrorHandler {

    @Override
    public void handleError(Throwable throwable) {
        log.error("Unexpected error.  Message: {}", ExceptionUtils.getRootCauseMessage(throwable), throwable);
    }
}
