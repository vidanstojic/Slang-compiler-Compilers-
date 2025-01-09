package org.raf;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.raf.slang.Parser;
import org.raf.slang.Scanner;
import org.raf.slang.Slang;
import org.raf.slang.TypeCheck;
import org.raf.slang.ast.ASTNodePrinter;
import org.raf.slang.ast.CSTtoASTConverter;
import org.raf.slang.ast.StatementList;
import org.raf.slang.codegen.CodeGenerator;
import org.raf.slang.utils.SlangPrint;
import org.raf.slang.vm.VM;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class Main {

    private static final Slang slang = new Slang();

    private static final CSTtoASTConverter treeProcessor
            = new CSTtoASTConverter(slang);

    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static final CodeGenerator codeGenerator = new CodeGenerator(slang);
    private static final VM vm = new VM(slang);

    public static void main(String[] args) throws IOException {
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        System.out.println("1. Write a code");
        System.out.println("2. Load a code from file");
        System.out.print("Choose your option: ");
        int answer = scanner.nextInt();
        if (answer == 2) {
            System.out.print("Write your file path(src\\main\\java\\org\\raf\\slang\\resources\\test.txt): ");
            String path = scanner.next();
            runFile(path);
        } else {
            runPrompt();
        }
    }
//      src\main\java\org\raf\slang\resources\test.txt
    private static void runFile(String path) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append(System.lineSeparator());
            }
            run(CharStreams.fromString(content.toString()));
        }

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
        //run(CharStreams.fromFileName(path));
        if (slang.hadError()) System.exit(65);
        if (slang.hadRuntimeError()) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        while (true) {
            System.out.print("> ");
            String line = reader.readLine();

            if (line == null || line.equalsIgnoreCase("exit")){
                break;
            }

            slang.setHadError(false);
            slang.setHadRuntimeError(false);
            run(CharStreams.fromString(line));
           // treeProcessor.finalClose();
        }
    }


    private static void run(CharStream source) {
        Scanner scanner = new Scanner(slang);
        var tokens = scanner.getTokens(source);

        if (slang.hadError()) return;

        Parser parser = new Parser(slang);
        var tree = parser.getSyntaxTree(tokens);


        System.out.println("Syntax Tree: " + SlangPrint.slangPrintTree(tree, parser.getSlangParser().getRuleNames()));

        if (slang.hadError()) return;

        System.out.println("AST:");
        var pp = new ASTNodePrinter(System.out);
        treeProcessor.firstOpen();
        var program = (StatementList) tree.accept(treeProcessor);
        treeProcessor.lastClose();
        program.nodePrint(pp);
        if (slang.hadError()) return;

        new TypeCheck(slang).typecheck(program);
        System.out.println("tAST:");
        program.nodePrint(pp);
        if (slang.hadError()) return;

        var bytecode = codeGenerator.compileInput(program);
        slang.dumpNewAssembly(System.out, bytecode);
        /* The compiler cannot emit errors.  */
        assert !slang.hadError();

        vm.run(bytecode);
    }

}