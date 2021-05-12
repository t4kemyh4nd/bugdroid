package com.tmh.bugdroid.arbitraryaccess;

import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.CHECKCAST;
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

public class ArbitraryComponents implements Detector {

    private static final String ARBITRARY_COMPONENTS_ACCESS = "ARBITRARY_COMPONENTS_ACCESS";
    private BugReporter bugReporter;

    public ArbitraryComponents(BugReporter bugReporter) {
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
                if (("getParcelableExtra".equals(invoke.getMethodName(cpg)) && "android.content.Intent".equals(invoke.getClassName(cpg)))) {
                	Instruction nextInst = i.next().getHandle().getInstruction();
                	if (nextInst instanceof CHECKCAST) {
                		if(((CHECKCAST) nextInst).getLoadClassType(cpg).toString() == "android.content.Intent"); {
                		bugReporter.reportBug(new BugInstance(this, ARBITRARY_COMPONENTS_ACCESS, Priorities.HIGH_PRIORITY) 
                                .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
                		}
                	}
                }
            }
        }        	
    }

	@Override
	public void report() {
		
	} 
}