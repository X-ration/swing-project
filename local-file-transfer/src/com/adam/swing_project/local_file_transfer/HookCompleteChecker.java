package com.adam.swing_project.local_file_transfer;

public class HookCompleteChecker {
    private Object[] targets;
    void register(Object ... objs) {
        targets = objs;
    }
    void checkComplete() {
        for(Object target: targets) {
            Assert.notNull(target, HookIncompleteException.class, "hook incomplete");
        }
    }
}
