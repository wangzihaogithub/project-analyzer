package com.github.projectanalyzer;

import com.github.projectanalyzer.util.JavaClassFile;
import com.github.projectanalyzer.util.Util;

import java.util.*;

import static com.github.projectanalyzer.util.JavaClassFile.Opcodes.INVOKEINTERFACE;
import static com.github.projectanalyzer.util.JavaClassFile.Opcodes.INVOKEVIRTUAL;

public class ImportInterfaceClassProjectStat extends ClassProjectStat {

    public ImportInterfaceClassProjectStat(String jarFileString, ClassProjectStat importProject) {
        super(jarFileString, importProject);
    }

    public ImportInterfaceClassProjectStat(String jarFileString, List<ClassProjectStat> importProject) {
        super(jarFileString, importProject);
    }

    public ImportInterfaceClassProjectStat(String jarFileString) {
        super(jarFileString);
    }

    public static class Counter {
        final List<String> list = new ArrayList<>();
        final Set<String> set = new LinkedHashSet<>();

        @Override
        public String toString() {
            return "Counter{" +
                    "list=" + list.size() +
                    ", set=" + set.size() +
                    '}';
        }
    }

    @Override
    public Map<String, Object> groupBy(ClassPool classPool) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (JavaClassFile javaClassFile : classPool.javaProject.values()) {
            for (JavaClassFile.Member method : javaClassFile.getMethods()) {
                if (method.isStatic()) {
//                    continue;
                }
                if (method.isConstructor()) {
                    continue;
                }
                JavaClassFile.Opcodes op = method.getOpcodes();
                if (op == null) {
                    continue;
                }
                short[] opcodes = op.getOpcodes();
                for (int i = 0, len = opcodes.length; i < len; i++) {
                    short opcode = opcodes[i];
                    switch (opcode) {
                        case INVOKEVIRTUAL: {
                            int nextOp = i + 1;
                            if (!op.existInt16(nextOp)) {
                                break;
                            }
                            int methodRef = op.readUint16(nextOp);
                            JavaClassFile.ConstantPool constantPool = javaClassFile.getConstantPool();
                            if (!constantPool.exist(methodRef, JavaClassFile.ConstantPool.ConstantMethodRefInfo.class)) {
                                break;
                            }
                            JavaClassFile.ConstantPool.ConstantMemberRefInfo methodRefInfo = constantPool.getConstantMethodRefInfo(methodRef).getMemberRefInfo();
                            String mapKey = mapKey(methodRefInfo, op, method, javaClassFile, classPool, opcode);
                            if (mapKey != null) {
                                Object reduce = reduce(methodRefInfo, op, method, javaClassFile, classPool, map.get(mapKey));
                                map.put(mapKey, reduce);
                            }
                            break;
                        }
                        case INVOKEINTERFACE: {
                            int nextOp = i + 1;
                            if (op.at(nextOp) == INVOKEVIRTUAL) {
                                break;
                            }
                            if (!op.existInt16(nextOp)) {
                                break;
                            }
                            JavaClassFile.ConstantPool constantPool = javaClassFile.getConstantPool();
                            int methodRef = op.readUint16(nextOp);
                            if (!constantPool.exist(methodRef, JavaClassFile.ConstantPool.ConstantInterfaceMethodRefInfo.class)) {
                                break;
                            }

                            JavaClassFile.ConstantPool.ConstantMemberRefInfo methodRefInfo = constantPool.getConstantInterfaceMethodRefInfo(methodRef).getMemberRefInfo();
                            String mapKey = mapKey(methodRefInfo, op, method, javaClassFile, classPool, opcode);
                            if (mapKey != null) {
                                Object reduce = reduce(methodRefInfo, op, method, javaClassFile, classPool, map.get(mapKey));
                                map.put(mapKey, reduce);
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            }
        }
        return map;
    }

    protected String mapKey(JavaClassFile.ConstantPool.ConstantMemberRefInfo memberRefInfo,
                            JavaClassFile.Opcodes op,
                            JavaClassFile.Member method,
                            JavaClassFile javaClassFile,
                            ClassPool classPool,
                            short opcode) {
        switch (opcode) {
            case INVOKEVIRTUAL: {
                String className = memberRefInfo.className();
                if (classPool.projectClass(className) != null) {
                    return null;
                } else if (Util.isJavaClassName(className)) {
                    return null;
                } else if (isBizClass(className)) {
                    return "调用外部方法";
                } else {
                    return null;
                }
            }
            case INVOKEINTERFACE: {
                String className = memberRefInfo.className();
                List<JavaClassFile> projectClass = classPool.projectClass(className);
                if (projectClass != null) {
                    if (Util.isMybatis(className, javaClassFile, projectClass, classPool, config.getDaoPackagePath())) {
                        return "调用数据库";
                    } else if (Util.isService(className)) {
                        return "调用内部服务";
                    } else {
                        return "调用内部接口";
                    }
                } else if (Util.isJavaClassName(className)) {
                    return null;
                } else if (isBizClass(className)) {
                    return "调用外部接口";
                } else {
                    return "调用其他接口";
                }
            }
            default: {
                return "";
            }
        }
    }

    protected Object reduce(JavaClassFile.ConstantPool.ConstantMemberRefInfo interfaceMethodRefInfo,
                            JavaClassFile.Opcodes op,
                            JavaClassFile.Member method,
                            JavaClassFile javaClassFile,
                            ClassPool classPool,
                            Object beforeValue) {
        Counter counter;
        if (beforeValue != null) {
            counter = (Counter) beforeValue;
        } else {
            counter = new Counter();
        }

        counter.list.add(String.format("%s#%s > %s#%s",
                javaClassFile.getThisClassName(),
                method.getName(),
                interfaceMethodRefInfo.className(),
                interfaceMethodRefInfo.nameAndType().getName()
        ));
        counter.set.add(String.format("%s#%s",
                interfaceMethodRefInfo.className(),
                interfaceMethodRefInfo.nameAndType().getName()
        ));
        return counter;
    }

}
