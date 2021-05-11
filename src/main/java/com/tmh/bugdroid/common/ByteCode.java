package com.tmh.bugdroid.common;

import org.apache.bcel.generic.*;

public class ByteCode {

    public static void printOpCode(Instruction ins, ConstantPoolGen cpg) {

        if (ins instanceof InvokeInstruction) {
            InvokeInstruction invokeIns = (InvokeInstruction) ins;
            System.out.println(formatName(ins) + " " + invokeIns.getClassName(cpg).replaceAll("\\.", "/") + "." + invokeIns.getMethodName(cpg) + invokeIns.getSignature(cpg));
        } else if (ins instanceof LDC) {
            LDC i = (LDC) ins;
            System.out.println(formatName(ins) + " \""+i.getValue(cpg).toString()+"\"");
        } else if (ins instanceof NEW) {
            NEW i = (NEW) ins;
            ObjectType type = i.getLoadClassType(cpg);
            System.out.println(formatName(ins) + " " + type.toString());
        } else if (ins instanceof LoadInstruction) {
            LoadInstruction i = (LoadInstruction) ins;
            System.out.println(formatName(ins) +" "+i.getIndex() + " => [stack]");
        } else if (ins instanceof StoreInstruction) {
            StoreInstruction i = (StoreInstruction) ins;
            System.out.println(formatName(ins) +" (objectref) => "+i.getIndex() + "");
        } else if (ins instanceof FieldInstruction) {
            FieldInstruction i = (FieldInstruction) ins;
            System.out.println(formatName(ins) +" "+i.getFieldName(cpg) + "");
        }  else if (ins instanceof IfInstruction) {
            IfInstruction i = (IfInstruction) ins;
            System.out.println(formatName(ins) +" target => "+i.getTarget().toString()+ "");
        } else if (ins instanceof ICONST) {
            ICONST i = (ICONST) ins;
            System.out.println(formatName(ins) +" "+i.getValue()+" ("+i.getType(cpg)+")");
        } else if (ins instanceof GOTO) {
            GOTO i = (GOTO) ins;
            System.out.println(formatName(ins) +" target => "+i.getTarget().toString());
        } else {
            System.out.println(formatName(ins));
        }
    }

    private static String formatName(Instruction ins) {
        return String.format("%-15s",ins.getName());
    }

    public static <T> T getConstantLDC(InstructionHandle h, ConstantPoolGen cpg, Class<T> clazz) {
        Instruction prevIns = h.getInstruction();
        if (prevIns instanceof LDC) {
            LDC ldcInst = (LDC) prevIns;
            Object val = ldcInst.getValue(cpg);
            if (val.getClass().equals(clazz)) {
                return clazz.cast(val);
            }
        }
        else if(clazz.equals(String.class) && prevIns instanceof INVOKESPECIAL) {
            //This additionnal call allow the support of hardcoded value passed to String constructor
            //new String("HARDCODE")
            INVOKESPECIAL invoke = (INVOKESPECIAL) prevIns;
            if(invoke.getMethodName(cpg).equals("<init>") && invoke.getClassName(cpg).equals("java.lang.String") &&
                    invoke.getSignature(cpg).equals("(Ljava/lang/String;)V")) {
                return getConstantLDC(h.getPrev(), cpg, clazz);
            }
        }

        return null;
    }

    public static Integer getConstantInt(InstructionHandle h) {
        Instruction prevIns = h.getInstruction();
        if (prevIns instanceof ICONST) {
            ICONST ldcCipher = (ICONST) prevIns;
            Number num = ldcCipher.getValue();
            return num.intValue();
        }

        return null;
    }

    public static Number getPushNumber(InstructionHandle h) {
        Instruction prevIns = h.getInstruction();
        if (prevIns instanceof BIPUSH) {
            BIPUSH ldcCipher = (BIPUSH) prevIns;
            return ldcCipher.getValue();
        } else if (prevIns instanceof SIPUSH) {
            SIPUSH ldcCipher = (SIPUSH) prevIns;
            return ldcCipher.getValue();
        }
        return null;
    }

    public static <T> T getPrevInstruction(InstructionHandle startHandle, Class<T> clazz) {
        InstructionHandle curHandle = startHandle;
        while (curHandle != null) {
            curHandle = curHandle.getPrev();

            if (curHandle != null && clazz.isInstance(curHandle.getInstruction())) {
                return clazz.cast(curHandle.getInstruction());
            }
        }
        return null;
    }

}