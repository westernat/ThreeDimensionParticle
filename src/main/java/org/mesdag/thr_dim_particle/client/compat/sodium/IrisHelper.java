package org.mesdag.thr_dim_particle.client.compat.sodium;

import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;
import org.mesdag.thr_dim_particle.TDP;

public class IrisHelper {
    public static void setAllowUnknownShaders() {
        try {
            IrisConfig config = Iris.getIrisConfig();
            if (!config.shouldAllowUnknownShaders()) {
                config.setUnknown(true);
            }
        } catch (Exception e) {
            TDP.LOGGER.warn("Failed to make our shaders to be allowed from iris");
        }
    }

    public static boolean hasShader() {
        return Iris.getIrisConfig().areShadersEnabled();
    }
}
