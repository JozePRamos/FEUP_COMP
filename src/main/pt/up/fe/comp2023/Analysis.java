package pt.up.fe.comp2023;

import org.antlr.v4.semantics.SymbolCollector;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

import java.util.List;
import java.util.ArrayList;




public class Analysis implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        System.out.println("AST: " + parserResult.getRootNode().toTree());
        JmmNode node = parserResult.getRootNode();

        List<Report> reports = new ArrayList<>();

        SemanticVisitor semanticVisitor = new SemanticVisitor();
        semanticVisitor.visit(node, reports);
        var stBuilder = new VisitorPattern();
        stBuilder.visit(parserResult.getRootNode());





        return new JmmSemanticsResult(parserResult, stBuilder.getSymbolTable(), semanticVisitor.getReports());
    }



}
