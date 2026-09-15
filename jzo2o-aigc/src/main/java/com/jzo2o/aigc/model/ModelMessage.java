package com.jzo2o.aigc.model;

import java.util.Objects;

public final class ModelMessage {

    private final String role;
    private final String content;

    public ModelMessage(String role, String content) {
        this.role = Objects.requireNonNull(role, "role");
        this.content = Objects.requireNonNull(content, "content");
    }

    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
