package pt.up.fe.comp2023;

import pt.up.fe. comp . jmm .ast. AJmmVisitor ;
import pt.up.fe. comp . jmm .ast. JmmNode ;

import java.util.function.BiFunction;

public class OllirVisitor extends AJmmVisitor < String , Void > {

    int whileInt = 0;
    int ifInt = 0;
    public String getString() {
        return string;
    }

    private String string;

    @Override
    protected void buildVisitor() {
        string = new String();
        addVisit("Program", (node, s) -> this.dealWithProgram(node));
        addVisit("ImportDeclaration", (node, s) -> this.dealWithImport(node));
        addVisit("ClassDeclaration", (node, s) -> this.dealWithClass(node));
        addVisit("VarDeclaration", (node, s) -> this.dealWithVar(node));
        addVisit("MethodDeclaration", (node,s)->this.dealWithMethod(node));
        addVisit("Statement",(node,s)->this.dealStatement(node));
        addVisit("VarDecl",(node,s)->this.dealWithVarDecl(node));
        addVisit("WhileStat",(node,s)->this.dealWithWhile(node));
        addVisit("IfStat",(node,s)->this.dealWithIf(node));
        addVisit("Array",(node,s)->this.dealWithArray(node));
        setDefaultVisit();
    }

    private Void dealWithArray(JmmNode node) {
        JmmNode ins = null;
        JmmNode RParent = node.getJmmParent();
        while (!RParent.getKind().equals("MethodDeclaration"))
            RParent = RParent.getJmmParent();
        for (JmmNode i : RParent.getChildren()){
            if (i.getKind().equals("VarDeclaration")) {
                if (i.get("variable").equals(node.getJmmChild(0).get("value"))) {
                    ins = i;
                    break;
                }
            }
            else if (i.getKind().equals("Paremeterlist")){
                if (i.getNumChildren() > 0) {
                    for (JmmNode temp : i.getChildren()) {
                        if (temp.get("nameParameter").equals(node.getJmmChild(0).get("value"))) {
                            ins = temp;
                            break;
                        }
                    }
                }
            }
        }
        string += node.getJmmChild(0).get("value")+ "[" + node.getJmmChild(1).get("value") + ".i32].";
        varCode(ins);
        string += " :=.";
        varCode(ins);
        string += " " + node.getJmmChild(2).get("value") + ".";
        varCode(ins);
        string += ";\n";

        return null;
    }

    private Void dealWithIf(JmmNode node) {
        String temp;
        boolean id = false;
        if (node.getJmmChild(0).getKind().equals("ID")) {
            temp = ".bool";
            id = true;
        }
        else if (node.getJmmChild(0).get("op").equals("<") || node.getJmmChild(0).get("op").equals(">")
                || node.getJmmChild(0).get("op").equals("=="))
            temp = ".i32";
        else
            temp = ".bool";
        if (id){
            string += "if (" + node.getJmmChild(0).get("value") + temp;
        }
        else {
            resolveIF(node.getJmmChild(0), temp);
            string += "if (t1.bool";

        }
            string +=  ") goto if" + ifInt + ";\ngoto else" + ifInt + ";\nif" + ifInt + ":\n";
        for (JmmNode i : node.getJmmChild(1).getChildren()) {
            visit(i);
        }
        string += "goto endif" + ifInt + ";\nelse" + ifInt + ":\n";
        for (JmmNode i : node.getJmmChild(2).getChildren()) {
            visit(i);
        }
        string += "endif" + ifInt + ":\n";
        ifInt++;
        return null;
    }

    private void resolveIF(JmmNode node, String temp){
        if (node.hasAttribute("op")){
            if (node.get("op").equals("(")) {
                resolveIF(node.getJmmChild(0), temp);
            }
            else if (node.get("op").equals("!")) {
                if (node.getNumChildren() > 0){
                    resolveIF(node.getJmmChild(0), temp);
                }
                string += "t1.bool :=.bool " + node.get("op") + ".bool t1.bool;\n";
            } else {
                if (node.getNumChildren() > 0){
                    resolveIF(node.getJmmChild(0), temp);
                }
                string += "t1.bool :=.bool " +  node.getJmmChild(0).get("value") + temp + " " + node.get("op") + ".bool"
                        + " " + node.getJmmChild(1).get("value") + temp + ";\n";
            }
        }
    }

    private Void dealWithWhile(JmmNode node) {
        String temp;
        if (node.getJmmChild(0).get("op").equals("<") || node.getJmmChild(0).get("op").equals(">")
                || node.getJmmChild(0).get("op").equals("=="))
            temp = ".i32";
        else
            temp = ".bool";
        string += "while" + whileInt + ":\n";
        resolveIF(node.getJmmChild(0),temp);
        string +="if (t1.bool) goto t" + whileInt + ";\ngoto endwhile" + whileInt + ";\nt" + whileInt + ":\n";
        for (JmmNode i : node.getJmmChild(1).getChildren()) {
            visit(i);
        }
        string += "goto while" + whileInt + ";\nendwhile" + whileInt + ":\n";
        whileInt++;
        return null;
    }

    private Void dealWithVarDecl(JmmNode node) {
        JmmNode temp = node.getJmmChild(0);
        JmmNode ins = null;
        JmmNode RParent = node.getJmmParent();
        while (!RParent.getKind().equals("MethodDeclaration"))
            RParent = RParent.getJmmParent();
        for (JmmNode i : RParent.getChildren()) {
            if (i.getKind().equals("VarDeclaration")) {
                if (i.get("variable").equals(temp.get("value"))) {
                    ins = i;
                }
            }
            else if (i.getKind().equals("Paremeterlist")){
                for(JmmNode a : i.getChildren()){
                   if( a.get("nameParameter").equals(temp.get("value"))){
                       ins=a;
                   }
                }
            }
        }
        if (ins == null){
            for (JmmNode i : RParent.getJmmParent().getChildren()){
                if (i.getKind().equals("VarDeclaration"))
                    if (i.get("variable").equals(temp.get("value")))
                        ins = i;
            }
        }
        temp = node.getJmmChild(1);
        if (temp.getKind().equals("NewInstance")) {
            string += "temp.";
            varCode(ins);
            string += " :=.";
            varCode(ins);
            string += " new(";
            varCode(ins);
            string += ").";
            varCode(ins);
            string += ";\n";
            string += "invokespecial(temp.";
            varCode(ins);
            string += ",\"<init>\").";
            if (node.getJmmParent().hasAttribute("nameParameter"))
                string += "V;\n";
            else{
                varCode(node.getJmmParent());
                string += ";\n";
            }
            string += node.getJmmChild(0).get("value") + ".";
            varCode(ins);
            string += " :=.";
            varCode(ins);
            string += " temp.";
            varCode(ins);
        }
        else if (temp.getKind().equals("NewArrayInstance")){
            string += "temp.";
            varCode(ins);
            string += " :=.";
            varCode(ins);
            string += " arraylength(" + temp.getJmmChild(0).get("value") + ".";
            varCode(ins);
            string += ").";
            varCode(ins);
            string += ";\n" + ins.get("variable") + ".";
            varCode(ins);
            string += " :=.";
            varCode(ins);
            string += " new(array, temp.i32).";
            varCode(ins);
        } else if (temp.getKind().equals("ArrayAcess")) {
            if (temp.getJmmChild(0).getKind().equals("BinaryOp")){
                string += "t1.";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                if (!temp.getJmmChild(0).getJmmChild(0).getKind().equals("ArrayAcess"))
                    string += " " + temp.getJmmChild(0).getJmmChild(0).get("value") + ".";
                else
                    string += " " + temp.getJmmChild(0).getJmmChild(0).getJmmChild(0).get("value") +
                            "[" + temp.getJmmChild(0).getJmmChild(0).getJmmChild(1).get("value")
                            + ".i32].";
                //string += " " + ;
                varCode(ins);
                string += ";\nt2.";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                string += " " + temp.getJmmChild(0).getJmmChild(1).get("value") + "["
                        + temp.getJmmChild(1).get("value") + ".i32].";
                varCode(ins);
                string += ";\nt3.";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                string += " t1.";
                varCode(ins);
                string += " " + temp.getJmmChild(0).get("op") + ".i32 t2.i32;\n";

                string += node.getJmmChild(0).get("value") + ".";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                string += " t3.";
                varCode(ins);

            }
            else {
                string += ins.get("variable") + ".";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                string += " " + temp.getJmmChild(0).get("value") + "[" + temp.getJmmChild(1).get("value") + ".i32].";
                varCode(ins);
            }

        } else if (temp.getKind().equals("Integer") || temp.getKind().equals("ID")) {
            string += node.getJmmChild(0).get("value") + ".";
            varCode(ins);
            string += " :=.";
            varCode(ins);
            string += " ";
            String val = node.getJmmChild(0).get("value");
            String num = node.getJmmChild(1).get("value");
            String type = node.getJmmChild(1).getKind();
            string +=  num + ".";
            varCode(ins);
        } else if (temp.getKind().equals("Call")) {
            String id;
            JmmNode bin;
            if (temp.getJmmChild(0).getKind().equals("Identifier")) {
                id = temp.getJmmChild(0).get("value");
                bin = temp.getJmmChild(1);
            }
            else{
                id = temp.getJmmChild(1).get("value");
                bin = temp.getJmmChild(0);
            }
            if (temp.getNumChildren() == 2) {
                string += "temp_0.";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                string += " invokevirtual(";
                if (bin.getKind().equals("thisAcess"))
                    string += bin.get("value");
                else {
                    for (JmmNode k : node.getJmmParent().getChildren()) {
                        if (k.getKind().equals("VarDeclaration")) {
                            if (k.get("variable").equals(bin.get("value"))) {
                                string += bin.get("value") + ".";
                                varCode(k);
                            }
                        }
                    }
                }
                string += ", \"" + id + "\").";
                varCode(ins);
                string += ";\n";
                string += node.getJmmChild(0).get("value") + "."; varCode(ins);
                string += " :=."; varCode(ins); string += " ";

                string += "temp_0" + "."; varCode(ins);
                //string += " " + bin.get("op") + "."; varCode(ins); string += " temp_0."; varCode(ins);

            }
            else{
                string += "temp_0.";
                varCode(ins);
                string += " :=.";
                varCode(ins);
                string += " invokevirtual(";
                int f = 0;
                for (JmmNode j : temp.getChildren()) {
                    if (f > 0)
                        string += ", ";
                    if (j.getKind().equals("ID")) {
                        for (JmmNode k : node.getJmmParent().getChildren()) {
                            if (k.getKind().equals("VarDeclaration")){
                                if (k.get("variable").equals(j.get("value"))){
                                    string += j.get("value") + ".";
                                    varCode(k);
                                }
                            }
                            if (k.getKind().equals("Paremeterlist")){
                                int temp1 = 0;
                                for (JmmNode p : node.getJmmParent().getChildren()) {
                                    temp1++;
                                    if (p.get("nameParameter").equals(j.get("value"))){
                                        string += "$" + temp1 + "." + j.get("value") + ".";
                                        varCode(p);
                                    }
                                }
                            }
                        }
                    } else if (f!=1)
                        string += j.get("value");
                    else
                        string += "\"" + j.get("value") + "\"";
                    f++;
                }
                string += ").";
                varCode(ins);
                string += ";\n";
                string += node.getJmmChild(0).get("value") + "."; varCode(ins);
                string += " :=."; varCode(ins); string += " temp_0."; varCode(ins);
            }
        } else BinaryOp(node, temp);
        string += ";\n";
        return null;
    }

    private Void dealWithVar(JmmNode node) {
        if (node.getJmmParent().getKind().equals("ClassDeclaration")) {
            string += ".field private " + node.get("variable") + ".";
            varCode(node);
            string += ";\n";
        }
        return null;
    }

    private void setDefaultVisit() {
    }

    private void binary(JmmNode node, boolean key) {

        JmmNode bin = null;
        JmmNode integer = null;
        if (node.getJmmChild(0).getKind().equals("BinaryOp")){
            bin = node.getJmmChild(0);
            integer = node.getJmmChild(1);
        }
        else if (node.getJmmChild(1).getKind().equals("BinaryOp")){
            bin = node.getJmmChild(1);
            integer = node.getJmmChild(0);
        }
        if(node.getJmmChild(1).getKind().equals("BinaryOp") || node.getJmmChild(0).getKind().equals("BinaryOp")) {
            binary(bin, true);
            if (key)
                string += "t1.i32 :=.i32 " + integer.get("value")
                        + ".i32 " + node.get("op") + ".i32 t1.i32;\n";
        }
        else
            string += "t1.i32 :=.i32 "  + node.getJmmChild(0).get("value")
                + ".i32 " + node.get("op") + ".i32 " + node.getJmmChild(1).get("value") + ".i32;\n";
    }

    private Void dealStatement(JmmNode node) {
        JmmNode temp = node.getJmmChild(0);


        int t = 0;
        if (temp.getKind().equals("Call")){
            int check = 0;
            for (JmmNode i : temp.getChildren()){
                if (check < 2)
                    check++;
                else {
                    if (i.getKind().equals("LenCall")) {
                        t++;
                        string += "t" + t + ".i32 :=.i32 " ;
                        string += i.getJmmChild(0).get("value") + ".i32;\n";
                    }
                    else if (i.getKind().equals("ArrayAcess")) {
                        if (i.getJmmChild(1).getKind().equals("Call")) {
                            t++;
                            string += "t" + t + ".i32 :=.i32 " ;
                            callResolve(i.getJmmChild(1));
                            string += ").i32";
                        }else if (i.getJmmChild(1).getKind().equals("ArrayAcess")) {
                            t++;
                            string += "t" + t + ".i32 :=.i32 " ;
                            string += i.getJmmChild(1).getJmmChild(0).get("value") + "["
                                    + i.getJmmChild(1).getJmmChild(1).get("value")
                                    + ".i32].i32";
                        } else if (i.getJmmChild(1).getKind().equals("BinaryOp")) {
                            t++;
                            string += "t" + t + ".i32 :=.i32 " ;
                            if (i.getJmmChild(1).getJmmChild(0).getKind().equals("LenCall"))
                                string += i.getJmmChild(0).get("value") + ".i32 ";
                            else if (i.getJmmChild(1).getJmmChild(0).getKind().equals("ArrayAcess")) {
                                string += i.getJmmChild(1).getJmmChild(0).getJmmChild(0).get("value")
                                        + "[" + i.getJmmChild(1).getJmmChild(0).getJmmChild(1).get("value")
                                        + ".i32].i32 ";
                            }
                            else
                                string += i.getJmmChild(1).getJmmChild(0).get("value") + ".i32 ";

                            string += i.getJmmChild(1).get("op") + ".i32 ";

                            if (i.getJmmChild(1).getJmmChild(1).getKind().equals("LenCall"))
                                string += i.getJmmChild(1).getJmmChild(1).get("value") + ".i32 ";
                            else if (i.getJmmChild(1).getKind().equals("ArrayAcess")) {
                                string += i.getJmmChild(1).getJmmChild(1).getJmmChild(0).get("value")
                                        + "[" + i.getJmmChild(1).getJmmChild(1).getJmmChild(1).get("value")
                                        + ".i32].i32 ";
                            }
                            else
                                string += i.getJmmChild(1).getJmmChild(1).get("value");
                            string += ".i32";

                        } else {
                            t++;
                            string += "t" + t + ".i32 :=.i32 " ;
                            string += i.getJmmChild(1).get("value") + ".i32";
                        }
                        t++;
                        string += ";\nt" + t + ".i32 :=.i32 " + i.getJmmChild(0).get("value")
                                + "[t" + (t - 1) + ".i32" + "].i32;\n";
                    } else if (i.getKind().equals("BinaryOp")) {
                        binary(i,false);
                    } else {
                        t++;
                        string += "t" + t + ".i32 :=.i32 " + i.get("value") + ".i32;\n";
                    }
                }
            }
        }
        string += "invokestatic(" + temp.getJmmChild(0).get("value") +
                ", \"" + temp.getJmmChild(1).get("value") + "\"";
        int check = 0;
        for (JmmNode i : temp.getChildren()) {
            if (check < 2)
                check++;
            else {
                string += ", t" + t + ".i32";
                t++;
            }
        }
        string += ").V;\n";
        return null;
    }

    private void callResolve(JmmNode node){

        string += "invokestatic(" + node.getJmmChild(0).get("value") +
                ", \"" + node.getJmmChild(1).get("value") + "\"";
        int check = 0;
        for (JmmNode i : node.getChildren()){
            if (check < 2)
                check++;
            else {
                string += ", ";
                if (i.getKind().equals("LenCall"))
                    string += i.getJmmChild(0).get("value") + ".i32";
                else if (i.getKind().equals("ArrayAcess")) {
                    string += i.getJmmChild(0).get("value") + "[";
                    if (i.getJmmChild(1).getKind().equals("Call")) {
                        callResolve(i.getJmmChild(1));
                        string += ")].i32";
                    }else if (i.getJmmChild(1).getKind().equals("ArrayAcess")) {
                        string += i.getJmmChild(1).getJmmChild(0).get("value") + "["
                                + i.getJmmChild(1).getJmmChild(1).get("value")
                                + ".i32].i32].i32";
                    } else if (i.getJmmChild(1).getKind().equals("BinaryOp")) {
                        if (i.getJmmChild(1).getJmmChild(0).getKind().equals("LenCall"))
                            string += i.getJmmChild(0).get("value") + ".i32 ";
                        else if (i.getJmmChild(1).getJmmChild(0).getKind().equals("ArrayAcess")) {
                            string += i.getJmmChild(1).getJmmChild(0).getJmmChild(0).get("value")
                                    + "[" + i.getJmmChild(1).getJmmChild(0).getJmmChild(1).get("value")
                                    + ".i32].i32].i32 ";
                        }
                        else
                            string += i.getJmmChild(1).getJmmChild(0).get("value") + ".i32 ";

                        string += i.getJmmChild(1).get("op") + " ";

                        if (i.getJmmChild(1).getJmmChild(1).getKind().equals("LenCall"))
                            string += i.getJmmChild(1).getJmmChild(1).get("value") + ".i32 ";
                        else if (i.getJmmChild(1).getKind().equals("ArrayAcess")) {
                            string += i.getJmmChild(1).getJmmChild(1).getJmmChild(0).get("value")
                                    + "[" + i.getJmmChild(1).getJmmChild(1).getJmmChild(1).get("value")
                                    + ".i32].i32].i32 ";
                        }
                        else
                            string += i.getJmmChild(1).getJmmChild(1).get("value");
                        string += ".i32].i32";

                    } else
                        string += i.getJmmChild(1).get("value") + ".i32].i32";
                } else
                    string += i.get("value") + ".i32";
            }
        }

    }

    private void BinaryOp(JmmNode node, JmmNode temp) {
        if (temp.getKind().equals("BinaryOp")) {
            JmmNode bin;
            JmmNode integer;
            if (node.getJmmChild(0).getKind().equals("BinaryOp")){
                bin = node.getJmmChild(0);
                integer = node.getJmmChild(1);
            }
            else{
                bin = node.getJmmChild(1);
                integer = node.getJmmChild(0);
            }
            if (bin.getJmmChild(1).getKind().equals("BinaryOp") || bin.getJmmChild(0).getKind().equals("BinaryOp")){
                binary(bin,false);
                String num;
                if(bin.getJmmChild(1).getKind().equals("BinaryOp"))
                    num = bin.getJmmChild(0).get("value");
                else
                    num = bin.getJmmChild(1).get("value");
                string += integer.get("value") + ".i32 :=.i32 "  + num
                        + ".i32 " + node.getJmmChild(1).get("op") + ".i32 t1.i32";
            }
            else {
                string += integer.get("value") + ".i32 :=";
                boolean key = false;
                for (JmmNode i : bin.getChildren()) {
                    if (key)
                        string += " " + node.getJmmChild(1).get("op") + " ";
                    else
                        key = true;
                    string += ".i32 " + i.get("value") + ".i32";
                }
            }
        }
    }

    private Void dealWithProgram(JmmNode node) {
        for (JmmNode s : node.getChildren()) {
            visit(s);
        }
        return null;
    }

    private Void dealWithMethod(JmmNode node) {
        //this.visit()
        if (node.hasAttribute("nameParameter")){
            string += ".method public static main(" + node.get("nameParameter") + ".array.String)" + ".V {\n";
            if (node.getNumChildren() > 1) {
                for (JmmNode i : node.getChildren().subList(1, node.getChildren().size())) {
                    visit(i);
                }
            }
            string += "ret.V;\n" + "}\n\n";
        }
        else{
            string += ".method public " + node.get("name") + "(";
            boolean test = false;
            for(JmmNode i : node.getChildren().get(1).getChildren()) {
                if (test)
                    string += ",";
                else
                    test = true;
                string += i.get("nameParameter") + ".";
                varCode(i);
            }
            string += ").";
            varCode(node);

            string += " {\n";

            for (JmmNode i : node.getChildren().subList(2,node.getChildren().size()-1)){
                visit(i);
            }
            JmmNode temp = node.getJmmChild(node.getNumChildren()-1);

            if (temp.getKind().equals("BinaryOp")){
                string+="t1.i32 :=.i32 ";
                boolean key = false;
                for (JmmNode i : temp.getChildren()){
                    if (key)
                        string += " +.i32 ";
                    else
                        key = true;
                    string += i.get("value") + ".i32";
                }
                string+=";\nret.i32 t1.i32";
            }
            else if (temp.getKind().equals("Integer")){
                String num = temp.get("value");
                string+="ret.";
                varCode(node);
                string+= " " + num + ".";
                varCode(node);
            }
            else if (temp.getKind().equals("ID")){
                int key = 0;
                boolean param = true;
                string+="ret.";
                for (JmmNode i : node.getJmmChild(1).getChildren()){
                    if (i.get("nameParameter").equals(temp.get("value"))){
                        varCode(node);
                        string+=" $" + key + "." + temp.get("value") + ".";
                        varCode(node);
                        param = false;
                        break;
                    }
                    key++;
                }
                if (param) {
                    for (JmmNode i : node.getChildren()) {
                        if (i.getKind().equals("VarDeclaration")) {
                            if (i.get("variable").equals(temp.get("value")))
                                varCode(node);
                            string += " " + temp.get("value") + ".";
                            varCode(node);
                            break;
                        }
                    }
                    for (JmmNode i : node.getJmmParent().getChildren()) {
                        if (i.getKind().equals("VarDeclaration")) {
                            if (i.get("variable").equals(temp.get("value")))
                                varCode(node);
                            string += " " + temp.get("value") + ".";
                            varCode(node);
                            break;
                        }
                    }
                }
            }
            else if (temp.getKind().equals("BooleanValue")){
                String num = temp.get("value");
                string+="ret.";
                varCode(node);
                string+= " " + num + ".";
                varCode(node);
            }
            else {
                string += "ret.i32 " + temp.get("value") + ".i32";

            }
            string += ";\n}\n\n";
        }

        return null;
    }

    private void varCode(JmmNode node) {


        if (node.getJmmChild(0).get("isArray").equals("true")){
            string += "array.";
        }
        if (node.getJmmChild(0).get("value").equals("int")){
            string += "i32";
        } else if (node.getJmmChild(0).get("value").equals("boolean")) {
            string += "bool";
        }
        else
            string += node.getJmmChild(0).get("value");
    }


    private Void dealWithClass(JmmNode node) {
        string += node.get("name");
        if (node.hasAttribute("extend"))
            string += " extends " + node.get("extend");
        string += " {\n";
        for (JmmNode s : node.getChildren()) {
            if (s.getKind().equals("VarDeclaration"))
                visit(s);
        }
        string += ".construct " + node.get("name") + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n"+
                "}\n\n";

        for (JmmNode s : node.getChildren()) {
            if (!s.getKind().equals("VarDeclaration"))
                visit(s);
        }
        string += "}\n";
        return null;
    }

    private Void dealWithImport(JmmNode node) {
        string += "import " + node.get("value");
        if (!node.get("name").equals("[]")) {
            String str = node.get("name");
            str = str.substring(1, str.length() - 1);
            String[] arr = str.split(", ");
            for (String i : arr)
                string += "." + i;
        }
        string +=  ";\n";
        return null;
    }

}
