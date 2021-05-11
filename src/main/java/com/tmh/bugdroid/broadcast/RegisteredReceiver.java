package com.tmh.bugdroid.broadcast;

import org.apache.bcel.Const;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class RegisteredReceiver extends OpcodeStackDetector {

    private static final String RECEIVER_REGISTERED = "RECEIVER_REGISTERED";
    private BugReporter bugReporter;

    public RegisteredReceiver(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEVIRTUAL && getNameConstantOperand().equals("registerReceiver") && !getClassConstantOperand().equals("androidx/localbroadcastmanager/content/LocalBroadcastManager")) {
        	if (!getSigConstantOperand().contains("java/lang/String;")) { //checks if receiver has no broadcast permission
            bugReporter.reportBug(new BugInstance(this, RECEIVER_REGISTERED, Priorities.NORMAL_PRIORITY) 
                    .addClass(this).addMethod(this).addSourceLine(this));
        	} else {
        		bugReporter.reportBug(new BugInstance(this, RECEIVER_REGISTERED, Priorities.LOW_PRIORITY) 
                        .addClass(this).addMethod(this).addSourceLine(this));
        	}
        } 
    }
}
