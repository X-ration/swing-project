package com.adam.swing_project.library.snapshot;

/**
 * 快照相关异常
 */
public class SnapshotException extends RuntimeException {

    public SnapshotException() {
        super();
    }

    public SnapshotException(String msg) {
        super(msg);
    }

    public SnapshotException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public SnapshotException(Throwable cause) {
        super(cause);
    }

}
