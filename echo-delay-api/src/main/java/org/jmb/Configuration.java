package org.jmb;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "echo-delay")
public interface Configuration {
    @WithDefault("200")
    long delayInMiliseconds();
}
