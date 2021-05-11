package com.tmh.bugdroid.files;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

public class ExternalFilesDirectory extends OpcodeStackDetector {

    private static final String EXTERNAL_FILES_DIR_USED = "EXTERNAL_FILES_DIR_USED";
    private BugReporter bugReporter;

    public ExternalFilesDirectory(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEVIRTUAL && (
                getNameConstantOperand().equals("getExternalCacheDir") ||
                getNameConstantOperand().equals("getExternalCacheDirs") ||
                getNameConstantOperand().equals("getExternalFilesDir") ||
                getNameConstantOperand().equals("getExternalFilesDirs") ||
                getNameConstantOperand().equals("getExternalMediaDirs")
            )) {
            bugReporter.reportBug(new BugInstance(this, EXTERNAL_FILES_DIR_USED, Priorities.NORMAL_PRIORITY) 
                    .addClass(this).addMethod(this).addSourceLine(this));
        }
        else if(seen == Const.INVOKESTATIC && getClassConstantOperand().equals("android/os/Environment") && (
                getNameConstantOperand().equals("getExternalStorageDirectory") ||
                getNameConstantOperand().equals("getExternalStoragePublicDirectory")
            )) {
            bugReporter.reportBug(new BugInstance(this, EXTERNAL_FILES_DIR_USED, Priorities.NORMAL_PRIORITY) 
                    .addClass(this).addMethod(this).addSourceLine(this));
        }
    }

} 