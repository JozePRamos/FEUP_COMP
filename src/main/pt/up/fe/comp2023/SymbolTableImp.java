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

public class SymbolTableImp implements SymbolTable {


List<String> imports;
String className;

String superClassName;



List<Symbol>fields;

Map<String, SymbolTableMethod> methods;



List<Symbol>localVariables;

List<String> method;




    public SymbolTableImp() {
        this.imports = new ArrayList<>();
        this.fields  = new ArrayList<>();
        this.className = null;
        this.superClassName = null;
        this.method=new ArrayList<>();
        this.methods=new HashMap<>();

    }




    public void addImport(String importSignature) {
        this.imports.add(importSignature);
    }

    public void addMethod(String methodSignature, Type returnType, List<Symbol> params, List<Symbol> localVariables, Boolean isStatic) {
        this.methods.put(methodSignature, new SymbolTableMethod(methodSignature, returnType, params, localVariables, isStatic));
    }






    public void setClassName(String className){
        this.className = className;
    }

    public void setSuper(String superClassName){
        this.superClassName = superClassName;
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }





    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

   @Override
  public List<String> getMethods(){
       for (String methodName : methods.keySet()) {
           method.add(methodName);
       }
       return method;
  }

    @Override
    public Type getReturnType(String s) {

        return methods.get(s).getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodSignature) {
        return this.methods.get(methodSignature).getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String s) {
        return this.methods.get(s).getLocalVariables();
    }


    }




