package com.telamin.fluxtion.builder.test.util;

public enum SepTestConfig {
    COMPILED_INLINE(true),
    COMPILED_METHOD_PER_EVENT(true),
    COMPILED_SWITCH_DISPATCH(true),
    COMPILED_DISPATCH_ONLY(true),
    INTERPRETED(false);
    private final boolean compiled;

    public boolean isCompiled() {
        return compiled;
    }

    SepTestConfig(boolean compiled) {
        this.compiled = compiled;
    }
}
