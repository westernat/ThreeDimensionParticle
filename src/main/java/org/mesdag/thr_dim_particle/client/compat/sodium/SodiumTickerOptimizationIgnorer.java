package org.mesdag.thr_dim_particle.client.compat.sodium;

public interface SodiumTickerOptimizationIgnorer {
    void tdp$setIgnored();

    boolean tdp$isIgnored();

    static void setIgnore(Object o) {
        ((SodiumTickerOptimizationIgnorer) o).tdp$setIgnored();
    }

    static boolean isIgnored(Object o) {
        return ((SodiumTickerOptimizationIgnorer) o).tdp$isIgnored();
    }
}
