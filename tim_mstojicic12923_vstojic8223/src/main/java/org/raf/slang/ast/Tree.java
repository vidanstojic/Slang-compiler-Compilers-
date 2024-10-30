package org.raf.slang.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public abstract class Tree {
    private Location location;
    public Tree(Location location) {
        this.location = location;
    }


}
