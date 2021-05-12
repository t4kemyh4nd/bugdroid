package com.tmh.bugdroid.arbitraryaccess;

import java.util.Iterator;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
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

public class CodeExecution implements Detector {

    private static final String CREATES_UNSAFE_PACKAGE_CONTEXT = "CREATES_UNSAFE_PACKAGE_CONTEXT";
    private BugReporter bugReporter;
    private JavaClass originalJc;
    private Method originalMethod;
	private boolean isContextCreated;
	private boolean isClassLoadCalled;

    public CodeExecution(BugReporter bugReporter) {
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
				e.printStackTrace();
			}
        }
    }

    private void analyzeMethod(Method m, ClassContext classContext) throws CFGBuilderException, DataflowAnalysisException, ClassNotFoundException {
    	this.isClassLoadCalled = false;
    	this.isContextCreated = false;
    	if (!classContext.getJavaClass().toString().contains("com.tmh")) {
    		return;
    	}
    	boolean isContextCreated = false;
        boolean isClassLoadCalled = false;
        
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
                if (("createPackageContext".equals(invoke.getMethodName(cpg)))) {
                	isContextCreated = true;
                } else if ("getClassLoader".equals(invoke.getMethodName(cpg)) && "()Landroid/content/Intent;".equals(invoke.getSignature(cpg))) {
                	isClassLoadCalled = true;
                } else {
                	if (invoke.getClassName(cpg) != null) {
                        String className = invoke.getClassName(cpg);
                        className.replace("/", ".");
                        if (!className.startsWith("android") && !className.startsWith("kotlin")) {
    		                JavaClass clazz = Repository.lookupClass(className);
    		                isVulnerable(invoke.getMethodName(cpg), clazz);
                        }
                    }
                }
            } else if (inst instanceof INVOKESTATIC) {
            	InvokeInstruction invoke = (InvokeInstruction) inst;
            	if (invoke.getClassName(cpg) != null) {
                    String className = invoke.getClassName(cpg);
                    className.replace("/", ".");
                    if (!className.startsWith("android") && !className.startsWith("kotlin")) {
		                JavaClass clazz = Repository.lookupClass(className);
		                isVulnerable(invoke.getMethodName(cpg), clazz);
                    }
                }
            }
        }
        
        if (isContextCreated && isClassLoadCalled) {
        	bugReporter.reportBug(new BugInstance(this, CREATES_UNSAFE_PACKAGE_CONTEXT, Priorities.HIGH_PRIORITY) 
                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
        } else if (isContextCreated) {
        	bugReporter.reportBug(new BugInstance(this, CREATES_UNSAFE_PACKAGE_CONTEXT, Priorities.NORMAL_PRIORITY) 
                    .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
        }
    }

	private void isVulnerable(String me, JavaClass clazz) throws ClassNotFoundException {
    	System.out.println("Now looking for method " + me + " in " + clazz.toString().split("\n")[0]);

		if (!clazz.toString().contains("com.tmh")) {
    		return;
    	}
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
	                if(ih.getInstruction() instanceof INVOKEVIRTUAL) 
	                {
	                	InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
	                    
	                	if (("createPackageContext".equals(invoke.getMethodName(cpg)))) {
	                    	isContextCreated = true;
	                    } else if ("getClassLoader".equals(invoke.getMethodName(cpg)) && "()Landroid/content/Intent;".equals(invoke.getSignature(cpg))) {
	                    	isClassLoadCalled = true;
	                    } else {
	                    	if (invoke.getClassName(cpg) != null) {
	                            String className = invoke.getClassName(cpg);
	                            className.replace("/", ".");
	                            if (!className.startsWith("android") && !className.startsWith("kotlin")) {
	        		                JavaClass cl = Repository.lookupClass(className);
	        		                isVulnerable(invoke.getMethodName(cpg), cl);
	                            }
	                        }
	                    } 
	                } else if (ih.getInstruction() instanceof INVOKESTATIC) {
	                	InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
	                	if (invoke.getClassName(cpg) != null) {
	                        String className = invoke.getClassName(cpg);
	                       	className.replace("/", ".");
	                       	if (!className.startsWith("android") && !className.startsWith("kotlin") && !className.startsWith("java")) {
	    		               	JavaClass cl = Repository.lookupClass(className);
	    		               	isVulnerable(invoke.getMethodName(cpg), cl);
	                       	}
	                	}
	                }
	            }
        	}
        	if (isContextCreated && isClassLoadCalled) {
            	bugReporter.reportBug(new BugInstance(this, CREATES_UNSAFE_PACKAGE_CONTEXT, Priorities.HIGH_PRIORITY) 
                        .addClass(this.originalJc).addMethod(this.originalJc, this.originalMethod));
            } else if (isContextCreated) {
            	bugReporter.reportBug(new BugInstance(this, CREATES_UNSAFE_PACKAGE_CONTEXT, Priorities.NORMAL_PRIORITY) 
                        .addClass(this.originalJc).addMethod(this.originalJc, this.originalMethod));
            }
        }
	}

	@Override
	public void report() {
		
	} 
}