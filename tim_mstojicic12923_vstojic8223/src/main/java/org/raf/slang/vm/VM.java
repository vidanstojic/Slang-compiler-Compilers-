package org.raf.slang.vm;

import org.raf.slang.Slang;

import java.util.ArrayList;


public class VM {
    private Slang slang;

    public VM(Slang context) {
        this.slang = context;
    }
    private int funcId = 0;
    private ArrayList<Value> globals = new ArrayList<>();

    public void run(BytecodeContainer bytecode) {


        int ip = 0;
        var callstack = new ArrayList<BytecodeContainerInvocation>();

        while (globals.size() < slang.getGlobalCount())
            globals.add(null);

        /* Prepare the outer invocation.  */
        callstack.add(new BytecodeContainerInvocation(bytecode));

        for (;;) {
            var frame = callstack.getLast();
            var stack = frame.getOperandStack();
          //  System.out.println("Velicina liste iz bytecode stack: "+ stack.size());
            var code = frame.getBytecodeContainer().code();
            var csts = frame.getBytecodeContainer().constantTable();
            var upvals = frame.getUpvalues();
            var locals = frame.getLocals();

            var insn = code.get(ip++);
            var op = insn.getOpcode();
            switch (op) {
                case BIT_PLUS, BIT_DIV, BIT_MUL, BIT_MINUS, BIT_CR -> {
                    var rhs = ((Value.Number) stack.getLast()).number();
                    stack.removeLast();
                    var lhs = ((Value.Number) stack.getLast()).number();
                    stack.removeLast();
                    stack.add(new Value.Number
                            (switch(op) {
                                case BIT_PLUS -> lhs + rhs;
                                case BIT_MINUS -> lhs - rhs;
                                case BIT_MUL -> lhs * rhs;
                                case BIT_DIV -> lhs / rhs;
                                case BIT_CR -> Math.pow(lhs, rhs);
                                default ->
                                        throw new IllegalArgumentException(op.name());
                            }));
                }
                case BIT_GT, BIT_LT, BIT_GTE, BIT_LTE, BIT_ET -> {
                    var rhs = ((Value.Number) stack.getLast()).number();
                    stack.removeLast();
                    var lhs = ((Value.Number) stack.getLast()).number();
                    stack.removeLast();
                    stack.add(new Value.Bool
                            (switch(op) {
                                case BIT_GT -> lhs > rhs;
                                case BIT_LT -> lhs < rhs;
                                case BIT_GTE -> lhs >= rhs;
                                case BIT_LTE -> lhs <= rhs;
                                case BIT_ET -> lhs == rhs;
                                default ->
                                        throw new IllegalArgumentException(op.name());
                            }));
                }

                case GET_GLOBAL ->
                        stack.add(globals.get((int)insn.getArg1()));
                case GET_LOCAL ->
                        stack.add(locals[(int)insn.getArg1()]);
                case GET_UPVALUE ->
                        stack.add(upvals[(int)insn.getArg1()]);
                case SET_GLOBAL -> {
                    globals.set((int)insn.getArg1(), stack.getLast());
                    stack.removeLast();
                }
                case SET_LOCAL -> {
                    locals[(int)insn.getArg1()] = stack.getLast();
                    stack.removeLast();
                }
                case COLLECT -> {
                    var cnt = /* Count.  */ insn.getArg1();
                    var elements = new ArrayList<>(stack.subList((int) (stack.size() - cnt),
                            stack.size()));
                    for (int i = 0; i < cnt; i++)
                        stack.removeLast();
                    stack.add(new Value.Vector(elements));
                }
                case PUSHI ->
                        stack.add(new Value.Number(csts.get((int)insn.getArg1())));
                case POP -> stack.removeLast();

                case FINISH_OUTER -> {
                    assert callstack.size() == 1;
                    assert ip == code.size();
                    return;
                }
                case PRINT -> {
                    var val = stack.getLast();
                    stack.removeLast();
                    val.print(System.out);
                    System.out.println();
                }

                case RET, RET_VOID -> {
                    final var retVoid = op == Instruction.Code.RET_VOID;
                /* In case this is a void function, we don't have anything to
                   return.  But, after each ExprStmt there's a POP.  A void
                   function call is necessarily nested in a ExprStmt due to it
                   being type-checked.  With these facts, we can safely insert
                   a Java null in place of a return value, or really any
                   arbitrary value, as it will never actually be read.  */
                    final var retval = retVoid ? null : stack.getLast();
                    ip = callstack.getLast().getPrevIp();

                    callstack.getLast().getOperandStack().add(retval);
                    callstack.removeLast();
                }
                case FUNCTION_CALL -> {
                    final var aty = insn.getArg1();
                    final var operands =
                            new ArrayList<>(stack.subList((int) (stack.size() - aty - 1),
                                    stack.size()));
                    final var closure = ((Value.Closure) operands.getFirst());

                    final var newLocals = new Value[closure.localCount()];
                    for (int i = 1; i < operands.size(); i++)
                        newLocals[i - 1] = operands.get(i);

                    final var invoc =
                            new BytecodeContainerInvocation(closure.code(),
                                    closure.upvalues(),
                                    newLocals,
                                    ip);
                    callstack.add(invoc);
                    ip = 0;
                    for (int i = 0; i < operands.size(); i++)
                        stack.removeLast();
                }
                case BUILD_CLOSURE -> {
                    var fn = slang.getFunction((int)insn.getArg1());
                    var um = fn.getUpvalueMap();
                    var newUpvalues = new Value[um.length];
                    for (int u = 0; u < newUpvalues.length; u++)
                        newUpvalues[u] =
                                (switch (um[u].loc()) {
                                    case UPVALUE -> upvals;
                                    case LOCAL -> locals;
                                })[um[u].slot()];
                    stack.add(new Value.Closure(fn.getCode(),
                            newUpvalues,
                            fn.getLocalCount()));
                }
                case PUSH_FUNC -> {
                    var fn = slang.getFunction((int)insn.getArg1());
                    var um = fn.getUpvalueMap();
                    stack.add(new Value.Closure(fn.getCode(),new Value[um.length],fn.getLocalCount()));
                }
                case PUSH_TRUE -> {
                    stack.add(new Value.Bool(true));
                }
                case PUSH_FALSE -> {
                    stack.add(new Value.Bool(false));
                }
                case JUMP_TRUE -> {
                    stack.add(new Value.Bool(true));
                }
                case SCAN -> {

                }case JUMP_FALSE -> {
                    stack.add(new Value.Bool(false));
                }
            }
        }
    }
}
