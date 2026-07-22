package com.jzo2o.aigc.service;

import com.jzo2o.api.foundations.ServeApi;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCatalogService {

    private final ServeApi serveApi;

    public List<ServeAggregationResDTO> search(String cityCode, String keyword, int limit) {
        return serveApi.searchActiveServes(cityCode, keyword, Math.min(limit, 20));
    }

    public ServeAggregationResDTO findById(Long serveId) {
        return serveApi.findById(serveId);
    }
}
