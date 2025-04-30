package Log;

import java.io.PrintStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class Logger {

    private PrintStream out;

    public Logger(PrintStream out) {
        this.out = out;
    }

    public void log(String message) {
        out.println(getCurrentTimeFormatted() + ":" + message);
    }

    private String getCurrentTimeFormatted() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        long currentTimeMillis = System.currentTimeMillis();
        Date resultDate = new Date(currentTimeMillis);
        return sdf.format(resultDate);
    }
}
