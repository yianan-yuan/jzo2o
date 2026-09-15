package com.jzo2o.aigc.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DemandProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    private String summary;
    private String searchKeyword;
    private String serviceTypeHint;
    private List<String> confirmedConstraints = new ArrayList<>();
    private Map<String, String> clarifiedFacts = new LinkedHashMap<>();
}
