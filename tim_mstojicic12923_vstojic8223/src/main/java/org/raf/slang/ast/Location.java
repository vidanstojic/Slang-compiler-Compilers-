package org.raf.slang.ast;

public record Location(Position start, Position end) {
    public Location span(Location other) {
        var start = other.start().lessThan(start()) ? other.start() : start();
        var stop = other.end().lessThan(end()) ? end() : other.end();

        return new Location(start, stop);
    }
}
