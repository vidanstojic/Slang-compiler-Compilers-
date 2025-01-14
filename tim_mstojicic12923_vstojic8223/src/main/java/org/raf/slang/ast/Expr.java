package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Expr extends Tree{

    public enum Operation {
        ADD("+", 2),
        SUB("-", 2),

        MUL("*", 2),
        DIV("/", 2),
        MOD("%", 2),
        CARET("^", 2),
        NEGATE("-", 1),
        BANG("!", 1),
        ASSIGN("=", 2),
        OR("||", 2),
        AND("&&", 2),
        EQUALTO("==", 2),
        NOT_EQUALS("!=", 2),
        LESSTHAN("<", 2),
        LESSTHANOREQ("<=", 2),
        GREATERTHAN(">", 2),
        GREATERTHANOREQ(">=", 2),

        /** A vector or a number or a variable.  */
        VALUE(null, 0),
        ;

        @Getter
        public final String label;
        @Getter
        private final int opCount;

        Operation(String label, int operationCount) {
            this.label = label;
            this.opCount = operationCount;
        }
    }

    private Operation operation;
    private List<Expr> operands;
    private Expr rhs;

    public Expr(Location location, Operation operation, List<Expr> operands) {
        super(location);
        operands.forEach (Objects::requireNonNull);
        assert ((operands.size () == operation.getOpCount ())
                || (operation.getOpCount () <= 0
                && operands.size () >= -operation.getOpCount ()))
                : "Wrong operand count";

        this.operation = operation;
        this.operands = new ArrayList<>(operands);
    }

    protected Expr(Location location)
    {
        super(location);
        this.operation = Operation.VALUE;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node(operation.label,
                () -> {
                    operands.forEach (x -> x.nodePrint (pp));
                });
    }
}
