package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Jasmin implements JasminBackend {
    private ClassUnit ollirClass;
    private int maxStackSize;
    private int currStackSize;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        ollirClass = ollirResult.getOllirClass();
        List<Report> reports = new ArrayList<>();

        String classCode = getClassCode(ollirResult.getOllirClass());

        Path path = Paths.get("C:\\Users\\home\\Desktop\\" + ollirClass.getClassName() + ".j");

        try {
            Files.writeString(path, classCode, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.out.print("Invalid Path");
        }

        return new JasminResult(ollirResult, classCode, reports);
    }

    private String getClassCode(ClassUnit ollirClass) {
        String className = ollirClass.getClassName();
        String superClass = ollirClass.getSuperClass();
        if (superClass == null) superClass = "java/lang/Object";

        return ""
                + getHeaderCode(className, superClass)
                + getFieldsCode(ollirClass)
                + getDefaultConstructorCode(superClass)
                + getMethodsCode(ollirClass)
                ;
    }

    private String  getHeaderCode(String className, String superClass) {
        return ""
                + ".class public " + className + "\n"
                + ".super " + superClass + "\n"
                + "\n"
        ;
    }

    private String getFieldsCode(ClassUnit ollirClass) {
        if (ollirClass.getFields().isEmpty()) return "";

        StringBuilder fieldsCodeBuilder = new StringBuilder();

        for (Field field : ollirClass.getFields()) {
            fieldsCodeBuilder.append(getFieldCode(field));
        }
        fieldsCodeBuilder.append("\n");

        return fieldsCodeBuilder.toString();
    }

    private String getFieldCode(Field field) {
        StringBuilder fieldCodeBuilder = new StringBuilder();

        String fieldName = field.getFieldName();
        if (fieldName.equals("field")) {
            fieldName = "'field";
        }
        fieldCodeBuilder
                .append(".field ")
                .append(switch (field.getFieldAccessModifier()) {
                    case PUBLIC -> "public";
                    case PRIVATE, DEFAULT -> "private";
                    case PROTECTED -> "protected";
        })
                .append(" ");

        if (field.isStaticField()) fieldCodeBuilder.append("static");
        if (field.isFinalField()) fieldCodeBuilder.append("final");

        fieldCodeBuilder.append(fieldName).append(" ");

        fieldCodeBuilder.append(getTypeCode(field.getFieldType())).append(" ");

        if (field.isInitialized()){
            fieldCodeBuilder.append(" = ");
            fieldCodeBuilder.append(field.getInitialValue());
        }

        fieldCodeBuilder.append("\n");

        return fieldCodeBuilder.toString();
    }

    private String getDefaultConstructorCode(String superClass) {
        return ""
                + ".method public <init>()V\n"
                + "\taload_0\n"
                + "\tinvokespecial " + superClass+  "/<init>()V\n"
                + "\treturn\n"
                + ".end method\n"
                + "\n"
        ;
    }

    private String getMethodsCode(ClassUnit ollirClass) {
        StringBuilder methodsCodeBuilder = new StringBuilder();

        for (Method method : ollirClass.getMethods()) {
            if (!method.isConstructMethod()) methodsCodeBuilder.append(getMethodCode(method));
        }
        return methodsCodeBuilder.toString();
    }

    private String getMethodHeaderMainCode() {
        return ".method public static main([Ljava/lang/String;)V\n";
    }

    private String getMethodHeaderCode(Method method) {
        StringBuilder methodHeaderCodeBuilder = new StringBuilder();

        methodHeaderCodeBuilder.append(".method public ");
        if (method.isStaticMethod()) {
            methodHeaderCodeBuilder.append("static ");
        }
        methodHeaderCodeBuilder.append(method.getMethodName()).append("(");
        for (Element param : method.getParams()) {
            methodHeaderCodeBuilder.append(getTypeCode(param.getType()));
        }
        methodHeaderCodeBuilder
                .append(")")
                .append(getTypeCode(method.getReturnType()))
                .append("\n")
        ;
        return methodHeaderCodeBuilder.toString();
    }

    private String getMethodCode(Method method) {
        maxStackSize = 0;
        currStackSize = 0;

        StringBuilder methodCodeBuilder = new StringBuilder();

        String instructionsCode = getInstructionsCode(method);

        methodCodeBuilder
                .append(method.getMethodName().equals("main") ?
                                getMethodHeaderMainCode() :
                                getMethodHeaderCode(method))
                .append(getLimitsCode(method))
                .append(instructionsCode)
        ;

        methodCodeBuilder.append(".end method\n\n");

        return methodCodeBuilder.toString();
    }

    private String getLimitsCode(Method method) {
        int localsCount = method.getVarTable().size();
        if (!method.isStaticMethod()) localsCount++;
        return ""
                + ".limit locals " + localsCount + "\n"
                + ".limit stack " + maxStackSize + "\n"
        ;
    }

    private String getInstructionsCode(Method method) {
        StringBuilder instructionsCodeBuilder = new StringBuilder();

        for (Instruction instr : method.getInstructions()) {
            instructionsCodeBuilder.append(getInstructionCode(instr, method.getVarTable(), method.getLabels()));
        }
        return instructionsCodeBuilder.toString();
    }

    private String getInstructionCode(Instruction instr, HashMap<String, Descriptor> varTable, HashMap<String, Instruction> labels) {
        StringBuilder instructionCodeBuilder = new StringBuilder();
        for (Map.Entry<String, Instruction> entry : labels.entrySet()) {
            if (entry.getValue().equals(instr)){
                instructionCodeBuilder.append(entry.getKey()).append(":\n");
            }
        }

        String instrCode = switch (instr.getInstType()) {
            case ASSIGN -> getAssignInstrCode((AssignInstruction) instr, varTable);
            case BINARYOPER -> getBinaryInstrCode((BinaryOpInstruction) instr, varTable);
            case CALL -> getCallInstrCode((CallInstruction) instr, varTable);
            case BRANCH -> getBranchInstrCode((CondBranchInstruction) instr, varTable);
            case RETURN -> getReturnInstrCode((ReturnInstruction) instr, varTable);
            case NOPER -> getSingleInstrCode((SingleOpInstruction) instr, varTable);
            case PUTFIELD -> getPutFieldInstrCode((PutFieldInstruction) instr, varTable);
            case GETFIELD -> getGetFieldInstrCode((GetFieldInstruction) instr, varTable);
            default -> throw new IllegalStateException("Unexpected value: " + instr.getInstType());
        };
        return instructionCodeBuilder.append(instrCode).toString();
    }

    private String getGetFieldInstrCode(GetFieldInstruction instr, HashMap<String, Descriptor> varTable) {
        StringBuilder getFieldInstrCodeBuilder = new StringBuilder();

        getFieldInstrCodeBuilder.append(getLoadCode(instr.getFirstOperand(), varTable));

        Operand var = (Operand)instr.getSecondOperand();
        getFieldInstrCodeBuilder
                .append("\tgetfield ")
                .append(ollirClass.getClassName())
                .append("/")
                .append(var.getName())
                .append(" ")
                .append(getTypeCode(var.getType()))
                .append("\n")
        ;
        return getFieldInstrCodeBuilder.toString();
    }

    private String getPutFieldInstrCode(PutFieldInstruction instr, HashMap<String, Descriptor> varTable) {
        StringBuilder putFieldCodeBuilder = new StringBuilder();

        putFieldCodeBuilder.append(getLoadCode(instr.getFirstOperand(), varTable));
        putFieldCodeBuilder.append(getLoadCode(instr.getThirdOperand(), varTable));

        subFromCurrStackSize(2);

        Operand var = (Operand)instr.getSecondOperand();
        putFieldCodeBuilder
                .append("\tputfield ")
                .append(ollirClass.getClassName())
                .append("/")
                .append(var.getName())
                .append(" ")
                .append(getTypeCode(var.getType()))
                .append("\n")
        ;

        return putFieldCodeBuilder.toString();
    }

    private String getSingleInstrCode(SingleOpInstruction instr, HashMap<String, Descriptor> varTable) {
        return getLoadCode(instr.getSingleOperand(), varTable);
    }

    private String getCallInstrCode(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        CallType callType = instruction.getInvocationType();

        return switch (callType) {
            case invokespecial, invokevirtual -> getInvokeCode(instruction, varTable, callType, ((ClassType) instruction.getFirstArg().getType()).getName());
            case invokestatic -> getInvokeCode(instruction, varTable, callType, ((Operand) instruction.getFirstArg()).getName());
            case NEW -> getNewObjectCode(instruction, varTable);
            default -> throw new IllegalStateException("Unexpected value: " + callType);
        };
    }

    private String getNewObjectCode(CallInstruction instruction, HashMap<String, Descriptor> varTable) {
        Element el = instruction.getFirstArg();
        StringBuilder newObjectCodeBuilder = new StringBuilder();

        if (el.getType().getTypeOfElement().equals(ElementType.ARRAYREF)) {
            newObjectCodeBuilder.append(getLoadCode(instruction.getListOfOperands().get(0), varTable));
            newObjectCodeBuilder.append("\tnewarray int\n");
        }
        else if (el.getType().getTypeOfElement().equals(ElementType.OBJECTREF)){
            subFromCurrStackSize(2);
            newObjectCodeBuilder
                    .append("\tnew ")
                    .append(getObjectName(((Operand) el).getName()))
                    .append("\n")
                    .append("\tdup\n")
            ;
        }

        return newObjectCodeBuilder.toString();
    }

    private String getReturnInstrCode(ReturnInstruction instr, HashMap<String, Descriptor> varTable) {
        if(!instr.hasReturnValue()) return "\treturn\n";

        StringBuilder returnInstrCodeBuilder = new StringBuilder();

        switch (instr.getOperand().getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> {
                returnInstrCodeBuilder.append(getLoadCode(instr.getOperand(), varTable));

                subFromCurrStackSize(1);
                returnInstrCodeBuilder.append("\tireturn");
            }
            case ARRAYREF, OBJECTREF -> {
                returnInstrCodeBuilder.append(getLoadCode(instr.getOperand(), varTable));

                subFromCurrStackSize(1);
                returnInstrCodeBuilder.append("\tareturn");
            }
        }

        return returnInstrCodeBuilder.append("\n").toString();
    }

    private String getBranchInstrCode(CondBranchInstruction instr, HashMap<String, Descriptor> varTable) {
        return "BRANCH CODE NOT IMPLEMENTED\n";
    }

    private String getInvokeCode(CallInstruction instruction, HashMap<String, Descriptor> varTable, CallType callType, String name) {
        StringBuilder invokeCodeBuilder = new StringBuilder();

        String functionLiteral = ((LiteralElement) instruction.getSecondArg()).getLiteral();
        StringBuilder arguments = new StringBuilder();

        if (!functionLiteral.equals("\"<init>\"")) {
            invokeCodeBuilder.append(this.getLoadCode(instruction.getFirstArg(), varTable));
        }

        int numArgs = 0;
        for (Element element : instruction.getListOfOperands()) {
            invokeCodeBuilder.append(getLoadCode(element, varTable));
            arguments.append(getTypeCode(element.getType()));
            numArgs++;
        }

        if (!instruction.getInvocationType().equals(CallType.invokestatic)) {
            numArgs += 1;
        }
        this.subFromCurrStackSize(numArgs);
        if (instruction.getReturnType().getTypeOfElement() != ElementType.VOID) {
            this.addToCurrStackSize(1);
        }

        invokeCodeBuilder
                .append("\t")
                .append(callType.name())
                .append(" ")
                .append(getObjectName(name))
                .append(".").append(functionLiteral.replace("\"", ""))
                .append("(").append(arguments)
                .append(")").
                append(getTypeCode(instruction.getReturnType()))
                .append("\n");

        if (functionLiteral.equals("\"<init>\"") && !ollirClass.getClassName().equals("this")) {
            invokeCodeBuilder.append(getStoreCode((Operand) instruction.getFirstArg(), varTable));
        }

        return invokeCodeBuilder.toString();
    }

    private String getObjectName(String name) {
        for (String imp : ollirClass.getImports()) {
            if (imp.endsWith("." + name)) {
                return imp.replaceAll("\\.", "/");
            }
        }
        return name;
    }


    private String getStoreCode(Operand operand, HashMap<String, Descriptor> varTable) {
        if (operand instanceof ArrayOperand) {
            subFromCurrStackSize(3);
            return "\tiastore\n";
        }

        String prefix = switch(operand.getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> "i";
            case OBJECTREF, ARRAYREF -> "a";
            default -> throw new IllegalStateException("Unexpected value: " + operand.getType().getTypeOfElement());
        };

        return "\t" + prefix + "store" + getVirtualRegCode(operand.getName(), varTable) + "\n";

    }

    private String getBinaryInstrCode(BinaryOpInstruction instr, HashMap<String, Descriptor> varTable) {
        return switch (instr.getOperation().getOpType()) {
            case ADD, SUB, MUL, DIV -> getBinaryArithmeticInstructionCode(instr, varTable);
            case LTH, GTE, ANDB, NOTB -> getBinaryBooleanInstructionCode(instr, varTable);
            default -> throw new IllegalStateException("Unexpected value: " + instr.getOperation().getOpType());
        };
    }

    private String getBinaryBooleanInstructionCode(BinaryOpInstruction instr, HashMap<String, Descriptor> varTable) {
        return "BINARY BOOLEAN CODE NOT IMPLEMENTED\n";
    }

    private String getBinaryArithmeticInstructionCode(BinaryOpInstruction instr, HashMap<String, Descriptor> varTable) {
        String leftOperand = getLoadCode(instr.getLeftOperand(), varTable);
        String rightOperand = getLoadCode(instr.getRightOperand(), varTable);
        String operation;

        operation = switch (instr.getOperation().getOpType()) {
            case ADD -> "add";
            case SUB -> "sub";
            case MUL -> "mul";
            case DIV -> "div";
            default -> throw new IllegalStateException("Unexpected value: " + instr.getOperation().getOpType());
        };

        subFromCurrStackSize(1);
        return leftOperand + rightOperand + "\t" + "i" + operation + "\n";
    }

    private String getLoadCode(Element element, HashMap<String, Descriptor> varTable) {
        if (element instanceof LiteralElement) {
            String number = ((LiteralElement) element).getLiteral();
            addToCurrStackSize(1);
            return "\t" + getIntElementCode(number) + "\n";
        } else if (element instanceof ArrayOperand operand) {

            StringBuilder loadArrayCodeBuilder = new StringBuilder();
            loadArrayCodeBuilder.append("aload").append(getVirtualRegCode(operand.getName(), varTable)).append("\n");
            addToCurrStackSize(1);

            loadArrayCodeBuilder.append(getLoadCode(operand.getIndexOperands().get(0), varTable));

            subFromCurrStackSize(1);
            loadArrayCodeBuilder.append("iaload\n");
            return loadArrayCodeBuilder.toString();
        }
        else if (element instanceof Operand operand) {
            addToCurrStackSize(1);
            return "\t" + switch (operand.getType().getTypeOfElement()) {
                case THIS -> "aload_0";
                case INT32, BOOLEAN -> "iload" + getVirtualRegCode(operand.getName(), varTable);
                case OBJECTREF, ARRAYREF -> "aload" + getVirtualRegCode(operand.getName(), varTable);
                case CLASS -> "";
                default -> throw new IllegalStateException("Unexpected value: " + operand.getType().getTypeOfElement());
            } + "\n";
        }
        return "";
    }

    private String getAssignInstrCode(AssignInstruction instr, HashMap<String, Descriptor> varTable) {
        StringBuilder assignInstrCodeBuilder = new StringBuilder();
        Operand operand = (Operand) instr.getDest();
        if (operand instanceof ArrayOperand arrayOperand) {
            assignInstrCodeBuilder
                    .append("aload")
                    .append(getVirtualRegCode(arrayOperand.getName(), varTable))
                    .append("\n");
            addToCurrStackSize(1);

            assignInstrCodeBuilder.append(getLoadCode(arrayOperand.getIndexOperands().get(0), varTable));
        }

        assignInstrCodeBuilder.append(getInstructionCode(instr.getRhs(), varTable, new HashMap<>()));
        if (!(operand.getType().getTypeOfElement().equals(ElementType.OBJECTREF) && instr.getRhs() instanceof CallInstruction)) {
            assignInstrCodeBuilder.append(getStoreCode(operand, varTable));
        }

        return assignInstrCodeBuilder.toString();
    }

    private String getVirtualRegCode(String name, HashMap<String, Descriptor> varTable) {
        int virtualReg = varTable.get(name).getVirtualReg();
        if (virtualReg > 3) return " " + virtualReg;
        return "_" + virtualReg;
    }

    private String getIntElementCode(String elem) {
        int value = Integer.parseInt(elem);
        if (value == -1) return "iconst_m1";
        else if (     0 <= value && value <=     5) return "iconst_" + value;
        else if (  -128 <= value && value <=   127) return "bipush " + value;
        else if (-32768 <= value && value <= 32767) return "sipush " + value;
        return "ldc " + value;
    }

    private String getTypeCode(Type type) {
        return switch (type.getTypeOfElement()) {
            case ARRAYREF -> "[I";
            case INT32 -> "I";
            case BOOLEAN -> "Z";
            //case CLASS, OBJECTREF -> "L" + ((ClassType) type).getName() + ";";
            case STRING -> "Ljava/lang/String;";
            case VOID -> "V";
            default -> throw new IllegalStateException("Unexpected value: " + type.getTypeOfElement());
        };
    }

    private void addToCurrStackSize(int value) {
        currStackSize += value;
        maxStackSize = Math.max(currStackSize, maxStackSize);
    }

    private void subFromCurrStackSize(int value) {
        currStackSize -= value;
    }
}
