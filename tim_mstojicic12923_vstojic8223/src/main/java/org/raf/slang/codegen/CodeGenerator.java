package org.raf.slang.codegen;


/*
* Ova klasa ce kod sluziti za generisanje medjukoda, ona ce biti nalik javinom bytecode-u.
* */

import lombok.Getter;
import org.raf.slang.Slang;
import org.raf.slang.ast.*;
import org.raf.slang.vm.Instruction;
import org.raf.slang.vm.IpInstruction;
import org.raf.slang.vm.UpvalueMapEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class CodeGenerator {
    // ovde je potrebno da stoji compile funkcija

    @Getter
    private final ArrayList<Instruction> instructions = new ArrayList<>(); // stek na koji stavljamo instrukcije
    private Stack<List<IpInstruction>> continueStmts = new Stack<>();
    private Stack<List<IpInstruction>> exitStmts = new Stack<>();

    private InTranslationBytecodeContainer bytecodeContainer;

    private final Slang slang;
  //  private final IdentityHashMap<FunctionDefinition, Integer> functionLUT;



    private CodeGenerator(Slang slang) {
        this.slang = slang;
        // this.functionLUT...
    }



    private void compileStatement(Statement statement) {
        switch(statement)
        {
            case SimpleStatement simpleStmt -> {
                compileSimpleStatement(simpleStmt);
                 emit(Instruction.Code.POP);
            }
            case IfStatement ifStmt -> {
                // ovde je napisano kako se izvrsava if u slucaju kada nema else
                // mi ne cuvamo nigde informacije o else unutar IfStmt pa zato ne mogu da pristupim tome i vidim da li postoji else


                // compileExpr za condition
                for(Expr expr : ifStmt.getExprList()) compileExpr(expr);
                var skipThen = emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                emit(Instruction.Code.POP);
                for(Statement statementEl : ifStmt.getStatementList()) compileStatement(statementEl);
                var skipPop = emit(Instruction.Code.JUMP, Integer.MAX_VALUE);
                backpatch(skipThen);
                emit(Instruction.Code.POP);
                backpatch(skipPop);
            }
            case ElseStatement elseStmt -> {}
            case LoopStatement loopStmt -> {
                // Ovo je logika samo za while, treba razdvojiti logiku za for i za while
                var startIp = ip();
                for(Expr expr : loopStmt.getExprList()) compileExpr(expr);
                var skipBody = emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                emit(Instruction.Code.POP);
                // dodati continue i breakStmts
            }
            case PrintStatement printStmt -> {
                printStmt.getArguments().forEach
                        (expr -> {
                            compileExpr(expr);
                            emit(Instruction.Code.PRINT);
                        });
            }
            case ScanStatement scanStmt -> {}
            case FunctionDefinition functionDefinition -> {}
            case FunctionCallStatement functionCallStatement -> {}
            case ArrayStatement arrayStmt -> {}
            case StatementList listStmt -> {}
        }
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


    private void compileSimpleStatement(SimpleStatement simpleStatement) {
        compileExpr(simpleStatement.getValue());
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
    private IpInstruction emit(Instruction.Code instructionCode, long arg1) {
        var insn = new Instruction(instructionCode, arg1);
        var ip = instructions.size();
        instructions.add(insn);
        return new IpInstruction(ip, insn);
    }
    private IpInstruction emit(Instruction.Code instructionCode) {
        var insn = new Instruction(instructionCode);
        var ip = instructions.size();
        instructions.add(insn);
        return new IpInstruction(ip, insn);
    }
    private int emit(Instruction insn) {
        return bytecodeContainer.getCode().addInsn(insn);
    }

    private int ip(){
        return instructions.size(); // vraca IP od sledece instrukcije
    }


    private void backpatch(IpInstruction ipInstruction) {
        backpatch(ipInstruction, ip());
    }

    private void backpatch(IpInstruction ipInstruction, int to) {
        assert(List.of(Instruction.Code.JUMP, Instruction.Code.JUMP_TRUE, Instruction.Code.JUMP_FALSE)
                .contains(ipInstruction.getInstruction().opcode()));
        var relIp = jumpOffset(to, ipInstruction.getIndexInStack());
        ipInstruction.setIndexInStack(relIp);
    }

    private int jumpOffset(int to){return jumpOffset(to, ip());}

    private int jumpOffset(int to, int from){
        return to - (from + 1);
    }
/*
    private Instruction getVarInsn(SimpleStatement decl) {
        return slang.getGlobalSlot(decl)
                .map(s -> new Instruction(Instruction.Code.GET_GLOBAL, s))
                .orElseGet(() -> findLocalInsn(blob, decl));
    }

*/
}
