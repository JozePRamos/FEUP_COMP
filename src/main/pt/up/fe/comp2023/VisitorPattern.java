package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

import java.util.*;
;

public class VisitorPattern extends PreorderJmmVisitor<String, Void> {

    SymbolTableImp symbolTableImp;


    public SymbolTableImp getSymbolTable() {
        return symbolTableImp;
    }

    @Override
    protected void buildVisitor() {
        symbolTableImp = new SymbolTableImp();
        addVisit("ImportDeclaration", (node, s) -> this.dealWithImport(node, s));
        addVisit("ClassDeclaration", (node, s) -> this.dealWithClass(node));
        addVisit("VarDeclaration",(node,s)->this.dealWithVar(node));
        addVisit("MethodDeclaration", (node,s)->this.dealWithMethod(node));
        setDefaultValue(() -> null);
    }

     public Type buildType(String typeSignature) {
        if (typeSignature.equals("int[]")) {
            return new Type("int", true);
        } else if (typeSignature.equals("String[]")) {
            return new Type("String", true);
        } else {
            return new Type(typeSignature, false);
        }
    }


    private Void dealWithImport(JmmNode jmmNode, String s) {
        symbolTableImp.addImport( s + "import" + jmmNode.get("name") + ";");
        return null;
    }
    private Void dealWithClass(JmmNode jmmNode) {
        String className = jmmNode.get("name");
        symbolTableImp.setClassName(className);
        if(jmmNode.getOptional("extend").isEmpty()==false){
            symbolTableImp.setSuper(jmmNode.get("extend"));
        };
        return null;
    }

    private Void dealWithVar(JmmNode jmmNode) {
        String name = jmmNode.getJmmParent().getKind();
        if(name.equals("ClassDeclaration")){
            String type = jmmNode.getJmmChild(0).get("value");
            Boolean Array = Boolean.valueOf(jmmNode.getJmmChild(0).get("isArray"));
            symbolTableImp.addField(new Symbol(new Type(type, Array), jmmNode.get("variable")));
        }
        return null;
    }


private Void dealWithMethod(JmmNode jmmNode) {
       String type="";
       String name="";
       Symbol sym;
        Boolean Array=false;
        List<String> namePara=new ArrayList<>();
        String method = jmmNode.getKind();
        List<Type> ret1=new ArrayList<>();
        //Type ret=null;
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> local = new ArrayList<>();
        Boolean Static = false;
        if(method.equals("MethodDeclaration")) {
            if(jmmNode.hasAttribute("nameParameter")){
                JmmNode node = jmmNode.getJmmChild(0);
                name = "main";
                type = node.get("value");
                Array = Boolean.valueOf(node.get("isArray"));
                ret1.add(new Type(type,Array));
               // ret = new Type(type, Array);
                Static = true;
            }
            else{
                name = jmmNode.get("name");
                for (int i = 0; i < jmmNode.getNumChildren(); i++) {
                    JmmNode node = jmmNode.getJmmChild(i);
                    String kind = node.getKind();
                    if (kind.equals("Type") && i == 0) {
                        type = node.get("value");
                        Array = Boolean.valueOf(node.get("isArray"));
                        //ret = new Type(type, Array);
                        ret1.add(new Type(type,Array));
                    } else if (kind.equals("Paremeterlist")) {
                        for (int j = 0; j < node.getNumChildren(); j++) {
                            JmmNode param = node.getJmmChild(j);
                            String typeTemp = param.getJmmChild(0).get("value");
                            Boolean ArrayTemp = Boolean.valueOf(param.getJmmChild(0).get("isArray"));
                            String nameTemp = param.get("nameParameter");
                            Symbol p = new Symbol(new Type(typeTemp, ArrayTemp), nameTemp);
                            parameters.add(p);
                        }
                    }

                }
            }
        }

       symbolTableImp.addMethod(name,ret1.get(0),parameters,local,Static);

        return null;
    }




}
