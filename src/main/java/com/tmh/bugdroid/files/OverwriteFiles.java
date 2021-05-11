package com.tmh.bugdroid.files;

import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;

import com.tmh.bugdroid.common.ByteCode;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;


public class OverwriteFiles implements Detector {
    private static final String INTERCEPT_FILE_INTENT = "INTERCEPT_FILE_INTENT";

    private final BugReporter bugReporter;



    public OverwriteFiles(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();

        for (Method m : javaClass.getMethods()) {
            try {
                analyzeMethod(m, classContext);
            } catch (CFGBuilderException e) {
            } catch (DataflowAnalysisException e) {
            }
        }
    }

    private void analyzeMethod(Method m, ClassContext classContext) throws CFGBuilderException, DataflowAnalysisException {
        boolean isStartActivityForResult = false;
        boolean isIntentWithPickUri = false;
        
        MethodGen methodGen = classContext.getMethodGen(m);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        CFG cfg = classContext.getCFG(m);

        if (methodGen == null || methodGen.getInstructionList() == null) {
            return;
        }

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
            Location location = i.next();
            Instruction inst = location.getHandle().getInstruction();
            
            if (inst instanceof INVOKEVIRTUAL) {
                InvokeInstruction invoke = (InvokeInstruction) inst;
                if ("setAction".equals(invoke.getMethodName(cpg))) {
                    LDC loadConst = ByteCode.getPrevInstruction(location.getHandle(), LDC.class);
                    if (loadConst != null) {
                        if ("android.media.action.VIDEO_CAPTURE".equals(loadConst.getValue(cpg)) || "android.media.action.IMAGE_CAPTURE".equals(loadConst.getValue(cpg))
                        	|| "android.media.action.STILL_IMAGE_CAMERA".equals(loadConst.getValue(cpg)) || "android.media.action.VIDEO_CAMERA".equals(loadConst.getValue(cpg))){
                        	isIntentWithPickUri = true;
                        }
                    }
                }
            } else if (inst instanceof INVOKESPECIAL) {
                InvokeInstruction invoke = (InvokeInstruction) inst;
                if ("android/content/Intent".equals(invoke.getClassName(cpg)) && "init".equals(invoke.getMethodName(cpg))) {
                    LDC loadConst = ByteCode.getPrevInstruction(location.getHandle(), LDC.class);
                    if (loadConst != null) {
                        if ("android.media.action.VIDEO_CAPTURE".equals(loadConst.getValue(cpg)) || "android.media.action.IMAGE_CAPTURE".equals(loadConst.getValue(cpg))
                        	|| "android.media.action.STILL_IMAGE_CAMERA".equals(loadConst.getValue(cpg)) || "android.media.action.VIDEO_CAMERA".equals(loadConst.getValue(cpg))){
                        	isIntentWithPickUri = true;
                        }
                    }
                }
            } else if (inst instanceof INVOKEVIRTUAL) {
            	InvokeInstruction invoke = (InvokeInstruction) inst;
            	if ("startActivityForResult".equals(invoke.getMethodName(cpg))) {
            		isStartActivityForResult = true;
            	}
            }
        }
        if (isStartActivityForResult && isIntentWithPickUri) {
        	bugReporter.reportBug(new BugInstance(this, INTERCEPT_FILE_INTENT, Priorities.NORMAL_PRIORITY) 
                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
        } else if ((isStartActivityForResult && !isIntentWithPickUri) || (!isStartActivityForResult && isIntentWithPickUri)) {
        	bugReporter.reportBug(new BugInstance(this, INTERCEPT_FILE_INTENT, Priorities.LOW_PRIORITY) 
                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
        }
    }
    


    @Override
    public void report() {
    }

}