
package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.valueOf;


public class SemanticVisitor extends AJmmVisitor<List<Report>, Void> {


    SymbolTableImp symbolTable;
    private List<Report> reports;

    List<String> variables;

    @Override
    public void buildVisitor() {


        symbolTable = new SymbolTableImp();
        reports = new ArrayList<>();
        variables = new ArrayList<>();


        addVisit("Statement", (node, s) -> this.dealWithStatement(node));
        addVisit("MethodDeclaration", (node, s) -> this.dealWithMethod(node));
        addVisit("ArrayAcess", (node, s) -> this.dealWithArray(node));
        addVisit("ThisAcess", (node, s) -> this.dealWithThis(node));


        setDefaultVisit(this::defaultVisit);
    }

    private Void dealWithThis(JmmNode node) {
        Optional<JmmNode> jmmNode = node.getAncestor("MethodDeclaration");
        if (jmmNode.get().hasAttribute("nameParameter")) {
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("lineStart")),
                    Integer.parseInt(node.get("colStart")),
                    "Invalid Operation in if not possible"));

        }


        return null;

    }


    private Void defaultVisit(JmmNode node, List<Report> reports) {
        for (JmmNode child : node.getChildren())
            visit(child, reports);
        return null;
    }

    private Void dealWithStatement(JmmNode node) {
        JmmNode parent = node.getJmmParent();

        Optional<JmmNode> parente2 = node.getAncestor("MethodDeclaration");
        if (node.getJmmChild(0).getKind().equals("BinaryOp")) {
            boolean key = true;
            for (String wh : node.getHierarchy()) {
                if (wh.equals("WhileStat") || wh.equals("IfStat"))
                    key = false;
            }
            if (node.getJmmChild(0).get("op").equals(">") || node.getJmmChild(0).get("op").equals("<")) {
                key = true;
            }
            if (!key) {
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("lineStart")),
                        Integer.parseInt(node.get("colStart")),
                        "Invalid Operation in if not possible"));
            }
        } else if (node.getKind().equals("VarDecl")) {
            String var = node.getJmmChild(0).get("value");
            String value = node.getJmmChild(1).getKind();
            if (value.equals("Integer"))
                value = "int";
            else if (value.equals("BooleanValue")) {
                value = "boolean";
            } else if (value.equals("NewInstance")) {
                value = node.getJmmChild(1).getJmmChild(0).get("value");
            }
            if (value.equals("ID")) {
                for (JmmNode i : parent.getChildren()) {
                    if (i.getKind().equals("VarDeclaration")) {
                        if (i.get("variable").equals(var)) {
                            value = i.getJmmChild(0).get("value");
                        }
                    }
                }
            }
            boolean key1 = true;
            boolean key = true;
            boolean key3 = true;
            for (JmmNode i : parent.getChildren()) {
                if (i.getKind().equals("VarDeclaration")) {
                    if (node.getJmmChild(1).getKind().equals("ID") && !(value.equals("int") || value.equals("boolean"))) {
                        if (i.get("variable").equals(node.getJmmChild(1).get("value"))) {
                            if (!i.getJmmChild(0).get("value").equals(value))
                                for (JmmNode k : parent.getJmmParent().getJmmParent().getChildren()) {
                                    if (k.getKind().equals("ImportDeclaration")) {
                                        if (k.get("value").equals(value)) {
                                            key = false;
                                        }
                                        if (k.get("value").equals(i.getJmmChild(0).get("value"))) {
                                            key1 = false;
                                        }
                                    }
                                }
                            else {
                                key = false;
                                key1 = false;
                            }
                            if (parent.getJmmParent().hasAttribute("extend")) {
                                if (parent.getJmmParent().get("extend").equals(value)) {
                                    key3 = false;
                                }
                                if (parent.getJmmParent().get("extend").equals(i.getJmmChild(0).get("value"))) {
                                    key3 = false;
                                }
                            }
                            if ((key || key1) && key3)
                                reports.add(new Report(
                                        ReportType.ERROR,
                                        Stage.SEMANTIC,
                                        Integer.parseInt(node.get("lineStart")),
                                        Integer.parseInt(node.get("colStart")),
                                        "Object assignment fail"));
                        }
                    } else if (i.get("variable").equals(var) && !value.equals("Call") && !value.equals("BinaryOp")) {
                        if (!i.getJmmChild(0).get("value").equals(value)) {
                            if (!value.equals("ArrayAcess") && !value.equals("NewArrayInstance")) {

                                reports.add(new Report(
                                        ReportType.ERROR,
                                        Stage.SEMANTIC,
                                        Integer.parseInt(node.get("lineStart")),
                                        Integer.parseInt(node.get("colStart")),
                                        "Incorret use with bool "));

                            }

                            for (JmmNode parent2 : parent.getChildren()) {
                                if (i.getJmmChild(0).get("isArray").equals("true")) {
                                    if (parent2.getKind().equals("WhileStat")) {
                                        reports.add(new Report(
                                                ReportType.ERROR,
                                                Stage.SEMANTIC,
                                                Integer.parseInt(node.get("lineStart")),
                                                Integer.parseInt(node.get("colStart")),
                                                "Incorret use array in while "));

                                    }
                                }
                            }

                        }
                    }
                }
            }


        } else if (node.getJmmChild(0).getKind().equals("Call")) {
            JmmNode temp = node.getJmmChild(0);
            String var = temp.getJmmChild(0).get("value");
            String id = temp.getJmmChild(1).get("value");
            String id1 = null;
            for (JmmNode i : parent.getChildren()) {
                if (i.getKind().equals("VarDeclaration")) {
                    if (i.get("variable").equals(var))
                        id1 = i.getJmmChild(0).get("value");
                }
            }
            boolean key = true;

            if (id1 != null) {
                if (id1.equals(parent.getJmmParent().get("name"))) {
                    for (JmmNode i : parent.getJmmParent().getChildren()) {
                        if (i.hasAttribute("name")) {
                            if (i.get("name").equals(id)) {
                                key = false;
                                break;
                            }
                        }
                        if (i.hasAttribute("nameParameter")) {
                            if (i.get("nameParameter").equals(id)) {
                                key = false;
                                break;
                            }
                        }
                    }
                }
                for (JmmNode j : parent.getJmmParent().getJmmParent().getChildren()) {
                    if (j.getKind().equals("ImportDeclaration")) {
                        if (j.get("value").equals(id1)) {
                            key = false;
                        }
                    }
                }


                if (parent.getJmmParent().hasAttribute("extend")) {
                    for (JmmNode j : node.getJmmParent().getChildren()) {
                        if (j.getKind().equals("VarDeclaration")) {

                            if (j.getJmmChild(0).get("value").equals(parent.getJmmParent().get("name"))) {
                                key = false;
                            }
                        }
                    }
                }

                //if(node.get)


                if (key) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("lineStart")),
                            Integer.parseInt(node.get("colStart")),
                            "Undeclared Method"));
                }


            }
        }
        boolean key = true;
        int x = 0;
        for (JmmNode jmmNode : parent.getChildren()) {
            if (jmmNode.getKind().equals("VarDecl")) {
                for (JmmNode jmmNode1 : parent.getChildren()) {
                    if (jmmNode1.getKind().equals("VarDeclaration")) {
                        if (jmmNode.getJmmChild(0).get("value").equals(jmmNode1.get("variable"))) {
                            key = false;
                        }
                        if (jmmNode.getJmmChild(1).getKind().equals("ID")) {
                            if (jmmNode.getJmmChild(1).get("value").equals(jmmNode1.get("variable"))) {
                                key = false;
                            }
                        }
                    }
                }
                for (JmmNode jmmNode2 : parent.getJmmParent().getChildren()) {
                    if (jmmNode2.getKind().equals("VarDeclaration")) {
                        for (JmmNode jmmNode3 : jmmNode2.getJmmParent().getChildren()) {
                            if (jmmNode3.getKind().equals("MethodDeclaration")) {
                                for (JmmNode jmmNode4 : jmmNode3.getChildren()) {
                                    if (jmmNode4.getKind().equals("VarDecl")) {
                                        if (jmmNode4.getJmmChild(0).get("value").equals(jmmNode2.get("variable"))) {
                                            key = false;
                                        }
                                    }
                                }
                            }


                        }
                    }
                }

                String name = "";
                for (JmmNode jmmNode1 : parent.getChildren()) {
                    if (jmmNode1.getKind().equals("Paremeterlist")) {
                        if (jmmNode1.getNumChildren() > 0) {
                            if (jmmNode1.getJmmChild(0).hasAttribute("nameParameter")) {
                                name = jmmNode1.getJmmChild(0).get("nameParameter");
                            }
                        }
                    } else if (jmmNode1.getKind().equals("VarDecl")) {
                        if (jmmNode1.getJmmChild(0).get("value").equals(name)) {
                            key = false;
                        }
                    }
                }

                if (key) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(node.get("lineStart")),
                            Integer.parseInt(node.get("colStart")),
                            "Undeclared Integer"));
                }
            }
        }
        List<String> a = new ArrayList<>();
        for (JmmNode i : parent.getJmmParent().getChildren()) {

            if (i.getKind().equals("VarDeclaration") && i.get("isStatic").equals("false")) {
                a.add(i.get("variable"));
            } else {

                String value = "";
                for (JmmNode jmmNode : parent.getJmmParent().getJmmParent().getChildren()) {
                    if (jmmNode.getJmmParent().getKind().equals("Program")) {
                        if (jmmNode.getKind().equals("ImportDeclaration")) {
                            value = jmmNode.get("value");
                        }
                        for (JmmNode jmmNode1 : parent.getChildren()) {
                            if (jmmNode1.getKind().equals("End")) {
                                for (JmmNode jmmNode2 : jmmNode1.getJmmChild(0).getChildren()) {
                                    if (jmmNode2.getKind().equals("ID") && jmmNode2.get("value").equals(value)) {
                                        x = 1;


                                    }

                                }

                            }

                        }
                    }

                }


                if (!parent.getJmmParent().getJmmParent().getJmmChild(0).getKind().equals("ImportDeclaration")) {
                    if (i.getKind().equals("MethodDeclaration") && i.hasAttribute("nameParameter")) {
                        for (JmmNode j : i.getChildren()) {
                            if (j.getKind().equals("VarDecl")) {
                                int w = 0;
                                while (w < a.size()) {
                                    if (a.get(w).equals(j.getJmmChild(0).get("value"))) {
                                        reports.add(new Report(
                                                ReportType.ERROR,
                                                Stage.SEMANTIC,
                                                Integer.parseInt(node.get("lineStart")),
                                                Integer.parseInt(node.get("colStart")),
                                                "Variable not static"));
                                    }
                                    w++;
                                }
                            }

                        }
                    }


                }
            }
        }


        return null;
    }


    private Void dealWithArray(JmmNode node) {
        String var = node.getJmmChild(0).get("value");
        String index = node.getJmmChild(1).get("value");
        JmmNode parent = node.getJmmParent();
        //String type=node.getJmmChild(0).
        String arr = null;
        String arr1 = null;
        String value = null;
        String value1 = null;


        for (JmmNode j : parent.getChildren()) {
            if (j.getKind().equals("VarDeclaration")) {
                if (j.get("variable").equals(var)) {
                    value = j.getJmmChild(0).get("value");
                    arr = j.getJmmChild(0).get("isArray");
                }
                if (j.get("variable").equals(index)) {
                    value1 = j.getJmmChild(0).get("value");
                    arr1 = j.getJmmChild(0).get("isArray");
                }
            }
        }
        if (arr.equals("false"))
            reports.add(new Report(
                    ReportType.ERROR,
                    Stage.SEMANTIC,
                    Integer.parseInt(node.get("lineStart")),
                    Integer.parseInt(node.get("colStart")),
                    "Incorret acess array "));

        if (value1 != null)
            if (!value1.equals(value))
                reports.add(new Report(
                        ReportType.ERROR,
                        Stage.SEMANTIC,
                        Integer.parseInt(node.get("lineStart")),
                        Integer.parseInt(node.get("colStart")),
                        "BinaryOp not possible"));

        return null;
    }

    private Void dealWithMethod(JmmNode node) {


        for (JmmNode i : node.getChildren()) {
            visit(i);

            if (i.getKind().equals("BinaryOp")) {

                boolean key = false;
                boolean key1 = false;
                String val = i.getJmmChild(0).get("value");
                String val1 = i.getJmmChild(1).get("value");
                String value = null;
                String value1 = null;
                String type = null;
                String type1 = null;

                for (JmmNode j : node.getChildren()) {
                    if (j.getKind().equals("VarDeclaration")) {
                        if (j.get("variable").equals(val)) {
                            value = j.getJmmChild(0).get("value");
                            type = j.getJmmChild(0).get("isArray");
                            if (!(value.equals("int") || value.equals("boolean")))
                                key = true;
                        }
                        if (j.get("variable").equals(val1)) {
                            value1 = j.getJmmChild(0).get("value");
                            type1 = j.getJmmChild(0).get("isArray");
                            if (!(value1.equals("int") || value1.equals("boolean")))
                                key1 = true;
                        }

                    }
                }
                if (key || key1) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(i.get("lineStart")),
                            Integer.parseInt(i.get("colStart")),
                            "BinaryOp not possible"));
                }


                if (value != null && value1 != null) {
                    if (!value.equals(value1))
                        reports.add(new Report(
                                ReportType.ERROR,
                                Stage.SEMANTIC,
                                Integer.parseInt(i.get("lineStart")),
                                Integer.parseInt(i.get("colStart")),
                                "One is array the other is not"));

                    if (!type.equals(type1))
                        reports.add(new Report(
                                ReportType.ERROR,
                                Stage.SEMANTIC,
                                Integer.parseInt(i.get("lineStart")),
                                Integer.parseInt(i.get("colStart")),
                                "One is array the other is not"));
                }


            }


            if (i.getKind().equals("Call")) {
                String val = i.getJmmChild(1).get("value");
                String id = i.getJmmChild(0).get("value");
                boolean key = true;
                boolean key1 = true;
                String clss = null;
                String clss1 = null;
                for (JmmNode j : node.getJmmParent().getJmmParent().getChildren()) {
                    if (j.getKind().equals("ImportDeclaration")) {
                        if (j.get("name").equals(val)) {
                            key = false;
                            break;
                        }
                    }
                    if (j.getKind().equals("ClassDeclaration"))
                        clss = j.get("name");
                }
                for (JmmNode j : node.getChildren()) {
                    if (j.getKind().equals("VarDecl")) {
                        if (j.getJmmChild(0).get("value").equals(id) && j.getJmmChild(1).getKind().equals("NewInstance")) {
                            clss1 = j.getJmmChild(1).getJmmChild(0).get("value");
                            key1 = false;
                            break;
                        }
                    }
                }

                if (clss1 != null || clss != null) {
                    key = true;
                    JmmNode method = null;
                    if (clss1 != null)
                        if (clss1.equals(clss)) {
                            for (JmmNode j : node.getJmmParent().getChildren()) {
                                if (j.getKind().equals("MethodDeclaration")) {
                                    if (j.hasAttribute("nameParameter")) {
                                        if (j.get("nameParameter").equals(val)) {
                                            key = false;
                                            method = j;
                                            break;
                                        }
                                    } else {
                                        if (j.get("name").equals(val)) {
                                            key = false;
                                            method = j;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (key)
                                reports.add(new Report(
                                        ReportType.ERROR,
                                        Stage.SEMANTIC,
                                        Integer.parseInt(i.get("lineStart")),
                                        Integer.parseInt(i.get("colStart")),
                                        val + " method not declared"));
                            else {
                                String value = null;
                                String value1 = null;
                                if (method.getJmmChild(1).getNumChildren() > 0)
                                    value = method.getJmmChild(1).getJmmChild(0).getJmmChild(0).get("value");
                                else
                                    reports.add(new Report(
                                            ReportType.ERROR,
                                            Stage.SEMANTIC,
                                            Integer.parseInt(i.get("lineStart")),
                                            Integer.parseInt(i.get("colStart")),
                                            val + " method not declared"));
                                for (JmmNode k : node.getChildren()) {
                                    if (k.getKind().equals("VarDeclaration"))
                                        value1 = k.getJmmChild(0).get("value");
                                }
                                if (!value.equals(value1))
                                    reports.add(new Report(
                                            ReportType.ERROR,
                                            Stage.SEMANTIC,
                                            Integer.parseInt(i.get("lineStart")),
                                            Integer.parseInt(i.get("colStart")),
                                            val + " method isn't initialised correctly"));
                            }
                        }
                }

                for (JmmNode j : node.getChildren()) {
                    if (j.getKind().equals("Call")) {
                        String nome = j.getJmmChild(1).get("value");
                        for (JmmNode jmm : node.getJmmParent().getChildren()) {
                            if (jmm.hasAttribute("name")) {
                                if (nome.equals(jmm.get("name"))) {
                                    key = false;
                                }
                            }

                        }
                    }
                }

                for (JmmNode j : node.getChildren()) {
                    if (j.getKind().equals("Call")) {
                        String name = j.getJmmChild(0).get("value");
                        for (JmmNode jmm : node.getJmmParent().getJmmParent().getChildren()) {
                            if (jmm.getKind().equals("ImportDeclaration")) {
                                if (name.equals(jmm.get("value"))) {
                                    key = false;
                                }
                            }

                        }


                    }
                }


                if (key && key1) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(i.get("lineStart")),
                            Integer.parseInt(i.get("colStart")),
                            val + " not imported declared"));
                }
            }

            if (i.getKind().equals("ID")) {
                boolean key = true;
                for (JmmNode j : node.getChildren()) {
                    if (j.getKind().equals("VarDeclaration")) {
                        if (j.get("variable").equals(i.get("value"))) {
                            key = false;
                            break;
                        }
                    }
                }
                for (JmmNode j : node.getJmmParent().getChildren()) {
                    if (j.getKind().equals("VarDeclaration")) {
                        if (j.get("variable").equals(i.get("value"))) {
                            key = false;
                            break;
                        }
                    }
                }
                for (JmmNode j : node.getJmmChild(1).getChildren()) {
                    if (j.get("nameParameter").equals(i.get("value"))) {
                        key = false;
                        break;
                    }
                }

                if (key) {
                    reports.add(new Report(
                            ReportType.ERROR,
                            Stage.SEMANTIC,
                            Integer.parseInt(i.get("lineStart")),
                            Integer.parseInt(i.get("colStart")),
                            i.get("value") + "not declared"));
                }


            }
            if (i.getKind().equals("Type")) {
                if (node.getJmmParent().getNumChildren() > 2) {
                    if (node.getJmmParent().getJmmChild(2).getNumChildren() > 6) {
                        String string3 = node.getJmmParent().getJmmChild(2).getJmmChild(6).getKind();
                        if (string3.equals("Call")) ;
                        String string4 = node.getJmmParent().getJmmChild(2).getJmmChild(0).get("value");// BOOL
                        String string5 = node.getJmmParent().getJmmChild(2).getJmmChild(6).getJmmChild(1).get("value");


                        String string7;

                        if (node.getJmmParent().getJmmChild(1).hasAttribute("nameParameter"))
                            string7 = node.getJmmParent().getJmmChild(1).get("nameParameter");
                        else
                            string7 = node.getJmmParent().getJmmChild(1).get("name");

                        if (string7.equals(string5)) {
                            if (!string4.equals(node.getJmmParent().getJmmChild(1).getJmmChild(0).get("value"))) {
                                reports.add(new Report(
                                        ReportType.ERROR,
                                        Stage.SEMANTIC,
                                        Integer.parseInt(i.get("lineStart")),
                                        Integer.parseInt(i.get("colStart")),
                                        "Return type error"));
                            }
                        }
                    }


                }


            }

            for (JmmNode jmmNode : node.getJmmParent().getChildren()) {
                for (JmmNode jmmNode1 : jmmNode.getChildren()) {
                    if (jmmNode1.getKind().equals("VarDecl")) {
                        if (jmmNode1.getJmmChild(1).getKind().equals("NewArrayInstance")) {
                            String nome = jmmNode1.getJmmChild(0).get("value");
                            int n = valueOf(jmmNode1.getJmmChild(1).getJmmChild(0).get("value"));
                            for (JmmNode jmmNode2 : jmmNode.getChildren()) {
                                if (jmmNode2.getKind().equals("Array")) {
                                    if (jmmNode2.getJmmChild(0).get("value").equals(nome) && valueOf(jmmNode2.getJmmChild(1).get("value")) >= n) {
                                        reports.add(new Report(
                                                ReportType.ERROR,
                                                Stage.SEMANTIC,
                                                Integer.parseInt(i.get("lineStart")),
                                                Integer.parseInt(i.get("colStart")),
                                                "Array Index Bad"));
                                    }

                                }

                            }
                        }
                    }


                }

                if (i.getKind().equals("VarDecl")) {
                    boolean key = true;
                    for (JmmNode jmmNode1 : i.getChildren()) {
                        if (jmmNode1.getKind().equals("ID")) {
                            String value = jmmNode1.get("value");
                            for (JmmNode jmmNode2 : node.getChildren()) {
                                if (jmmNode2.getKind().equals("VarDeclaration")) {
                                    if (jmmNode2.get("variable").equals(value)) {
                                        key = false;
                                    }
                                }
                            }
                            if (key) {
                                reports.add(new Report(
                                        ReportType.ERROR,
                                        Stage.SEMANTIC,
                                        Integer.parseInt(i.get("lineStart")),
                                        Integer.parseInt(i.get("colStart")),
                                        jmmNode1.get("value") + "not declared"));
                            }
                        }
                    }
                }

            }


        }


        return null;
    }


    private Void dealWithClass(JmmNode node) {
        for (JmmNode i : node.getChildren()) {
            visit(i);
        }


        return null;
    }


    public List<Report> getReports() {
        return reports;
    }
}

