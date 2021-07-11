package com.adam.swing_project.local_file_transfer;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HookCompleteCheck {
    enum CHECK_CONSTANT {
        REQUIRED, IGNORED
    }

    CHECK_CONSTANT value();

}
