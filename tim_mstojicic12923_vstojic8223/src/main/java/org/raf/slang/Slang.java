package org.raf.slang;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.raf.slang.ast.Location;
import org.raf.slang.ast.Position;

@Getter
@Setter

public class Slang {

    @Getter(AccessLevel.NONE)
    private boolean hadError = false;
    @Getter(AccessLevel.NONE)
    private boolean hadRuntimeError = false;

    public boolean hadError() {
        return hadError;
    }

    public boolean hadRuntimeError() {
        return hadRuntimeError;
    }

    public ANTLRErrorListener errorListener() {
        /* ANTLR reports many kinds of errors, but we're only interested in
           syntax errors, so lets steal those from BaseErrorListener.  */
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol, int line,
                                    int charPositionInLine, String msg,
                                    RecognitionException e) {
                var pos = new Position(line, charPositionInLine);
                error(new Location(pos, pos), "%s", msg);
            }
        };
    }
    public void error(Location location, String message, Object... args) {
        /* Could be improved to handle end also, later.  */
        var p = location.start();
        System.err.printf ("error: %d:%d: %s\n", p.line(), p.column(),
                message.formatted(args));
        setHadError(true);
    }

}
/**  SCOPE CHECKING
 * TESTOVI ZA FUNKCIJE
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; numero i; replay(primer > broj2){i = broj1;} getback i;}  - ne treba da baci error
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; replay(primer > broj2){i = broj1;} getback i;} - baca error jer ne vidi i
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; replay(primer > broj2){} getback broj1;}  - ne treba da baci error
 *  action numero proba(numero broj1, numero broj2){ numero primer = broj1; replay(primer > broj2){} getback nePostojecaVarijabla;} - treba da baci error
 *
 * TESTOVI ZA SPIN I REPLAY(ZA FOR I WHILE)
 * numero broj = 5;check(broj > 2){broj = broj +1;}numero vidan = 1; numero marko = 0; check(vidan>marko){spin(numero i = 0;vidan > 0; i + 1){vidan = vidan - i;}}
 *
 * TESTOVI ZA CHECK I BACKUP
 *
 *
 * */



