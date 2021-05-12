package com.tmh.bugdroid.arbitraryaccess;

import java.util.Iterator;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
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

public class InstallApk implements Detector {

    private static final String INSTALL_ARBITRARY_APK = "INSTALL_ARBITRARY_APK";
    private BugReporter bugReporter;
    private JavaClass originalJc;
    private Method originalMethod;

    public InstallApk(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        this.originalJc = javaClass;

        for (Method m : javaClass.getMethods()) {
            try {
                analyzeMethod(m, classContext);
            } catch (CFGBuilderException e) {
            } catch (DataflowAnalysisException e) {
            } catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
    }

    private void analyzeMethod(Method m, ClassContext classContext) throws CFGBuilderException, DataflowAnalysisException, ClassNotFoundException {
    	this.originalMethod = m;
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
                
                if ("setDataAndType".equals(invoke.getMethodName(cpg)) || "setType".equals(invoke.getMethodName(cpg))) {
                	LDC loadConst = ByteCode.getPrevInstruction(location.getHandle(), LDC.class);
                    if (loadConst != null) {
                        if ("application/vnd.android.package-archive".equals(loadConst.getValue(cpg))){                      
                        	bugReporter.reportBug(new BugInstance(this, INSTALL_ARBITRARY_APK, Priorities.LOW_PRIORITY) 
                                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
                        }
                    }
                }
            } else if (inst instanceof INVOKESTATIC) {
            	InvokeInstruction invoke = (InvokeInstruction) inst;
            	if (invoke.getClassName(cpg) != null) {
                    String className = invoke.getClassName(cpg);
                    if (!className.startsWith("android") && !className.startsWith("kotlin")) {
		                JavaClass clazz = Repository.lookupClass(className.replace("/", "."));
		                isSharedPrefsMethod(invoke.getMethodName(cpg), clazz);
                    	}
                }
            }
        }
    }
    
    private void isSharedPrefsMethod(String me, JavaClass clazz) throws CFGBuilderException, DataflowAnalysisException, ClassNotFoundException {
    	if (!clazz.toString().contains("com.tmh")) {
    		return;
    	}
    	System.out.println("Now looking for method " + me + " in " + clazz.toString().split("\n")[0]);
    	ConstantPool constantPool = clazz.getConstantPool();
        Method [] method=clazz.getMethods();
        
        ConstantPoolGen cpg = new ConstantPoolGen(constantPool);
        for(Method m : method)
        {
        	if (m.getName().contains(me)) {
        		System.out.println("Now scanning " + m.getName());
	            MethodGen mg = new MethodGen(m, m.getName(), cpg);
	            for(InstructionHandle ih = mg.getInstructionList().getStart(); 
	                    ih != null; ih = ih.getNext())
	            {
	            	if (ih.getInstruction() instanceof INVOKEVIRTUAL) {
	                    InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
	                    
	                    if ("setDataAndType".equals(invoke.getMethodName(cpg)) || "setType".equals(invoke.getMethodName(cpg))) {
	                    	LDC loadConst = ByteCode.getPrevInstruction(ih, LDC.class);
	                        if (loadConst != null) {
	                            if ("application/vnd.android.package-archive".equals(loadConst.getValue(cpg))){                      
	                            	bugReporter.reportBug(new BugInstance(this, INSTALL_ARBITRARY_APK, Priorities.LOW_PRIORITY) 
	                                        .addClass(this.originalJc).addMethod(this.originalJc, this.originalMethod));
	                            }
	                        }
	                    }
	                } else if (ih.getInstruction() instanceof INVOKESTATIC) {
	                	InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
	                	if (invoke.getClassName(cpg) != null) {
	                        String className = invoke.getClassName(cpg);
	                       	className.replace("/", ".");
	                       	if (!className.startsWith("android") && !className.startsWith("kotlin") && !className.startsWith("java")) {
	    		               	JavaClass cl = Repository.lookupClass(className.replace("/", "."));
	    		               	isSharedPrefsMethod(invoke.getMethodName(cpg), cl);
	                       	}     
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