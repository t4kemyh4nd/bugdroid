package com.tmh.bugdroid.webview;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

public class JavascriptEnabled extends OpcodeStackDetector {

    private static final String WEBVIEW_JAVASCRIPT_TYPE = "WEB_VIEW_JAVASCRIPT_ENABLED";
    private BugReporter bugReporter;

    public JavascriptEnabled(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEVIRTUAL && getClassConstantOperand().equals("android/webkit/WebSettings") &&
                getNameConstantOperand().equals("setJavaScriptEnabled")) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if(item.getConstant() instanceof Integer && (Integer) item.getConstant() == 1) {  //check if value is true
                bugReporter.reportBug(new BugInstance(this, WEBVIEW_JAVASCRIPT_TYPE, Priorities.LOW_PRIORITY) //
                        .addClass(this).addMethod(this).addSourceLine(this));
            }
        }
    }
}