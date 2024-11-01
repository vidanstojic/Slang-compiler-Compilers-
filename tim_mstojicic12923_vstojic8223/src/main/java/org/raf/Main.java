package org.raf;

import org.antlr.v4.runtime.Parser;
import org.raf.slang.Scanner;
import org.raf.slang.Slang;
import org.raf.slang.ast.ASTNodePrinter;
import org.raf.slang.ast.CSTtoASTConverter;
import org.raf.slang.ast.StatementList;
import org.raf.slang.utils.SlangPrint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main {
    private static final Slang slang = new Slang();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;


    public static void main(String[] args) throws IOException {
        if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            run(content.toString());
        }

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();

            if (line == null || line.equalsIgnoreCase("exit")) {
                break;
            }

            run(line);
            hadError = false;
        }
    }


    private static void run(String source) {
        Scanner scanner = new Scanner(slang);
        var tokens = scanner.getAllTokens(source);

        if (slang.hadError()) return;

        Parser parser = new Parser(slang);
        var tree = parser.getSyntaxTree(tokens);


        System.out.println("Syntax Tree: " + SlangPrint.slangPrintTree(tree, parser.gets().getRuleNames()));

        if (slang.hadError()) return;

        System.out.println("AST:");
        var pp = new ASTNodePrinter(System.out);
        var program = (StatementList) tree.accept(new CSTtoASTConverter());

        program.nodePrint(pp);
    }

}