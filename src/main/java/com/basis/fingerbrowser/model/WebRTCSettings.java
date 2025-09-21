package com.basis.fingerbrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebRTCSettings {
    private boolean enabled = true;
    private String ipHandlingPolicy = "default_public_interface_only";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIpHandlingPolicy() {
        return ipHandlingPolicy;
    }

    public void setIpHandlingPolicy(String ipHandlingPolicy) {
        this.ipHandlingPolicy = ipHandlingPolicy;
    }
}

