package org.raf.slang.codegen;


/*
* Ova klasa ce kod sluziti za generisanje medjukoda, ona ce biti nalik javinom bytecode-u.
* */

import org.raf.slang.Slang;
import org.raf.slang.ast.*;
import org.raf.slang.vm.*;

import java.util.*;

//numero br = 1; action numero pr(yeahNah flag, numero x, numero y){numero i = 0; check(flag == true){i = x;}backup{i = y;}getback i;} pr(true, 1, 2);
public class CodeGenerator {
    // ovde je potrebno da stoji compile funkcija

    private Stack<List<Integer>> continueStmts = new Stack<>();
    private Stack<List<Integer>> exitStmts = new Stack<>();
    private HashMap<String, FunctionDefinition> functionDefMap = new HashMap<>();
    private HashMap<String, Function> functionMap = new HashMap<>();

    private InTranslationBytecodeContainer bytecodeContainer = null;
    private StatementList statementList;
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
        statementList = input;
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
                compileExpr(simpleStmt.getValue());//mozda je ovde potrebno da se ispravi value i da se setuje ako postoji Expr sa operands
                emit(newVarSetter);
            }
            case IfStatement ifStmt -> {
                // ovde je napisano kako se izvrsava if u slucaju kada nema else
                // mi ne cuvamo nigde informacije o else unutar IfStmt pa zato ne mogu da pristupim tome i vidim da li postoji else
                Expr expr = ifStmt.getExprList().get(0);
                ElseStatement elseStatement = null;
                for (Statement statement1 : statementList.getListOfStatements()){
                    if (statement1 instanceof ElseStatement){
                        elseStatement = (ElseStatement) statement1;
                        break;
                    }
                }
                compileExpr(expr);
//                for (Expr expr : ifStmt.getExprList()) compileExpr(expr);
                BoolType boolType = null;
                if (expr.getResultType() instanceof BoolType)
                    boolType = (BoolType) expr.getResultType();
                if (boolType.isBool())
                    emit(Instruction.Code.JUMP_TRUE, Integer.MAX_VALUE);
                else {
                    emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                    if (elseStatement != null){
                        compileStatement(elseStatement);
                        statementList.getListOfStatements().remove(elseStatement);
                    }
                }
                var skipThen = emit(boolType.isBool() ? Instruction.Code.JUMP_TRUE : Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
//                var skipThen = emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                emit(Instruction.Code.POP);
                InTranslationBytecodeContainer ifBlob = null;
                if (!ifStmt.getStatementList().isEmpty()){
                    ifBlob = new InTranslationBytecodeContainer(new BytecodeContainer(),
                            new IdentityHashMap<>(),// bytecodeContainer.getLocalSlots
                            new IdentityHashMap<>(),
                            bytecodeContainer);
                    bytecodeContainer = ifBlob;
                }
                for (Statement statementEl : ifStmt.getStatementList()) compileStatement(statementEl);
                /**POP**/
                if (ifBlob != null) bytecodeContainer = bytecodeContainer.getPreviousBlob();
                var skipPop = emit(Instruction.Code.JUMP, Integer.MAX_VALUE);
                backpatch(skipThen);
                emit(Instruction.Code.POP);
                backpatch(skipPop);
            }
            case ElseStatement elseStmt -> {
                InTranslationBytecodeContainer elseBlob = null;
                if (!elseStmt.getStatementList().isEmpty()){
                    elseBlob = new InTranslationBytecodeContainer(new BytecodeContainer(),
                            new IdentityHashMap<>(),// bytecodeContainer.getLocalSlots
                            new IdentityHashMap<>(),
                            bytecodeContainer);
                    bytecodeContainer = elseBlob;
                }
                for(Statement statementEl : elseStmt.getStatementList()) {
                    compileStatement(statementEl);
                }
                /**POP**/
                if (elseBlob != null) bytecodeContainer = bytecodeContainer.getPreviousBlob();
            }
            case LoopStatement loopStmt -> {
                var startIp = ip();
                if (loopStmt.getIterator() != null)
                    declareVariable(loopStmt.getIterator());
                for (Expr expr : loopStmt.getExprList()) compileExpr(expr);
                var skipBody = emit(Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                emit(Instruction.Code.POP);

                continueStmts.push(new ArrayList<>());
                exitStmts.push(new ArrayList<>());

                InTranslationBytecodeContainer loopBlob = null;
                if (!loopStmt.getStatementList().isEmpty()){
                    loopBlob = new InTranslationBytecodeContainer(new BytecodeContainer(),
                            new IdentityHashMap<>(),// bytecodeContainer.getLocalSlots
                            new IdentityHashMap<>(),
                            bytecodeContainer);
                    bytecodeContainer = loopBlob;
                }
                for (Statement statementEl : loopStmt.getStatementList()) compileStatement(statementEl);
                /**POP**/
                if (loopBlob != null) bytecodeContainer = bytecodeContainer.getPreviousBlob();
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
                var function = new Function();
                function.setFuncDef(functionDefinition);
                var fnId = slang.addFunction(function);

                emit(Instruction.Code.BUILD_CLOSURE, fnId);
                functionMap.put(functionDefinition.getName(), function);

//                var function = new Function();
//                function.setFuncDef(functionDefinition);
//                var fnId = slang.addFunction(function);
                functionDefMap.put(functionDefinition.getName(), functionDefinition);
                //    var fnId = compileFunction(functionDefinition);
//                emit(Instruction.Code.BUILD_CLOSURE, fnId);
                emit(newVarSetter);
                emit(Instruction.Code.PUSH_FUNC, fnId);
            }
            case FunctionCallStatement functionCallStatement -> {
              //  Expr expr = new Expr(functionCallStatement.getLocation()); // IZMENITI
                //compileExpr(expr);
                functionCallStatement.getArguments().forEach(this::compileExpr);
                if (functionDefMap.containsKey(functionCallStatement.getName()))
                    compileFunction(functionDefMap.get(functionCallStatement.getName()));
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

    private void compileFunction(FunctionDefinition fn) {
        Function function = null;
        if (functionMap.get(fn.getName())!= null)
            function = functionMap.get(fn.getName());
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

        if (fn.getValueOfReturnData() != null) {
            compileExpr(fn.getValueOfReturnData());
            emit(Instruction.Code.RET);
        } else
            emit(Instruction.Code.RET_VOID);
        /* Pop.  */
        bytecodeContainer = bytecodeContainer.getPreviousBlob();


    }



    private Instruction findLocalInsn(InTranslationBytecodeContainer blob,
                                      SimpleStatement decl) {
        /* We already checked globals in getVarInsn.  */

        var locals = blob.getLocalSlots();
        var upvals = blob.getUpvalSlots();
        assert locals != null && upvals != null;

        Integer local = null;
        SimpleStatement localSimpleStatement = null;
        for (SimpleStatement simpleStatementEl : locals.keySet()){
            if(simpleStatementEl.getName().equals(decl.getName())){
                local = locals.get(simpleStatementEl);
                localSimpleStatement = simpleStatementEl;
                break;
            }
        }
        if (local != null)
            return new Instruction(Instruction.Code.GET_LOCAL, local);

        /* So, this is a upvalue.  But is it new?  */
        var upval = blob.getUpvalSlots().get(localSimpleStatement);
        if (upval != null)
            /* No, it isn't.  */
            return new Instruction(Instruction.Code.GET_UPVALUE, upval.slotNr());

//        /* It is.  */
        var inSuperscope = findLocalInsn(blob.getPreviousBlob(), decl);
        var upvalSlot = blob.getUpvalSlots().size();

        var upvalME = new UpvalueMapEntry(switch (inSuperscope.getOpcode()) {
            case Instruction.Code.GET_LOCAL -> UpvalueMapEntry.UpvalueLocation.LOCAL;
            case Instruction.Code.GET_UPVALUE  -> UpvalueMapEntry.UpvalueLocation.UPVALUE;
            default -> throw new IllegalArgumentException();
        }, Math.toIntExact(inSuperscope.getArg1()));

        var oldSlot = blob.getUpvalSlots()
                .put(localSimpleStatement, new InTranslationBytecodeContainer.UpvalSlotInfo(upvalSlot, upvalME));
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
                    case ADD, SUB, MUL, DIV, MOD, CARET-> {
                        for (Expr expr1 : binaryExpr.getOperands())
                            compileExpr(expr1);
                        emit(switch (binaryExpr.getOperation()) {
                            case ADD -> Instruction.Code.BIT_PLUS;
                            case SUB -> Instruction.Code.BIT_MINUS;
                            case MUL -> Instruction.Code.BIT_MUL;
                            case DIV -> Instruction.Code.BIT_DIV;
                            case CARET -> Instruction.Code.BIT_CR;
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + binaryExpr.getOperation());
                        });
                    }
                    case GREATERTHAN, LESSTHAN,
                            LESSTHANOREQ, GREATERTHANOREQ -> {
                        relationalOperands(binaryExpr);
                        emit(switch (binaryExpr.getOperation()) {
                            case GREATERTHAN -> Instruction.Code.BIT_GT;
                            case LESSTHAN -> Instruction.Code.BIT_LT;
                            case LESSTHANOREQ -> Instruction.Code.BIT_LTE;
                            case GREATERTHANOREQ -> Instruction.Code.BIT_GTE;
                            default ->
                                    throw new IllegalStateException("Unexpected value: " + binaryExpr.getOperation());
                        });
                    }
                    case NOT_EQUALS, EQUALTO -> {
                        int flag = 0;
                        if (binaryExpr.getOperands()!= null){
                            for (Expr expr1: binaryExpr.getOperands()) {
                                if (expr1.getResultType().equals(slang.getBoolType())){
                                    flag = 1;
                                    break;
                                }
                            }
                            if (flag == 1){
                                trueOrFalse(binaryExpr);
                            }else {
                                relationalOperands(binaryExpr);
                            }
                        }else {
                            if (binaryExpr.getResultType().equals(slang.getBoolType()))
                                trueOrFalse(binaryExpr);
                            else relationalOperands(binaryExpr);
                        }
                    }
                    case BANG -> {
                        BoolLiteral boolExpr = (BoolLiteral) expr.getOperands().get(0);
                        boolExpr.setBool(!boolExpr.isBool());
                        compileExpr(boolExpr);
                        emit(Instruction.Code.NEGATE);
                    }
                    case AND, OR -> {
                        var returnType = (BoolLiteral)trueOrFalse(expr);
                        System.out.println(returnType);
                        var operation = expr.getOperation();
                        var skipOther = emit(returnType.isBool()
                                    ? Instruction.Code.JUMP_TRUE : Instruction.Code.JUMP_FALSE, Integer.MAX_VALUE);
                        backpatch(skipOther);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + binaryExpr.getOperation());
                }
            }

        }
    }
    private Expr trueOrFalse(Expr expr){
        if (expr.getOperation().equals(Expr.Operation.AND)){
            Expr lhs = expr.getOperands().get(0);
            if (lhs.getOperands() != null)
                lhs = trueOrFalse(lhs);
            Expr rhs = expr.getOperands().get(1);
            if (rhs.getOperands() != null)
                rhs = trueOrFalse(rhs);
            if (rhs instanceof BoolLiteral && lhs instanceof BoolLiteral){
                BoolLiteral boolRhs = (BoolLiteral) rhs;
                BoolLiteral boolLhs = (BoolLiteral) lhs;
                boolean rhsValue = boolRhs.isBool();
                boolean lhsValue = boolLhs.isBool();
                if (rhsValue == lhsValue)
                    return boolLhs;
                else {
                    if (!lhsValue)
                        return boolLhs;
                    else
                        return boolRhs;
                }
            }
        }else if (expr.getOperation().equals(Expr.Operation.OR)){
            Expr lhs = expr.getOperands().get(0);
            if (lhs.getOperands() != null)
                lhs = trueOrFalse(lhs);
            Expr rhs = expr.getOperands().get(1);
            if (rhs.getOperands() != null)
                rhs = trueOrFalse(rhs);
            if (rhs instanceof BoolLiteral && lhs instanceof BoolLiteral){
                BoolLiteral boolRhs = (BoolLiteral) rhs;
                BoolLiteral boolLhs = (BoolLiteral) lhs;
                boolean rhsValue = boolRhs.isBool();
                boolean lhsValue = boolLhs.isBool();
                if (rhsValue || lhsValue){
                    if (rhsValue) return boolRhs;
                    else return boolLhs;
                }
                else return boolRhs;
            }
        } else if (expr.getOperation().equals(Expr.Operation.EQUALTO)) {
            Expr lhs = expr.getOperands().get(0);
            if (lhs.getOperands() != null)
                lhs = trueOrFalse(lhs);
            Expr rhs = expr.getOperands().get(1);
            if (rhs.getOperands() != null)
                rhs = trueOrFalse(rhs);
            if (rhs instanceof BoolLiteral && lhs instanceof BoolLiteral){
                BoolLiteral boolRhs = (BoolLiteral) rhs;
                BoolLiteral boolLhs = (BoolLiteral) lhs;
                boolean rhsValue = boolRhs.isBool();
                boolean lhsValue = boolLhs.isBool();
                if (rhsValue == lhsValue){
                    if (rhsValue) return boolRhs;
                    else return boolLhs;
                }
                else return boolRhs;
            }
        } else if (expr.getOperation().equals(Expr.Operation.NOT_EQUALS)) {
            Expr lhs = expr.getOperands().get(0);
            if (lhs.getOperands() != null)
                lhs = trueOrFalse(lhs);
            Expr rhs = expr.getOperands().get(1);
            if (rhs.getOperands() != null)
                rhs = trueOrFalse(rhs);
            if (rhs instanceof BoolLiteral && lhs instanceof BoolLiteral){
                BoolLiteral boolRhs = (BoolLiteral) rhs;
                BoolLiteral boolLhs = (BoolLiteral) lhs;
                boolean rhsValue = boolRhs.isBool();
                boolean lhsValue = boolLhs.isBool();
                if (rhsValue != lhsValue){
                    if (rhsValue) return boolRhs;
                    else return boolLhs;
                }
                else return boolRhs;
            }
        }
        return expr;
    }

    private Expr relationalOperands(Expr expr){
        VariableRef varLhs = null;
        VariableRef varRhs = null;
        NumberLiteral numberLhs = null;
        NumberLiteral numberRhs = null;
        if (expr.getOperands().get(0) instanceof VariableRef){
            varLhs = (VariableRef)expr.getOperands().get(0);
            numberLhs = (NumberLiteral) varLhs.getVariable().getValue();
        } else numberLhs = (NumberLiteral)expr.getOperands().get(0);
        if (expr.getOperands().get(1) instanceof VariableRef){
            varRhs = (VariableRef)expr.getOperands().get(1);
            numberRhs = (NumberLiteral) varRhs.getVariable().getValue();
        } else numberRhs = (NumberLiteral)expr.getOperands().get(1);
        switch (expr.getOperation()) {
            case GREATERTHAN -> {
                if (numberLhs.getValue() > numberRhs.getValue()){
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(true);
                    expr.setResultType(boolType);
                }else {
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(false);
                    expr.setResultType(boolType);
                }
            }
            case LESSTHAN -> {
                if (numberLhs.getValue() < numberRhs.getValue()){
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(true);
                    expr.setResultType(boolType);
                }else {
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(false);
                    expr.setResultType(boolType);
                }
            }
            case LESSTHANOREQ -> {
                if (numberLhs.getValue() <= numberRhs.getValue()){
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(true);
                    expr.setResultType(boolType);
                }else {
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(false);
                    expr.setResultType(boolType);
                }
            }
            case EQUALTO -> {
                if (numberLhs.getValue() == numberRhs.getValue()){
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(true);
                    expr.setResultType(boolType);
                }else {
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(false);
                    expr.setResultType(boolType);
                }
            }
            case GREATERTHANOREQ -> {
                if (numberLhs.getValue() >= numberRhs.getValue()){
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(true);
                    expr.setResultType(boolType);
                }else {
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(false);
                    expr.setResultType(boolType);
                }
            }
            case NOT_EQUALS -> {
                if (numberLhs.getValue() != numberRhs.getValue()){
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(true);
                    expr.setResultType(boolType);
                }else {
                    BoolType boolType = new BoolType(expr.getLocation(), "yeahNah");
                    boolType.setBool(false);
                    expr.setResultType(boolType);
                }
            }
            default ->
                    throw new IllegalStateException("Unexpected value: " + expr.getOperation());
        }
        return expr;
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
//    private int emit(Instruction.Code instructionCode, FunctionDefinition arg1) {
//        return emit(new Instruction(instructionCode, arg1));
//    }

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
// src\main\java\org\raf\slang\resources\promenljive.txt
/*numero br = 1; action numero pr(yeahNah flag, numero x, numero y){numero i = 0; check(flag == true){i = x;}backup{i = y;}getback i;} pr(true, 1, 2);*/