package org.raf.slang.ast;

import java.io.PrintStream;

public class ASTNodePrinter {
    private int space = 0;
    private final PrintStream output;

    public ASTNodePrinter(PrintStream output) {
        this.output = output;
    }

    private String spaceStr() {
        return " ".repeat(4 * space);
    }

    public void node(String name, Runnable subprinter) {
        var indentStr = spaceStr();
        output.printf("%s%s {\n", indentStr, name);
        try {
            space++;
            subprinter.run();
        } finally {
            space--;
        }
        output.printf("%s}\n", indentStr, name);
    }

    public void terminal(String value) {
        var indentStr = spaceStr();
        output.printf("%s%s;\n", indentStr, value);
    }
}
