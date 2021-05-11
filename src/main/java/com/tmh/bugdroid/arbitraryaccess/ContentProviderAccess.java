package com.tmh.bugdroid.arbitraryaccess;

import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;

public class ContentProviderAccess implements Detector {

    private static final String CONTENT_PROVIDER_ACCESS = "CONTENT_PROVIDER_ACCESS";
    private BugReporter bugReporter;

    public ContentProviderAccess(BugReporter bugReporter) {
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
    	boolean isSetResultWithIntent = false;
        boolean isGetIntentCalled = false;
        
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
                if (("setResult".equals(invoke.getMethodName(cpg)) && "(ILandroid/content/Intent;)V".equals(invoke.getSignature(cpg))) || ("startActivityForResult".equals(invoke.getMethodName(cpg)))) {
                	isSetResultWithIntent = true;
                } else if ("getIntent".equals(invoke.getMethodName(cpg)) && "()Landroid/content/Intent;".equals(invoke.getSignature(cpg))) {
                	isGetIntentCalled = true;
                }
            }
        }
        if (isSetResultWithIntent && isGetIntentCalled) {
        	bugReporter.reportBug(new BugInstance(this, CONTENT_PROVIDER_ACCESS, Priorities.HIGH_PRIORITY) 
                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
        } else if (isSetResultWithIntent) {
        	bugReporter.reportBug(new BugInstance(this, CONTENT_PROVIDER_ACCESS, Priorities.NORMAL_PRIORITY) 
                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
        }
    }

	@Override
	public void report() {
		
	} 
}