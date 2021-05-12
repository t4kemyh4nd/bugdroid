package com.tmh.bugdroid.common;

import java.util.Iterator;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;

public class InsecureActivityStart implements Detector {

    private static final String INSECURE_ACTIVITY_START = "INSECURE_ACTIVITY_START";
    private BugReporter bugReporter;
    private boolean isNewIntent = false; 
	private boolean isPackageSet = false;
	boolean isActivityStart = false;

    public InsecureActivityStart(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
    	if (!classContext.getJavaClass().toString().contains("com.tmh.vulnwebview")) {
    		return;
    	}
    	
        JavaClass javaClass = classContext.getJavaClass();

        for (Method m : javaClass.getMethods()) {
            try {
                try {
                	isNewIntent = false; 
                	isPackageSet = false;
                	isActivityStart = false;

					analyzeMethod(m, classContext);
					//System.out.println("Booleans for " + m.toString() + " :" + isNewIntent + isPackageSet + isActivityStart);
					if (isNewIntent && (isPackageSet == false) && isActivityStart) {
						bugReporter.reportBug(new BugInstance(this, INSECURE_ACTIVITY_START, Priorities.HIGH_PRIORITY) 
                                .addClass(classContext.getJavaClass()).addMethod(classContext.getJavaClass(), m));
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
            } catch (CFGBuilderException e) {
				e.printStackTrace();

            } catch (DataflowAnalysisException e) {
				e.printStackTrace();
            }
        }
    }

    private void analyzeMethod(Method m, ClassContext classContext) throws CFGBuilderException, DataflowAnalysisException, ClassNotFoundException {
        MethodGen methodGen = classContext.getMethodGen(m);
        ConstantPoolGen cpg = classContext.getConstantPoolGen();
        CFG cfg = classContext.getCFG(m);

        if (methodGen == null || methodGen.getInstructionList() == null) {
            return;
        }

        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
            Location location = i.next();
            Instruction inst = location.getHandle().getInstruction();
            
            if (inst instanceof NEW) {
                if(((NEW) inst).getLoadClassType(cpg).toString() == "android.content.Intent"); {
                	//System.out.println("Setting isNewIntent to true for " + m.toString());
                	isNewIntent = true;
            	}
            } else if (inst instanceof INVOKESPECIAL) { //for intent with context and className in constructor
            	if (isNewIntent == true) {
	        		InvokeInstruction invoke = (InvokeInstruction) inst;  
	        		System.out.println(inst.toString() + " " + invoke.getClassName(cpg) + " " + invoke.getMethodName(cpg));
	            	if (invoke.getClassName(cpg).equals("android.content.Intent") && invoke.getMethodName(cpg).equals("<init>") && invoke.getSignature(cpg).equals("(Landroid/content/Context;Ljava/lang/Class;)V")) {
	            		isPackageSet = true;
	            	}
            	}
            } else if (inst instanceof INVOKEVIRTUAL) {
            	if (isNewIntent = true) {
                    InvokeInstruction invoke = (InvokeInstruction) inst;     
                	//System.out.println(inst.toString() + " " + invoke.getClassName(cpg) + " " + invoke.getMethodName(cpg));
                    if (invoke.getClassName(cpg) == "android.content.Intent") {
	            		if (invoke.getMethodName(cpg).equals("setClassName") || invoke.getMethodName(cpg).equals("setClass") || invoke.getMethodName(cpg).equals("setComponentName")) {
	            			isPackageSet = true;
	            		}
                    } else if (invoke.getMethodName(cpg).equals("startActivity")) {
            			System.out.println("Setting isActivityStart to true");
            			isActivityStart = true;
            		} else if (invoke.getClassName(cpg) != null && !invoke.getClassName(cpg).contains("android") && !invoke.getClassName(cpg).contains("kotlin") &&
                    		invoke.getSignature(cpg).contains("android/content/Intent")) {
                    	String className = invoke.getClassName(cpg);
                       	className.replace("/", ".");
                    	JavaClass cl = Repository.lookupClass(className);
		               	isVulnerable(invoke.getMethodName(cpg), cl);
                    }
            	}
            } else if (inst instanceof INVOKESTATIC) {
            	if (isNewIntent = true) {
            		InvokeInstruction invoke = (InvokeInstruction) inst;
            		if (invoke.getClassName(cpg) != null && !invoke.getClassName(cpg).contains("android") && !invoke.getClassName(cpg).contains("kotlin") &&
                    		invoke.getSignature(cpg).contains("android/content/Intent")) {
            			String className = invoke.getClassName(cpg);
                       	className.replace("/", ".");
                    	JavaClass cl = Repository.lookupClass(className);
		               	isVulnerable(invoke.getMethodName(cpg), cl);
            		}
            	}
            }
        }
    }
    
    private void isVulnerable(String me, JavaClass clazz) throws CFGBuilderException, DataflowAnalysisException, ClassNotFoundException {
    	//System.out.println("Now looking for method " + me + " in " + clazz.toString().split("\n")[0]);

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
	            	if (ih.getInstruction() instanceof INVOKEVIRTUAL) {
	                	if (isNewIntent = true) {
	                        InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();     
	                        if (invoke.getClassName(cpg).equals("android.content.Intent")) {
	    	            		if (invoke.getMethodName(cpg).equals("setClassName") || invoke.getMethodName(cpg).equals("setClass") || invoke.getMethodName(cpg).equals("setComponentName")) {
	    	                    	System.out.println(ih.getInstruction().toString() + " " + invoke.getClassName(cpg) + " " + invoke.getMethodName(cpg));
	    	            			isPackageSet = true;
	    	            			System.out.println("isPackageSet" + isPackageSet);
	    	            		}
	                        } else if (invoke.getMethodName(cpg).equals("startActivity")) {
	                			//System.out.println("Setting isActivityStart to true");
	                			isActivityStart = true;
	                		} else if (invoke.getClassName(cpg) != null && !invoke.getClassName(cpg).contains("android") && !invoke.getClassName(cpg).contains("kotlin") &&
	                        		invoke.getSignature(cpg).contains("android/content/Intent")) {
	                        	String className = invoke.getClassName(cpg);
	                           	className.replace("/", ".");
	                        	JavaClass cl = Repository.lookupClass(className);
	    		               	isVulnerable(invoke.getMethodName(cpg), cl);
	                        }
	                	}
	                } else if (ih.getInstruction() instanceof INVOKESTATIC) {
	                	if (isNewIntent = true) {
	                		InvokeInstruction invoke = (InvokeInstruction) ih.getInstruction();
	                		if (invoke.getClassName(cpg) != null && !invoke.getClassName(cpg).contains("android") && !invoke.getClassName(cpg).contains("kotlin") &&
	                        		invoke.getSignature(cpg).contains("android/content/Intent")) {
	                			String className = invoke.getClassName(cpg);
	                           	className.replace("/", ".");
	                        	JavaClass cl = Repository.lookupClass(className);
	    		               	isVulnerable(invoke.getMethodName(cpg), cl);
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