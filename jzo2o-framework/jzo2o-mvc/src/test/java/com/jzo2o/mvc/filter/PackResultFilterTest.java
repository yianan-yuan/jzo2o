package com.jzo2o.mvc.filter;

import javax.servlet.FilterChain;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class PackResultFilterTest {
    @Test
    void shouldNotBufferOrWrapEventStream() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/aigc/consumer/assistant/sessions/s1/messages");
        request.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_EVENT_STREAM_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
            res.getWriter().write("event: delta\ndata: {\"text\":\"你\"}\n\n");
            res.getWriter().flush();
        };

        new PackResultFilter().doFilter(request, response, chain);

        assertThat(response.getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(response.getContentAsString()).isEqualTo("event: delta\ndata: {\"text\":\"你\"}\n\n");
        assertThat(response.getContentAsString()).doesNotContain("\"code\":200");
    }
}
