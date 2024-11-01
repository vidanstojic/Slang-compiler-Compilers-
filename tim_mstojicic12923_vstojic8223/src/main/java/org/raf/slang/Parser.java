package org.raf.slang;

import lombok.Getter;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import slang.parser.SlangParser;

public class Parser {
    private final Slang compiler;

    @Getter
    private SlangParser slangParser;

    public Parser(Slang compiler) {
        this.compiler = compiler;
    }

    public SlangParser.StartContext getSyntaxTree(Lexer tokens) {
        CommonTokenStream tokenStream = new CommonTokenStream(tokens);
        slangParser = new SlangParser(tokenStream);
        slangParser.removeErrorListeners();
        slangParser.addErrorListener(compiler.errorListener());

        return SlangParser.start();
    }
}
