package com.jzo2o.aigc.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
@NoArgsConstructor
public class AigcSession implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int MAX_CHAT_TURNS = 10;

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

    public List<ChatTurn> getRecentChatTurns() {
        return Collections.unmodifiableList(new ArrayList<>(recentChatTurns));
    }

    public void setRecentChatTurns(List<ChatTurn> recentChatTurns) {
        if (recentChatTurns == null) {
            this.recentChatTurns = new ArrayList<>();
            return;
        }
        int firstRetainedIndex = Math.max(0, recentChatTurns.size() - MAX_CHAT_TURNS);
        this.recentChatTurns = new ArrayList<>(recentChatTurns.subList(firstRetainedIndex, recentChatTurns.size()));
    }

    public void addChatTurn(ChatTurn chatTurn, int maxRounds) {
        int retainedRounds = Math.min(MAX_CHAT_TURNS, Math.max(1, maxRounds));
        this.recentChatTurns.add(chatTurn);
        while (this.recentChatTurns.size() > retainedRounds) {
            this.recentChatTurns.remove(0);
        }
    }
}
