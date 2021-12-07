package com.adam.swing_project.local_file_transfer;

import java.lang.reflect.Field;
import com.adam.swing_project.library.assertion.Assert;

public class HookCompleteChecker {
    private Object target;
    public HookCompleteChecker(Object target) {
        register(target);
    }
    public void register(Object target) {
        this.target = target;
    }
    public void checkComplete() {
        Class targetClass = target.getClass();
        Field[] fields = targetClass.getDeclaredFields();
        for(Field field: fields) {
            field.setAccessible(true);
            HookCompleteCheck hookCompleteCheck = field.getAnnotation(HookCompleteCheck.class);
            if(hookCompleteCheck == null || hookCompleteCheck.value() == HookCompleteCheck.CHECK_CONSTANT.IGNORED) {
                continue;
            }
            try {
                Assert.notNull(field.get(target), HookIncompleteException.class, "目标对象" + target + "的" + field.getName() + "属性为Null");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
