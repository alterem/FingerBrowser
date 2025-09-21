package com.basis.fingerbrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FontSettings {
    private boolean spoof = true;

    public boolean isSpoof() {
        return spoof;
    }

    public void setSpoof(boolean spoof) {
        this.spoof = spoof;
    }
}

