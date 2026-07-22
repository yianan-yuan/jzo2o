package com.jzo2o.foundations.controller.inner;

import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.foundations.model.dto.response.ServeSimpleResDTO;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.foundations.service.ServeAggregationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InnerServeControllerTest {

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
