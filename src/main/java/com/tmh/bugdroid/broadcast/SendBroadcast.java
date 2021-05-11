package com.tmh.bugdroid.broadcast;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

public class SendBroadcast extends OpcodeStackDetector {

    private static final String BROADCAST_SENT = "BROADCAST_SENT";
    private BugReporter bugReporter;

    public SendBroadcast(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
    	if (seen == Const.INVOKEVIRTUAL && (getNameConstantOperand().equals("sendBroadcast") || getNameConstantOperand().equals("sendStickyBroadcast")) && !getClassConstantOperand().equals("androidx/localbroadcastmanager/content/LocalBroadcastManager")) {
        	if (!getSigConstantOperand().contains("java/lang/String;")) { //checks if broadcast is without broadcast permission
                bugReporter.reportBug(new BugInstance(this, BROADCAST_SENT, Priorities.NORMAL_PRIORITY) 
                        .addClass(this).addMethod(this).addSourceLine(this));
            	} else {
            		bugReporter.reportBug(new BugInstance(this, BROADCAST_SENT, Priorities.LOW_PRIORITY) 
                            .addClass(this).addMethod(this).addSourceLine(this));
            	}
        }
    }
}
