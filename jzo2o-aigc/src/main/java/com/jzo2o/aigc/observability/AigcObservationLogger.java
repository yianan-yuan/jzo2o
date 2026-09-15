package com.jzo2o.aigc.observability;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class AigcObservationLogger {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void log(AigcObservation observation) {
        try {
            log.info(OBJECT_MAPPER.writeValueAsString(
                    Objects.requireNonNull(observation, "observation").toLogFields()));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("Unable to serialize AIGC observation", error);
        }
    }
}
