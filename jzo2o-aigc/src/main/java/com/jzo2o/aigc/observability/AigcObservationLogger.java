package com.jzo2o.aigc.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class AigcObservationLogger {

    public void log(AigcObservation observation) {
        log.info("aigc_observation={}", Objects.requireNonNull(observation, "observation").toLogFields());
    }
}
