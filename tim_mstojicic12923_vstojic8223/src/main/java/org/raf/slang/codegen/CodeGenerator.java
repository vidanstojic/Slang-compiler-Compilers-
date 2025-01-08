package org.raf.slang.codegen;


/*
* Ova klasa ce kod sluziti za generisanje medjukoda, ona ce biti nalik javinom bytecode-u.
* */

import lombok.Getter;
import org.raf.slang.Slang;
import org.raf.slang.ast.*;
import org.raf.slang.vm.*;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Stack;


public class CodeGenerator {
    // ovde je potrebno da stoji compile funkcija

    private Stack<List<Integer>> continueStmts = new Stack<>();
    private Stack<List<Integer>> exitStmts = new Stack<>();

    private InTranslationBytecodeContainer bytecodeContainer = null;

    private final Slang slang;
  //  private final IdentityHashMap<FunctionDefinition, Integer> functionLUT;



    public CodeGenerator(Slang slang) {
        this.slang = slang;
        // this.functionLUT...
    }
    /** Compiles a single global scope statement list and produces a blob of
     code for it that the VM can interpret immediately.  Populates the
     function table if a function is declared within {@code input}.  */
    public BytecodeContainer compileInput(StatementList input) {
        assert !(slang.hadError() || slang.hadRuntimeError());
        /* This function should only be called for the global scope.  */
        assert bytecodeContainer == null;
        var outerBlob = new InTranslationBytecodeContainer(new BytecodeContainer(),
                new IdentityHashMap<>(),
                new IdentityHashMap<>(),
                bytecodeContainer);
        /* Push.  */
        bytecodeContainer = outerBlob;

        compileBlock(input);
        /* Used as a signal to our VM that we're done with the blob.  */
        emit(Instruction.Code.FINISH_OUTER);

        /* We must've come back down to the bottom of the stack.  */
        assert bytecodeContainer == outerBlob;
        bytecodeContainer = null;

        /* There can't possibly be any locals here.  */
        assert outerBlob.getMaxLocalDepth() == 0;
        return outerBlob.getCode();
    }

    private void compileBlock(StatementList input) {
        var currentBlob = bytecodeContainer;
        var oldLocalDepth = currentBlob.getLocalDepth();

        for (var statement : input.getListOfStatements())
            compileStatement(statement);

        bytecodeContainer.setLocalDepth(oldLocalDepth);
        assert currentBlob == bytecodeContainer;
    }


    private Instruction declareVariable(SimpleStatement declaration) {
        if (bytecodeContainer.getPreviousBlob() == null) {
            /* New global variable.  */
            return new Instruction(Instruction.Code.SET_GLOBAL, slang.declareGlobal(declaration));
        } else {
            var newVarId = bytecodeContainer.getLocalDepth();
            bytecodeContainer.setLocalDepth(newVarId + 1);
            var oldId = bytecodeContainer.getLocalSlots().put(declaration, newVarId);
            assert oldId == null : "how did you redeclare it??";
            return new Instruction(Instruction.Code.SET_LOCAL, newVarId);
        }
    }


    private void compileStatement(Statement statement) {
        switch(statement)
        {
            case SimpleStatement simpleStmt -> {
                var newVarSetter = declareVariable(simpleStmt);
                compileExpr(simpleStmt.getValue());
                emit(newVarSetter);
            }
            case IfStatement ifStmt -> {
                // ovde je napisano kako se izvrsava if u slucaju kada nema else
                // mi ne cuvamo nigde informacije o else unutar IfStmt pa zato ne mogu da pristupim tome i vidim da li postoji else
                for (Expr expr : ifStmt.getExprList()) compileExpr(expr);
                var skipThen = emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                emit(Instruction.Code.POP);
                for (Statement statementEl : ifStmt.getStatementList()) compileStatement(statementEl);
                var skipPop = emit(Instruction.Code.JUMP, Integer.MAX_VALUE);
                backpatch(skipThen);
                emit(Instruction.Code.POP);
                backpatch(skipPop);
            }
            case ElseStatement elseStmt -> {
                for(Statement statementEl : elseStmt.getStatementList()) {
                    compileStatement(statementEl);
                }
            }
            case LoopStatement loopStmt -> {
                var startIp = ip();
                for (Expr expr : loopStmt.getExprList()) compileExpr(expr);
                var skipBody = emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                emit(Instruction.Code.POP);

                continueStmts.push(new ArrayList<>());
                exitStmts.push(new ArrayList<>());

                for (Statement statementEl : loopStmt.getStatementList()) compileStatement(statementEl);
                emit(Instruction.Code.JUMP, jumpOffset(startIp));

                for (var stmt : exitStmts.pop()) backpatch(stmt);
                for (var stmt : continueStmts.pop()) backpatch(stmt, startIp);

                emit(Instruction.Code.POP);
            }
            case PrintStatement printStmt -> {
                printStmt.getArguments().forEach
                        (expr -> {
                            compileExpr(expr);
                            emit(Instruction.Code.PRINT);
                        });
            }
            case ScanStatement scanStmt -> {
                emit(Instruction.Code.SCAN);

                // Postavljamo rezultat unosa u promenljivu
                var newVarSetter = declareVariable(new SimpleStatement(
                        scanStmt.getLocation(),
                        scanStmt.getName(),
                        null, scanStmt.getVariableType())
                );
                emit(newVarSetter);
            }
            case FunctionDefinition functionDefinition -> {
                VariableType variableType = new VariableType(functionDefinition.getLocation(), functionDefinition.getFunctionReturnType()) {
                    @Override
                    public String userReadableName() {
                        return functionDefinition.getFunctionReturnType();
                    }
                };
                var newVarSetter = declareVariable(new SimpleStatement(functionDefinition.getLocation(), functionDefinition.getName(), null,variableType));
                var fnId = compileFunction(functionDefinition);
                emit(Instruction.Code.BUILD_CLOSURE, fnId);
                emit(newVarSetter);
            }
            case FunctionCallStatement functionCallStatement -> {
              //  Expr expr = new Expr(functionCallStatement.getLocation()); // IZMENITI
                //compileExpr(expr);
                functionCallStatement.getArguments().forEach(this::compileExpr);
                emit(Instruction.Code.FUNCTION_CALL, functionCallStatement.getArguments().size());
            }
            case ArrayStatement arrayStmt -> {
                arrayStmt.getElements().getElements().forEach(this::compileExpr);
                emit(Instruction.Code.COLLECT, arrayStmt.getElements().getElements().size());
            }
            case StatementList listStmt -> {
                compileBlock(listStmt);
            }
        }
    }

    private int compileFunction(FunctionDefinition fn) {
        var function = new Function();
        function.setFuncDef(fn);
        var functionBlob = new InTranslationBytecodeContainer(new BytecodeContainer(),
                new IdentityHashMap<>(),// bytecodeContainer.getLocalSlots
                new IdentityHashMap<>(),
                bytecodeContainer);
        bytecodeContainer = functionBlob;
        var newFnId = slang.addFunction(function);

        /* Declare variables.  */
        for (FunctionParameter functionParameter : fn.getParameters()) {
            SimpleStatement simpleStatementFunctionPar = new SimpleStatement(functionParameter.getLocation(),functionParameter.getParamName(), functionParameter.getValueOfParameter(),functionParameter.getType());
            declareVariable(simpleStatementFunctionPar);
        }

        /* Compile body.  */
        compileBlock(new StatementList(fn.getLocation(), fn.getStatementList()));

        /* Populate the function data.  */
        function.setCode(functionBlob.getCode());
        var upvals = new UpvalueMapEntry[functionBlob.getUpvalSlots().size()];
        function.setUpvalueMap(upvals);
        function.setLocalCount(functionBlob.getMaxLocalDepth());
        functionBlob.getUpvalSlots()
                .values()
                .forEach(s -> { upvals[s.slotNr()] = s.entry(); });

        /* Pop.  */
        bytecodeContainer = bytecodeContainer.getPreviousBlob();

        if (fn.getValueOfReturnData() != null) {
            compileExpr(fn.getValueOfReturnData());
            emit(Instruction.Code.RET);
        } else
            emit(Instruction.Code.RET_VOID);


        return newFnId;
    }



    private Instruction findLocalInsn(InTranslationBytecodeContainer blob,
                                      SimpleStatement decl) {
        /* We already checked globals in getVarInsn.  */

        var locals = blob.getLocalSlots();
        var upvals = blob.getUpvalSlots();
        assert locals != null && upvals != null;

        var local = locals.get(decl);
        if (local != null)
            return new Instruction(Instruction.Code.GET_LOCAL, local);

        /* So, this is a upvalue.  But is it new?  */
        var upval = blob.getUpvalSlots().get(decl);
        if (upval != null)
            /* No, it isn't.  */
            return new Instruction(Instruction.Code.GET_UPVALUE, upval.slotNr());

        /* It is.  */
        if(blob.getPreviousBlob() == null)
            return new Instruction(Instruction.Code.GET_UPVALUE, blob.getUpvalSlots().size());
        var inSuperscope = findLocalInsn(blob.getPreviousBlob(), decl);
        var upvalSlot = blob.getUpvalSlots().size();

        var upvalME = new UpvalueMapEntry(switch (inSuperscope.getOpcode()) {
            case Instruction.Code.GET_LOCAL -> UpvalueMapEntry.UpvalueLocation.LOCAL;
            case Instruction.Code.GET_UPVALUE  -> UpvalueMapEntry.UpvalueLocation.UPVALUE;
            default -> throw new IllegalArgumentException();
        }, Math.toIntExact(inSuperscope.getArg1()));

        var oldSlot = blob.getUpvalSlots()
                .put(decl, new InTranslationBytecodeContainer.UpvalSlotInfo(upvalSlot, upvalME));
        assert oldSlot == null;
        return new Instruction(Instruction.Code.GET_UPVALUE, upvalSlot);
    }


    private Instruction getVarInsn(SimpleStatement decl) {
        return slang.getGlobalSlot(decl)
                .map(s -> new Instruction(Instruction.Code.GET_GLOBAL, s))
                .orElseGet(() -> findLocalInsn(bytecodeContainer, decl));
    }


    private void compileExpr(Expr expr) {


        switch (expr) {
            case ErrorExpr ignored -> throw new IllegalStateException();
            case BoolLiteral boolLiteral -> {
                if(boolLiteral.isBool()) emit(Instruction.Code.PUSH_TRUE, Integer.MAX_VALUE);
                else emit(Instruction.Code.PUSH_FALSE, Integer.MAX_VALUE);
            }
            case NumberLiteral numberLiteral -> {
                var constantNumber = bytecodeContainer.getCode().constantTable().size();
                bytecodeContainer.getCode().constantTable().add(numberLiteral.getValue());
                emit(Instruction.Code.PUSHI, constantNumber);
            }
            case ArrayLiteral arrayLiteral -> {
                arrayLiteral.getElements().forEach(this::compileExpr);
                emit(Instruction.Code.COLLECT, arrayLiteral.getElements().size());
            }
            case VariableRef variable -> {
                emit(getVarInsn(variable.getVariable()));
            }
            case Expr binaryExpr -> {
                switch (binaryExpr.getOperation()) {
                    case ADD, SUB, MUL, DIV, MOD, CARET, GREATERTHAN, LESSTHAN,
                         EQUALTO, LESSTHANOREQ, GREATERTHANOREQ, BITAND, BITOR, BANG -> {

                        var opsLhs = expr.getLhs();
                        var opsRhs = expr.getRhs();
                        compileExpr(opsLhs);
                        compileExpr(opsRhs);
                        emit(switch (binaryExpr.getOperation()) {
                            case ADD -> Instruction.Code.BIT_PLUS;
                            case SUB -> Instruction.Code.BIT_MINUS;
                            case MUL -> Instruction.Code.BIT_MUL;
                            case DIV -> Instruction.Code.BIT_DIV;
                            case CARET -> Instruction.Code.BIT_CR;
                            case GREATERTHAN -> Instruction.Code.BIT_GT;
                            case LESSTHAN -> Instruction.Code.BIT_LT;
                            case LESSTHANOREQ -> Instruction.Code.BIT_LTE;
                            case EQUALTO -> Instruction.Code.BIT_ET;
                            case GREATERTHANOREQ -> Instruction.Code.BIT_GTE;
                            case BANG -> Instruction.Code.NEGATE;
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + binaryExpr.getOperation());
                        });
                    }
                    case AND, OR -> {
                        var opsLhs = expr.getLhs();
                        var opsRhs = expr.getRhs();
                        var operation = expr.getOperation();
                        compileExpr(opsLhs);
                        var skipOther = emit(operation == Expr.Operation.AND
                                ? Instruction.Code.JUMP_FALSE : Instruction.Code.JUMP_TRUE, Integer.MAX_VALUE);
                        emit(Instruction.Code.POP);
                        compileExpr(opsRhs);
                        backpatch(skipOther);
                    }
                    // treba poseban case za BANG
                    default -> throw new IllegalStateException("Unexpected value: " + binaryExpr.getOperation());
                }
            }

        }
    }




    private void backpatch(int instructionIndex) {
        backpatch(instructionIndex, ip());
    }

    private void backpatch(int instructionIndex, int to) {
        var instruction = bytecodeContainer.getCode().code().get(instructionIndex);
        assert List.of(Instruction.Code.JUMP, Instruction.Code.JUMP_TRUE, Instruction.Code.JUMP_FALSE)
                .contains(instruction.opcode());
        var relIp = jumpOffset(to, instructionIndex);
        instruction.setArg1(relIp);
    }

    private int jumpOffset(int to) {
        return jumpOffset(to, ip());
    }

    private int jumpOffset(int to, int from) {
        return to - (from + 1);
    }

    private int emit(Instruction.Code instructionCode, long arg1) {
        return emit(new Instruction(instructionCode, arg1));
    }

    private int emit(Instruction.Code instructionCode) {
        return emit(new Instruction(instructionCode));
    }

    private int emit(Instruction insn) {
        return bytecodeContainer.getCode().addInsn(insn);
    }

    private int ip() {
        return bytecodeContainer.getCode().code().size();
    }
/*
    private Instruction getVarInsn(SimpleStatement decl) {
        return slang.getGlobalSlot(decl)
                .map(s -> new Instruction(Instruction.Code.GET_GLOBAL, s))
                .orElseGet(() -> findLocalInsn(blob, decl));
    }

*/
}
/*numero br = 1; action numero pr(yeahNah flag, numero x, numero y){numero i = 0; check(flag == true){i = x;}backup{i = y;}getback i;} pr(true, 1, 2);*/