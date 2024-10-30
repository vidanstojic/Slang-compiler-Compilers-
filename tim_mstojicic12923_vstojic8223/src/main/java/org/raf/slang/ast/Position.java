package org.raf.slang.ast;

public record Position(int line, int column) {
    public boolean lessThan(Position other) {
        if (other.line < this.line)
            return true;
        if (other.line == this.line)
            return other.column < this.column;

        /* Necessarily on a later line, hence greater.  */
        return false;
    }
}
