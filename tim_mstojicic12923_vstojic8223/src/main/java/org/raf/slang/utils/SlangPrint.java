package org.raf.slang.utils;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SlangPrint {
    public static String slangPrintTree(ParseTree tree, String[] ruleNames) {
        StringBuilder sb = new StringBuilder();
        printNode(tree, sb, ruleNames, 0);
        return sb.toString();
    }

    private static void printNode(ParseTree node, StringBuilder sb, String[] ruleNames, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }

        if (node instanceof TerminalNode) {
            sb.append(node.getText()).append("\n");
        } else {
            sb.append(ruleNames[((ParserRuleContext) node).getRuleIndex()]).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            printNode(node.getChild(i), sb, ruleNames, indent + 1);
        }
    }
}
