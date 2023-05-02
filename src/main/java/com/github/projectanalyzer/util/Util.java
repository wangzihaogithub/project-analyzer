package com.github.projectanalyzer.util;

import com.github.projectanalyzer.ClassProjectStat;

import java.util.*;

public class Util {

    public static boolean isBizClass(String className, Collection<String> bizList) {
        for (String biz : bizList) {
            if (className.startsWith(biz)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDubboMethod(JavaClassFile.Member method,
                                        Map<String, JavaClassFile> javaProject,
                                        List<ClassProjectStat> importProject) {
        if (method.isStatic() || method.isConstructor()) {
            return false;
        }

        JavaClassFile classFile = method.getDeclaringClassFile();
        for (String interfaceName : classFile.getInterfaceNames()) {
            JavaClassFile interfaceClassFile = ClassProjectStat.getJavaClassFile(interfaceName, javaProject, importProject);
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
