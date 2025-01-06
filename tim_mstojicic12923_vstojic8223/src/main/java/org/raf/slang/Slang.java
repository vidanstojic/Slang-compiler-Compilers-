package org.raf.slang;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.raf.slang.ast.*;
import org.raf.slang.vm.BytecodeContainer;
import org.raf.slang.vm.Function;

import java.io.PrintStream;
import java.util.*;

@Getter
@Setter

public class Slang {

    @Getter(AccessLevel.NONE)
    private boolean hadError = false;
    @Getter(AccessLevel.NONE)
    private boolean hadRuntimeError = false;

    public boolean hadError() {
        return hadError;
    }

    public boolean hadRuntimeError() {
        return hadRuntimeError;
    }

    public ANTLRErrorListener errorListener() {
        /* ANTLR reports many kinds of errors, but we're only interested in
           syntax errors, so lets steal those from BaseErrorListener.  */
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol, int line,
                                    int charPositionInLine, String msg,
                                    RecognitionException e) {
                var pos = new Position(line, charPositionInLine);
                error(new Location(pos, pos), "%s", msg);
            }
        };
    }
    public void error(Location location, String message, Object... args) {
        /* Could be improved to handle end also, later.  */
        var p = location.start();
        System.err.printf ("error: %d:%d: %s\n", p.line(), p.column(),
                message.formatted(args));
        setHadError(true);
    }

    /* Type handling.  */
    private final NumberType numberType = new NumberType(null, "numero");// ispraviti ovo null
    private final BoolType boolType = new BoolType(null, "yeahNah");
    private final VoidType voidType = new VoidType(null, "empty");
    @Getter(AccessLevel.NONE)
    private final Map<VariableType, ListType> listTypes = new HashMap<>();
    public VariableType listOfType(VariableType elementType) {
        return listTypes.computeIfAbsent(elementType, ListType::new).getVariableType();
    }

    /* Function handling.  */
    /** Function table.  Used by the VM.  */
    @Getter(AccessLevel.NONE)
    private final List<Function> functions = new ArrayList<>();

    public int addFunction(Function newFunction) {
        var itsIndex = functions.size();
        functions.add(newFunction);
        return itsIndex;
    }

    public Function getFunction(int functionId) {
        return functions.get(functionId);
    }

@Getter(AccessLevel.NONE)
private final IdentityHashMap<SimpleStatement, Integer> globalIndices =
        new IdentityHashMap<>();

    /** Allocate a slot in the globals table for this global variable.  */
    public int declareGlobal(SimpleStatement newGlobal) {
        var newGlobalNumber = globalIndices.size();
        var oldIndex = globalIndices.put(newGlobal, newGlobalNumber);
        /* Should not have been present.  */
        assert oldIndex == null;
        return newGlobalNumber;
    }

    /** Get number of globals.  */
    public int getGlobalCount() {
        return globalIndices.size();
    }

    /** Get the slot in which {@code global} is stored in the global table.  */
    public Optional<Integer> getGlobalSlot(SimpleStatement global) {
        var globalSlot = globalIndices.get(global);
        return Optional.ofNullable(globalSlot);
    }
    // posle ide novo
    @Getter(AccessLevel.NONE)
    /** How many functions have we printed so far?  So that we can resume.  */
    private int printedFunctionCount = 0;
    @Getter(AccessLevel.NONE)
    /** How many globals have we printed so far?  So that we can resume.  */
    private int printedVariableCount = 0;

    public void dumpNewAssembly(PrintStream out, BytecodeContainer bytecode) {
        if (printedVariableCount < globalIndices.size())
            out.println("New globals:");
        globalIndices
                .entrySet()
                .stream()
                .filter(e -> e.getValue() >= printedVariableCount)
                .forEach(e ->
                        out.printf("  %4x: %s\n",
                                e.getValue(), e.getKey().getName()));
        printedVariableCount = globalIndices.size();

        if (printedVariableCount < functions.size())
            out.println("New functions:");
        for (; printedFunctionCount < functions.size();
             printedFunctionCount++) {
            var fn = getFunction(printedFunctionCount);
            var fnTag = "FN%x".formatted(printedFunctionCount);
            out.printf("  %-6s: Function '%s'\n", fnTag,
                    fn.getFuncDef().getName());
            dumpBytecodeContainter(out, "  ", fn.getCode());
            out.printf("  - Upvalues:\n");
            var upvals = fn.getUpvalueMap();
            for (int u = 0; u < upvals.length; u++) {
                out.printf("    %4x: %s\n", u, upvals[u].format());
            }
        }
        out.println("Toplevel blob:");
        dumpBytecodeContainter(out, "  ", bytecode);
    }

    private void dumpBytecodeContainter(PrintStream out, String indent, BytecodeContainer bytecode) {
        if (!bytecode.code().isEmpty())
            out.printf("%s- Code:\n", indent);
        for (int i = 0; i < bytecode.code().size(); i++){
            var insn = bytecode.code().get(i);
            out.printf("%s  %4x: %s\n", indent, i, insn);
        }
        if (!bytecode.constantTable().isEmpty())
            out.printf("%s- Constants:\n", indent);
        for (int i = 0; i < bytecode.constantTable().size(); i++){
            out.printf("%s  %4x: %s\n", indent, i, bytecode.constantTable().get(i));
        }
    }

}
/**  SCOPE CHECKING
 * TESTOVI ZA FUNKCIJE
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; numero i; replay(primer > broj2){i = broj1;} getback i;}  - ne treba da baci error
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; replay(primer > broj2){i = broj1;} getback i;} - baca error jer ne vidi i
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; replay(primer > broj2){} getback broj1;}  - ne treba da baci error
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; replay(primer > broj2){} getback nePostojecaVarijabla;} - treba da baci error
 *
 * TESTOVI ZA SPIN I REPLAY(ZA FOR I WHILE)
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan = vidan - i;}}
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan = vidan - marko;}}
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan =  marko;}}
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan = vidan - y;}} baca gresku ne nalazi y
 *
 * TESTOVI ZA CHECK I BACKUP
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan =  marko;}backup{marko = vidan;}}
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan =  marko;}backup{marko = i;}} ne nalazi i
 *
 * TESTOVI ZA DROPMSG
 * dropmsg(marko); ne nalazi marka
 * numero marko = 5; dropmsg(marko);
 *
 * TESTOVI ZA GRABMSG
 * grabmsg(vidan); ne nalazi vidana
 * numero vidan; grabmsg(vidan);
 *
 * TESTOVI ZA CALLFUNC
 * Definisana funkcija i poziv funkcije - action numero proba(numero broj1, numero broj2){ numero primer = broj1; numero i; replay(primer > broj2){i = broj1;} getback i;} proba(2,10);  -- ne izbacuje gresku
 * Definisana funkcija i poziv funkcije sa vise argumenata nego sto treba - action numero proba(numero broj1, numero broj2){ numero primer = broj1; numero i; replay(primer > broj2){i = broj1;} getback i;} proba(2,10,15,25,30);  -- izbacuje error zbog argumenata
 * Definisana funkcija i poziv funkcije sa manje argumenata nego sto treba - action numero proba(numero broj1, numero broj2){ numero primer = broj1; numero i; replay(primer > broj2){i = broj1;} getback i;} proba(2);  -- izbacuje error zbog argumenata
 * probaa(55); - izbacuje error jer funkcija nije nigde definisana
 * action numero proba(numero broj1, numero broj2){ numero marko = 1; getback marko; }action numero proba(numero broj1, numero broj2){numero marko = 1; getback marko;} --- izbacuje error jer je dva puta definisana funkcija sa istim nazivom
 * */



