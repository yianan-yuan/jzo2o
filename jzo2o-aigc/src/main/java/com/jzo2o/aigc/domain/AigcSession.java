package com.jzo2o.aigc.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public class AigcSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;
    private Long userId;
    private String cityCode;
    private DemandProfile demandProfile = new DemandProfile();
    private List<ChatTurn> recentChatTurns = new ArrayList<>();
    private List<Long> lastRecommendedServeIds = new ArrayList<>();
    private ConversationStage stage = ConversationStage.INITIAL;

    public static AigcSession create(String sessionId, Long userId) {
        AigcSession session = new AigcSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        return session;
    }

    public void updateCity(String cityCode) {
        if (!Objects.equals(this.cityCode, cityCode)) {
            this.lastRecommendedServeIds.clear();
        }
        this.cityCode = cityCode;
    }

    public void setLastRecommendedServeIds(List<Long> lastRecommendedServeIds) {
        this.lastRecommendedServeIds = lastRecommendedServeIds == null
                ? new ArrayList<>()
                : new ArrayList<>(lastRecommendedServeIds);
    }

    public void addChatTurn(ChatTurn chatTurn, int maxRounds) {
        this.recentChatTurns.add(chatTurn);
        while (this.recentChatTurns.size() > maxRounds) {
            this.recentChatTurns.remove(0);
        }
    }
}
