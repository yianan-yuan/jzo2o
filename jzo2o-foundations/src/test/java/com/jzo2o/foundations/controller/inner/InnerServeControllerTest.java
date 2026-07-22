package com.jzo2o.foundations.controller.inner;

import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.foundations.model.dto.response.ServeSimpleResDTO;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.foundations.service.ServeAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InnerServeControllerTest {

    @Test
    void shouldUseDefaultLimitWhenHttpRequestOmitsLimit() throws Exception {
        IServeService serveService = mock(IServeService.class);
        ServeAggregationService aggregationService = mock(ServeAggregationService.class);
        InnerServeController controller = new InnerServeController();
        ReflectionTestUtils.setField(controller, "serveService", serveService);
        ReflectionTestUtils.setField(controller, "serveAggregationService", aggregationService);
        when(aggregationService.findServeList("010", null, "\u4fdd\u6d01"))
                .thenReturn(Collections.emptyList());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(new Validator() {
                    @Override
                    public boolean supports(Class<?> clazz) {
                        return false;
                    }

                    @Override
                    public void validate(Object target, Errors errors) {
                    }
                })
                .build();

        mockMvc.perform(get("/inner/serve/search")
                        .param("cityCode", "010")
                        .param("keyword", "\u4fdd\u6d01"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnOnlyActiveServicesInRequestedCity() {
        IServeService serveService = mock(IServeService.class);
        ServeAggregationService aggregationService = mock(ServeAggregationService.class);
        InnerServeController controller = new InnerServeController();
        ReflectionTestUtils.setField(controller, "serveService", serveService);
        ReflectionTestUtils.setField(controller, "serveAggregationService", aggregationService);
        when(aggregationService.findServeList("010", null, "\u4fdd\u6d01"))
                .thenReturn(Arrays.asList(simple(1L), simple(2L), simple(3L)));
        when(serveService.findServeDetailById(1L)).thenReturn(detail(1L, "010", 2));
        when(serveService.findServeDetailById(2L)).thenReturn(detail(2L, "010", 1));
        when(serveService.findServeDetailById(3L)).thenReturn(detail(3L, "021", 2));

        List<ServeAggregationResDTO> result = controller.searchActiveServes("010", "\u4fdd\u6d01", 20);

        assertThat(result).extracting(ServeAggregationResDTO::getId).containsExactly(1L);
    }

    @Test
    void shouldApplyDefaultLimitAfterFiltering() {
        IServeService serveService = mock(IServeService.class);
        ServeAggregationService aggregationService = mock(ServeAggregationService.class);
        InnerServeController controller = new InnerServeController();
        ReflectionTestUtils.setField(controller, "serveService", serveService);
        ReflectionTestUtils.setField(controller, "serveAggregationService", aggregationService);
        List<ServeSimpleResDTO> candidates = simpleRange(1L, 22L);
        when(aggregationService.findServeList("010", null, "keyword")).thenReturn(candidates);
        when(serveService.findServeDetailById(1L)).thenReturn(detail(1L, "010", 1));
        for (long id = 2L; id <= 22L; id++) {
            when(serveService.findServeDetailById(id)).thenReturn(detail(id, "010", 2));
        }

        List<ServeAggregationResDTO> result = controller.searchActiveServes("010", "keyword", null);

        assertThat(result).extracting(ServeAggregationResDTO::getId)
                .containsExactlyElementsOf(idRange(2L, 21L));
    }

    @Test
    void shouldCapLimitAtTwenty() {
        IServeService serveService = mock(IServeService.class);
        ServeAggregationService aggregationService = mock(ServeAggregationService.class);
        InnerServeController controller = new InnerServeController();
        ReflectionTestUtils.setField(controller, "serveService", serveService);
        ReflectionTestUtils.setField(controller, "serveAggregationService", aggregationService);
        List<ServeSimpleResDTO> candidates = simpleRange(1L, 25L);
        when(aggregationService.findServeList("010", null, "keyword")).thenReturn(candidates);
        for (long id = 1L; id <= 25L; id++) {
            when(serveService.findServeDetailById(id)).thenReturn(detail(id, "010", 2));
        }

        List<ServeAggregationResDTO> result = controller.searchActiveServes("010", "keyword", 50);

        assertThat(result).extracting(ServeAggregationResDTO::getId)
                .containsExactlyElementsOf(idRange(1L, 20L));
    }

    @Test
    void shouldUseMinimumLimitForValuesBelowOne() {
        IServeService serveService = mock(IServeService.class);
        ServeAggregationService aggregationService = mock(ServeAggregationService.class);
        InnerServeController controller = new InnerServeController();
        ReflectionTestUtils.setField(controller, "serveService", serveService);
        ReflectionTestUtils.setField(controller, "serveAggregationService", aggregationService);
        when(aggregationService.findServeList("010", null, "keyword"))
                .thenReturn(Arrays.asList(simple(1L), simple(2L)));
        when(serveService.findServeDetailById(1L)).thenReturn(detail(1L, "010", 2));
        when(serveService.findServeDetailById(2L)).thenReturn(detail(2L, "010", 2));

        List<ServeAggregationResDTO> result = controller.searchActiveServes("010", "keyword", 0);

        assertThat(result).extracting(ServeAggregationResDTO::getId).containsExactly(1L);
    }

    private List<ServeSimpleResDTO> simpleRange(long startInclusive, long endInclusive) {
        return LongStream.rangeClosed(startInclusive, endInclusive)
                .mapToObj(this::simple)
                .collect(Collectors.toList());
    }

    private List<Long> idRange(long startInclusive, long endInclusive) {
        return LongStream.rangeClosed(startInclusive, endInclusive)
                .boxed()
                .collect(Collectors.toList());
    }

    private ServeSimpleResDTO simple(Long id) {
        ServeSimpleResDTO result = new ServeSimpleResDTO();
        result.setId(id);
        return result;
    }

    private ServeAggregationResDTO detail(Long id, String cityCode, Integer saleStatus) {
        ServeAggregationResDTO result = new ServeAggregationResDTO();
        result.setId(id);
        result.setCityCode(cityCode);
        result.setSaleStatus(saleStatus);
        return result;
    }
}
