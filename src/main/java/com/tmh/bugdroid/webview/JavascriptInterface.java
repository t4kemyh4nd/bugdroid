package com.tmh.bugdroid.webview;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;

public class JavascriptInterface extends OpcodeStackDetector {

    private static final String WEBVIEW_JAVASCRIPT_INTERFACE_TYPE = "WEB_VIEW_JAVASCRIPT_INTERFACE_ADDED";
    private BugReporter bugReporter;

    public JavascriptInterface(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.INVOKEVIRTUAL && getClassConstantOperand().equals("android/webkit/WebView") &&
                getNameConstantOperand().equals("addJavascriptInterface")) {
                bugReporter.reportBug(new BugInstance(this, WEBVIEW_JAVASCRIPT_INTERFACE_TYPE, Priorities.LOW_PRIORITY) //
                        .addClass(this).addMethod(this).addSourceLine(this));
        }
    }
}