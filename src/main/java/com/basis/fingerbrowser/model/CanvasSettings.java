package com.basis.fingerbrowser.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CanvasSettings {
    private boolean spoof = true;
    private double noise = 0.0;

    public boolean isSpoof() {
        return spoof;
    }

    public void setSpoof(boolean spoof) {
        this.spoof = spoof;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }
}

