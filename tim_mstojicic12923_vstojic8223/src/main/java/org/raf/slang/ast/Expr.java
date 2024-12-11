package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Expr extends Tree{

    public enum Operation {
        ADD("+"),
        SUB("-"),
        MUL("*"),
        DIV("/"),
        MOD("%"),
        CARET("^"),
        BITOR("|"),
        BITAND("&"),
        OR("||"),
        AND("&&"),
        NOTEQUAL ("!="),
        GREATERTHAN(">"),
        LESSTHAN("<"),
        BANG("!"),
        EQUALTO("=="),
        LESSTHANOREQ("<="),
        GREATERTHANOREQ(">="),

        /** A vector or a number or a variable.  */
        VALUE(null),
        ;

        public final String label;

        Operation(String label) {
            this.label = label;
        }
    }

    private Operation operation;
    private Expr lhs;
    private Expr rhs;

    public Expr(Location location, Operation operation, Expr lhs, Expr rhs) {
        super(location);
        if (operation == Operation.VALUE)
            throw new IllegalArgumentException("cannot construct a value like that");
        this.operation = operation;
        this.lhs = Objects.requireNonNull(lhs);
        this.rhs = Objects.requireNonNull(rhs);
    }

    protected Expr(Location location)
    {
        super(location);
        this.operation = Operation.VALUE;
    }

    protected Expr(Location location, Expr expr,Operation operation)
    {
        super(location);
        this.lhs = expr;
        this.operation = operation;
    }

    @Override
    public void nodePrint(ASTNodePrinter pp) {
        pp.node(operation.label,
                () -> {
                    lhs.nodePrint(pp);
                    rhs.nodePrint(pp);
                });
    }
}
