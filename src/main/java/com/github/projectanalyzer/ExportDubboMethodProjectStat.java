package com.github.projectanalyzer;

import com.github.projectanalyzer.util.JavaClassFile;
import com.github.projectanalyzer.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportDubboMethodProjectStat extends ClassProjectStat {

    public ExportDubboMethodProjectStat(String jarFileString, ClassProjectStat importProject) {
        super(jarFileString, importProject);
    }

    public ExportDubboMethodProjectStat(String jarFileString, List<ClassProjectStat> importProject) {
        super(jarFileString, importProject);
    }

    public ExportDubboMethodProjectStat(String jarFileString) {
        super(jarFileString);
    }

    @Override
    public Map<String, Object> groupBy(ClassPool classPool) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (JavaClassFile javaClassFile : classPool.javaProject.values()) {
            if (!Util.isDubboClass(javaClassFile, config.getDubboExportAnnotations())) {
                continue;
            }
            JavaClassFile.Member[] methods = javaClassFile.getMethods();
            for (JavaClassFile.Member method : methods) {
                if (!Util.isDubboMethod(method,classPool)) {
                    continue;
                }
                String mapKey = mapKey(method, javaClassFile, classPool);
                Object reduce = reduce(method, javaClassFile, classPool, map.get(mapKey));
                map.put(mapKey, reduce);
            }
        }
        return map;
    }

    protected String mapKey(JavaClassFile.Member method,
                            JavaClassFile javaClassFile,
                            ClassPool classPool) {
        return "Dubbo";
    }

    protected Object reduce(JavaClassFile.Member method,
                            JavaClassFile javaClassFile,
                            ClassPool classPool,
                            Object beforeValue) {
        Counter counter;
        if (beforeValue != null) {
            counter = (Counter) beforeValue;
        } else {
            counter = new Counter();
        }
        counter.list.add(String.format("%s#%s",
                javaClassFile.getThisClassName(),
                method.getName()
        ));

        return counter;
    }

    public static class Counter {
        final List<String> list = new ArrayList<>();

        @Override
        public String toString() {
            return "Counter{" +
                    "list=" + list.size() +
                    '}';
        }
    }


}
