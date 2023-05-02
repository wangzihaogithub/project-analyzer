package com.github.projectanalyzer.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.jar.JarEntry;

public class Config {
    private final Set<String> bizPackagePath = new LinkedHashSet<>(Arrays.asList(
//            "com/ig",
//            "com/zhipin"
    ));
    private final Set<String> dubboExportAnnotations = new LinkedHashSet<>(Arrays.asList(
//            "Lcom/ig/annotation/IGService;",
//            "Lcom/ig/annotation/BossZpService;",
            "Lorg/apache/dubbo/config/annotation/DubboService;",
            "Lorg/apache/dubbo/config/annotation/Service;",
            "Lcom/alibaba/dubbo/config/annotation/Service;"
    ));
    private final Set<String> httpExportAnnotations = new LinkedHashSet<>(Arrays.asList(
            "Lorg/springframework/web/bind/annotation/RequestMapping;",
            "Lorg/springframework/web/bind/annotation/PostMapping;",
            "Lorg/springframework/web/bind/annotation/PutMapping;",
            "Lorg/springframework/web/bind/annotation/GetMapping;",
            "Lorg/springframework/web/bind/annotation/DeleteMapping;",
            "Lorg/springframework/web/bind/annotation/PatchMapping;"
    ));
    private Predicate<JarEntry> jarEntryFilter = jarEntry -> true;

    public Config dubboExportAnnotations(String... annotations) {
        dubboExportAnnotations.addAll(Arrays.asList(annotations));
        return this;
    }

    public Config httpExportAnnotations(String... annotations) {
        httpExportAnnotations.addAll(Arrays.asList(annotations));
        return this;
    }

    public Config bizPackagePath(String... paths) {
        bizPackagePath.addAll(Arrays.asList(paths));
        return this;
    }

    public Config jarEntryFilter(Predicate<JarEntry> jarEntryFilter) {
        this.jarEntryFilter = jarEntryFilter;
        return this;
    }

    public Set<String> getBizPackagePath() {
        return bizPackagePath;
    }

    public Set<String> getDubboExportAnnotations() {
        return dubboExportAnnotations;
    }

    public Set<String> getHttpExportAnnotations() {
        return httpExportAnnotations;
    }

    public Predicate<JarEntry> getJarEntryFilter() {
        return jarEntryFilter;
    }
}
