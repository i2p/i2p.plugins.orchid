package net.i2p.orchid;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

import net.i2p.I2PAppContext;
import net.i2p.util.Log;
import net.i2p.util.LogManager;

/**
 *  Divert Java Logger messages to I2P router log
 */
public class OrchidLogHandler extends Handler {

    private final LogManager _mgr;

    public OrchidLogHandler(I2PAppContext ctx) {
        _mgr = ctx.logManager();
        Logger base = Logger.getLogger("com.subgraph.orchid");
        base.addHandler(this);
        base.setUseParentHandlers(false);
        base.setLevel(Level.FINE);
    }

    public void close() {
        Logger base = Logger.getLogger("com.subgraph.orchid");
        base.setUseParentHandlers(true);
        base.removeHandler(this);
    }

    public void flush() {}

    public void publish(LogRecord record) {
        Log log = _mgr.getLog(record.getLoggerName());
        int level = toI2PLevel(record.getLevel());
        // fix logs so they don't spew html tags; various cleanups & detritus removal
        log.log(level, "[Orchid] " + record.getMessage().replaceAll("<noscript>((?:.*?\r?\n?)*)</noscript>", "")
                                          .replaceAll("<.+?>", " ")
                                          .replaceAll("Building&hellip;", "")
                                          .replaceAll("&hellip;", "...")
                                          .replaceAll("Target=", "\\\n* Target: ")
                                          .replaceAll("Circuit=", "CircuitID=")
                                          .replaceAll("Exit ", "")
                                          .replaceAll("Open ", "")
                                          .replaceAll("( )+", " ")
                                          .replaceAll("\\(.+?\\)", "")
                                          .replaceAll("\\[ ", "\\[")
                                          .replaceAll(" \\]", "\\]")
                                          .replaceAll(" +\\]", "\\]")
                                          .replaceAll(" =", "=")
                                          .replaceAll("= ", "=")
                                          .replaceAll("\\] -> \\[", " -> ")
                                          .replaceAll("\\( ", "\\(")
                                          .replaceAll(" \\)", "\\)"), record.getThrown());
    }

    private static int toI2PLevel(Level level) {
        int val = level.intValue();
        if (val >= Level.SEVERE.intValue())
            return Log.ERROR;
        if (val >= Level.WARNING.intValue())
            return Log.WARN;
        if (val >= Level.INFO.intValue())
            return Log.INFO;
        return Log.DEBUG;
    }
}
