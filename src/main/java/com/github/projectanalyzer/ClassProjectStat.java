package com.github.projectanalyzer;

import com.github.projectanalyzer.util.Config;
import com.github.projectanalyzer.util.JavaClassFile;
import com.github.projectanalyzer.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.IllegalClassFormatException;
import java.util.*;
import java.util.function.Predicate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ClassProjectStat {
    protected Map<String, JavaClassFile> javaProject;
    protected Map<String, List<JavaClassFile>> interfaceMap;
    protected final List<ClassProjectStat> importProject = new ArrayList<>();
    protected Config config = new Config();
    private Map<String, ?> groupBy;
    private String jarFileString;

    public ClassProjectStat(String jarFileString, ClassProjectStat importProject) {
        this(jarFileString, Collections.singletonList(importProject));
    }

    public ClassProjectStat(String jarFileString) {
        this(jarFileString, Collections.emptyList());
    }

    public ClassProjectStat(String jarFileString, List<ClassProjectStat> importProject) {
        this.jarFileString = jarFileString;
        if (importProject != null) {
            for (ClassProjectStat stat : importProject) {
                if (stat != null) {
                    this.importProject.addAll(importProject);
                }
            }
        }
    }

    public ClassProjectStat parse(Config config) throws IllegalClassFormatException, IOException {
        this.config = config;
        try (JarFile jarFile = new JarFile(jarFileString)) {
            List<JarEntry> jarEntryList = getJarEntryList(jarFile);
            javaProject = getJavaProject(jarFile, jarEntryList);
            interfaceMap = interfaceMap(javaProject);
        }
        groupBy = groupBy(javaProject, interfaceMap, importProject);
        return this;
    }

    protected Map<String, Object> groupBy(Map<String, JavaClassFile> javaProject,
                                          Map<String, List<JavaClassFile>> interfaceMap,
                                          List<ClassProjectStat> importProject) {
        return null;
    }

    public void importProject(ClassProjectStat projectStat) {
        importProject.add(projectStat);
    }


    private Map<String, List<JavaClassFile>> interfaceMap(Map<String, JavaClassFile> javaProject) {
        Map<String, List<JavaClassFile>> interfaceMap = new LinkedHashMap<>();
        for (Map.Entry<String, JavaClassFile> entry : javaProject.entrySet()) {
            JavaClassFile value = entry.getValue();
            String[] interfaceNames = value.getInterfaceNames();
            for (String interfaceName : interfaceNames) {
                if (isBizClass(interfaceName)) {
                    interfaceMap.computeIfAbsent(interfaceName, e -> new ArrayList<>())
                            .add(value);
                }
            }
        }
        return interfaceMap;
    }

    protected boolean isBizClass(String className) {
        return Util.isBizClass(className, config.getBizPackagePath());
    }

    private static Map<String, JavaClassFile> getJavaProject(JarFile jarFile, List<JarEntry> jarEntryList) throws IllegalClassFormatException, IOException {
        Map<String, JavaClassFile> map = new LinkedHashMap<>();
        for (JarEntry jarEntry : jarEntryList) {
            if (!isClassFile(jarEntry)) {
                continue;
            }
            InputStream inputStream = jarFile.getInputStream(jarEntry);
            JavaClassFile javaClassFile = new JavaClassFile(inputStream);
            map.put(javaClassFile.getThisClassName(), javaClassFile);
        }
        return map;
    }

    private List<JarEntry> getJarEntryList(JarFile jarFile) throws IOException {
        List<JarEntry> list = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        Manifest manifest = jarFile.getManifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        String springBootClasses = mainAttributes.getValue("Spring-Boot-Classes");
        Predicate<JarEntry> filter = config.getJarEntryFilter();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            if (jarEntry.isDirectory()) {
                continue;
            }
            String name = jarEntry.getName();
            if (springBootClasses != null && !name.startsWith(springBootClasses)) {
                continue;
            }
            if (filter != null) {
                if (filter.test(jarEntry)) {
                    list.add(jarEntry);
                }
            } else {
                list.add(jarEntry);
            }
        }
        return list;
    }

    public static JavaClassFile getJavaClassFile(String className,
                                                 Map<String, JavaClassFile> javaProject,
                                                 List<ClassProjectStat> importProject) {
        JavaClassFile javaClassFile = javaProject.get(className);
        if (javaClassFile == null) {
            for (ClassProjectStat classProjectStat : importProject) {
                javaClassFile = getJavaClassFile(className, classProjectStat.javaProject, classProjectStat.importProject);
                if (javaClassFile != null) {
                    break;
                }
            }
        }
        return javaClassFile;
    }

    private static boolean isClassFile(JarEntry jarEntry) {
        return jarEntry.getName().endsWith(".class");
    }
}
