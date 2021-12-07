package com.adam.swing_project.library.assertion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class Assert {
    public static void notNull(Object o) {
        if(o == null)
            throw new AssertException("Object is null");
    }
    public static void notNull(Object object, Class<? extends RuntimeException> exceptionClass, String msg) {
        isTrue(object != null, exceptionClass, msg);
    }
    public static void notNull(Object object, String msg) {
        isTrue(object != null, msg);
    }
    public static void isTrue(boolean exp) {
        isTrue(exp, "Expression not true");
    }
    public static void isTrue(boolean exp, String msg) {
        isTrue(exp, AssertException.class, msg);
    }
    public static void isTrue(boolean exp, Class<? extends RuntimeException> exceptionClass, String msg) {
        if(!exp) {
            try {
                Constructor constructor = exceptionClass.getConstructor(String.class);
                RuntimeException exception = (RuntimeException) constructor.newInstance(msg);
                throw exception;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
