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
    protected ClassPool classPool;
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
            Map<String, JavaClassFile> javaProject = getJavaProject(jarFile, jarEntryList);
            classPool = new ClassPool(javaProject, interfaceMap(javaProject), importProject);
        }

        groupBy = groupBy(classPool);
        return this;
    }

    protected Map<String, Object> groupBy(ClassPool classPool) {
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
                                                 ClassPool classPool) {
        JavaClassFile javaClassFile = classPool.javaProject.get(className);
        if (javaClassFile == null) {
            for (ClassProjectStat classProjectStat : classPool.importProject) {
                javaClassFile = getJavaClassFile(className, classProjectStat.classPool);
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

    public static class ClassPool {
        public final Map<String, JavaClassFile> javaProject;
        public final Map<String, List<JavaClassFile>> interfaceMap;
        public final List<ClassProjectStat> importProject;

        public ClassPool(Map<String, JavaClassFile> javaProject, Map<String, List<JavaClassFile>> interfaceMap, List<ClassProjectStat> importProject) {
            this.javaProject = javaProject;
            this.interfaceMap = interfaceMap;
            this.importProject = importProject;
        }

        public JavaClassFile forName(String className) {
            return forName(className, this);
        }

        private static JavaClassFile forName(String className,
                                             ClassPool classPool) {
            JavaClassFile file = classPool.javaProject.get(className);
            if (file != null) {
                return file;
            }
            for (ClassProjectStat projectStat : classPool.importProject) {
                // !!这里认为不会出现循环相互依赖jar包，项目里禁止
                file = forName(className, projectStat.classPool);
                if (file != null) {
                    return file;
                }
            }
            return null;
        }

        public List<JavaClassFile> projectClass(String className) {
            return projectClass(className, this);
        }

        private static List<JavaClassFile> projectClass(String className,
                                                        ClassPool classPool) {
            JavaClassFile file = classPool.javaProject.get(className);
            if (file != null) {
                return Collections.singletonList(file);
            }
            List<JavaClassFile> list = classPool.interfaceMap.get(className);
            if (list != null && list.size() > 0) {
                return list;
            }
            for (ClassProjectStat projectStat : classPool.importProject) {
                // !!这里认为不会出现循环相互依赖jar包，项目里禁止
                List<JavaClassFile> fileList = projectClass(className, projectStat.classPool);
                if (fileList != null) {
                    return fileList;
                }
            }
            return null;
        }
    }

}
