package com.adam.swing_project.loader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarReadDemo {

    public static void main(String[] args) {
        try {
            JarFile jarFile = new JarFile("D:\\Users\\Adam\\Documents\\Coding\\swing-project\\timer\\release\\timer-v1.2.3-dev.jar");
            Manifest manifest = jarFile.getManifest();
            Attributes manifestMainAttributes = manifest.getMainAttributes();
            outputSectionStart("Manifest Main Attributes");
            Set<Map.Entry<Object, Object>> entrySet = manifestMainAttributes.entrySet();
            Iterator<Map.Entry<Object, Object>> iterator = entrySet.iterator();
            while(iterator.hasNext()) {
                Map.Entry<Object,Object> entry = iterator.next();
                System.out.println("Key=" + entry.getKey() + ",Value=" + entry.getValue());
            }
            outputSectionEnd("Manifest Main Attributes");

            Map<String, Attributes> manifestEntries = manifest.getEntries();
            outputSectionStart("Manifest Entries");
            Iterator<Map.Entry<String, Attributes>> iterator1 = manifestEntries.entrySet().iterator();
            while(iterator1.hasNext()) {
                Map.Entry<String, Attributes> attributesEntry = iterator1.next();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Name:").append(attributesEntry.getKey()).append(" Attributes:");
                Iterator<Map.Entry<Object, Object>> iterator2 = attributesEntry.getValue().entrySet().iterator();
                while(iterator2.hasNext()) {
                    Map.Entry<Object, Object> entry = iterator2.next();
                    stringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
                }
                System.out.println(stringBuilder);
            }
            outputSectionEnd("Manifest Entries");

            Enumeration<JarEntry> jarEntryEnumeration = jarFile.entries();

            outputSectionStart("Jar Entry");
            while(jarEntryEnumeration.hasMoreElements()) {
                JarEntry jarEntry = jarEntryEnumeration.nextElement();
                System.out.println("JarEntry:" + jarEntry.getName());
            }
            outputSectionEnd("Jar Entry");

            ClassLoader myClassLoader = new ClassLoader(JarReadDemo.class.getClassLoader()) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    System.out.println("Invoked loadClass " + name);
                    return super.loadClass(name);
                }

                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    Enumeration<JarEntry> jarEntryEnumeration1 = jarFile.entries();
                    while(jarEntryEnumeration1.hasMoreElements()) {
                        JarEntry jarEntry = jarEntryEnumeration1.nextElement();
                        String entryPath = jarEntry.getName();
                        String expectedPath = name.replaceAll("\\.", "/") + ".class";
                        if(entryPath.equals(expectedPath)) {
                            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                                byte[] classBytes = inputStream.readAllBytes();
                                Class<?> clazz = defineClass(name, classBytes, 0, classBytes.length);
                                System.out.println("Found class " + name + " from jar file");
                                return clazz;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    throw new ClassNotFoundException("Cannot load class " + name);
                }
            };
            Thread.currentThread().setContextClassLoader(myClassLoader);

            try {
                Class<?> clazz = myClassLoader.loadClass("com.adam.swing_project.timer.TimerProgram");
                System.out.println("Class:" + clazz.getName() + " loaded by my classloader");
                Method mainMethod = clazz.getDeclaredMethod("main", String[].class);   //数组类型经调试发现其name为[Ljava.lang.String;
//                mainMethod.invoke(null, (Object) new String[0]);  //静态方法调用不用传obj参数
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
            }
            System.out.println("read complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void outputSection(String sectionName, boolean start) {
        System.out.println("---" + sectionName + " " + (start ? "Start" : "End") + " ----");
    }

    static void outputSectionStart(String sectionName) {
        outputSection(sectionName, true);
    }

    static void outputSectionEnd(String sectionName) {
        outputSection(sectionName, false);
    }

}
