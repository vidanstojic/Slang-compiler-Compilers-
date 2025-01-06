package org.raf.slang.vm;

/*
* Posto cemo na steku cuvati zapravo instrukcije koje je potrebno da izvrsimo, moramo imati klasu
* za instrukcije(Instructions/cesto se skraceno pise i insn).
* */

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public final class Instruction {
    // unutar code cemo bukvalno imati svaku komandu koja je moguca u nasem jeziku
    public enum Code{
        POP,
        PUSH_TRUE,// na stek stavljamo vrednost true
        PUSH_FALSE,// na stek stavljamo vrednost false
        PUSHI (1),// dodajemo neku brojcanu vrednost na stek
        PUSH_FUNC (1), // dodajemo pokazivac neke funkcije na stek
        RET,// signalizira kraj izvrsavanja neke funkcije
        CALL (1), // ako imamo npr f(a,b,c) onda ceo reci CALL(3) jer pozivamo funkciju sa 3 parametra
        SET_LOCAL (1),// ako imamo npr. x=5 onda treba da setujemo prom x vrednost
        GET_LOCAL(1), // uzimanje neke promenljive sa steka, ako imamo npr print(x);
       // Sledece stavke su za nizove
        SET_INDEX,
        GET_INDEX,
        LENGTH,
        PUSH,
        COLLECT(1),
        // ---------- kraj stavki za nizove
        // posto u nasem jeziku imamo kontrolu toka programa, potrebno je da imamo ove jump-ove
        // na primer ukoliko je tacan uslov if-a mi zelimo da skocimo na odredjeni tok programa,
        // dok ukoliko uslov if-a nije tacan onda zelimo da skocimo do else
        JUMP(1),
        JUMP_TRUE(1),
        JUMP_FALSE(1),
        // unarni operatori
        NEGATE,
        // binarne operacije(operacije sa 2 operanda)
        BIT_AND,
        BIT_OR,
        BIT_GT, // >
        BIT_LT, // <
        BIT_GTE, // >=
        BIT_LTE,// <=
        BIT_ET, // ==
        BIT_PLUS,
        BIT_MINUS,
        BIT_MUL,
        BIT_DIV,
        BIT_CR,// ^
        SET_GLOBAL(1),
        GET_GLOBAL(1),
        // Upvalue handling.
        GET_UPVALUE(1),
        PRINT,
        EXIT,
        PUTC,
        ;
        public final int argCount;
        Code()
        {this(0);}
        Code(int argCount)
        {this.argCount = argCount;}
    }
    private Code opcode;
    public Code opcode(){return opcode;}

    public Instruction(Code opcode){
        assert opcode.argCount == 0;
        this.opcode = opcode;
    }

    public Instruction(Code opcode, long arg1){
        assert opcode.argCount == 1;
        this.opcode = opcode;
        this.arg1 = arg1;
    }
    private long arg1 = -1;
    public void setArg1(long arg1){
        assert opcode.argCount >= 1;
        this.arg1 = arg1;
    }
    public long getArg1(){return arg1;}
    @Override
    public String toString(){
        var s = new StringBuilder();
        s.append(opcode);
        if(opcode.argCount >= 1){s.append(" ").append(arg1);}
        return s.toString();
    }
    // dodati ovde printer(pretty print)

}
