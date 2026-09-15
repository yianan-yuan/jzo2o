package com.jzo2o.aigc.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandDecision implements Serializable {

    private static final long serialVersionUID = 1L;

    private DemandProfile profile;
    private boolean needsClarification;
    private String clarifyingQuestion;
    private Integer referencedRecommendationIndex;
    private Long referencedServeId;
}
