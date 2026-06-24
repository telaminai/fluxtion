package com.telamin.fluxtion.builder.replay;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

final class ReplayYamlFactory {

    private ReplayYamlFactory() {
    }

    static Yaml newYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setTagInspector(tag -> true);
        return new Yaml(options);
    }
}
