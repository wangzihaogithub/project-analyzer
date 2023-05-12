package com.github.projectanalyzer.util;

import com.github.projectanalyzer.ClassProjectStat;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Util {

    public enum Scope {
        className,
        classPath,
        any
    }

    public static boolean isService(String className) {
        return className.endsWith("Service") || className.endsWith("Serivce");
    }

    public static boolean isMybatis(String className, JavaClassFile file,
                                    List<JavaClassFile> projectClass,
                                    ClassProjectStat.ClassPool classPool,
                                    Set<String> keyword) {
        for (String s : className.split("/")) {
            if (keyword.contains(s)) {
                return true;
            }
        }
        String[] thisClassNames = file.getThisClassName().split("/");
        for (String s : thisClassNames) {
            if (keyword.contains(s)) {
                return true;
            }
        }
        return projectClass.stream()
                .anyMatch(e -> e.isInterface()
                        && Util.existKeyword(e, classPool, keyword, Util.Scope.classPath));
    }

    public static boolean existKeyword(JavaClassFile file,
                                       ClassProjectStat.ClassPool classPool,
                                       Collection<String> keywords,
                                       Scope scope
    ) {
        if (file == null) {
            return false;
        }
        String[] interfaceNames = file.getInterfaceNames();
        for (String interfaceName : interfaceNames) {
            String[] split = interfaceName.split("/");
            switch (scope) {
                case any: {
                    for (String s : split) {
                        for (String keyword : keywords) {
                            if (s.equals(keyword)) {
                                return true;
                            }
                        }
                    }
                    break;
                }
                case classPath: {
                    for (int i = 0; i < split.length - 1; i++) {
                        for (String keyword : keywords) {
                            if (split[i].equals(keyword)) {
                                return true;
                            }
                        }
                    }
                    break;
                }
                case className: {
                    for (String keyword : keywords) {
                        if (split[split.length - 1].equals(keyword)) {
                            return true;
                        }
                    }
                    break;
                }
            }
            if (existKeyword(classPool.forName(interfaceName), classPool, keywords, scope)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBizClass(String className, Collection<String> bizList) {
        for (String biz : bizList) {
            if (className.startsWith(biz)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDubboMethod(JavaClassFile.Member method,
                                        ClassProjectStat.ClassPool classPool) {
        if (method.isStatic() || method.isConstructor()) {
            return false;
        }

        JavaClassFile classFile = method.getDeclaringClassFile();
        for (String interfaceName : classFile.getInterfaceNames()) {
            JavaClassFile interfaceClassFile = ClassProjectStat.getJavaClassFile(interfaceName, classPool);
            if (interfaceClassFile == null) {
                return true;
            }
            JavaClassFile.Member[] interfaceMethods = interfaceClassFile.getMethods();
            for (JavaClassFile.Member interfaceMethod : interfaceMethods) {
                if (Objects.equals(interfaceMethod.getName(), method.getName())
                        && Objects.equals(interfaceMethod.getDescriptorName(), method.getDescriptorName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isDubboClass(JavaClassFile javaClassFile, Set<String> annotations) {
        return existAnnotation(javaClassFile.getRuntimeVisibleAnnotations(), annotations);
    }

    public static boolean isHttpMethod(JavaClassFile.Member method, Set<String> annotations) {
        if (method.isStatic() || method.isConstructor()) {
            return false;
        }
        return existAnnotation(method.getRuntimeVisibleAnnotations(), annotations);
//        for (JavaClassFile.Attribute.Annotation annotation : annotations) {
//            for (JavaClassFile.Attribute.Annotation.ElementValue elementValue : annotation.getElementValues()) {
//                if (elementValue instanceof JavaClassFile.Attribute.Annotation.ArrayElementValue) {
//                    JavaClassFile.Attribute.Annotation.ElementValue[] values = ((JavaClassFile.Attribute.Annotation.ArrayElementValue) elementValue).value();
//                    for (JavaClassFile.Attribute.Annotation.ElementValue elementValue1 : values) {
//                        if (elementValue1 instanceof JavaClassFile.Attribute.Annotation.StringElementValue) {
//                            String value = ((JavaClassFile.Attribute.Annotation.StringElementValue) elementValue1).value();
//                        }
//                    }
//                } else if (elementValue instanceof JavaClassFile.Attribute.Annotation.StringElementValue) {
//                    String value = ((JavaClassFile.Attribute.Annotation.StringElementValue) elementValue).value();
//                }
//            }
//        }
    }

    public static boolean isJavaClassName(String className) {
        return className.startsWith("java/")
                || className.startsWith("jdk/")
                || className.startsWith("sun/");
    }

    private static boolean existAnnotation(JavaClassFile.Attribute.Annotation[] annotations,
                                           Collection<String> findAnnotationNames) {
        if (annotations != null) {
            for (JavaClassFile.Attribute.Annotation annotation : annotations) {
                if (findAnnotationNames.contains(annotation.getTypeName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
