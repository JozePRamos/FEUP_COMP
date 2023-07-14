package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe. comp . jmm .ast. JmmNode ;
import pt.up.fe.comp.jmm.parser.JmmParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SymbolTableMethod {
    private final String signature;
    private final Type returnType;
    private final List<Symbol> parameters;
    private final List<Symbol> localVariables;
    private final Boolean isStatic;

    public SymbolTableMethod(String signature, Type returnType, List<Symbol> parameters, List<Symbol> localVariables, Boolean isStatic) {
        this.signature = signature;
        this.returnType = returnType;
        this.parameters = parameters;
        this.localVariables = localVariables;
        this.isStatic = isStatic;
    }

    public String getSignature() {
        return signature;
    }
    public Type getReturnType() {
        return returnType;
    }
    public List<Symbol> getParameters() {
        return parameters;
    }
    public List<Symbol> getLocalVariables() {
        return localVariables;
    }
    public Boolean getStatic() {
        return isStatic;
    }
}