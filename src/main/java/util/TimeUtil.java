package main.java.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
    private static final Logger LOG = LoggerFactory.getLogger(TimeUtil.class);

    private static SimpleDateFormat projExpDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static int[] getCurrentAndNextYearMonth(){
        Calendar calendar = Calendar.getInstance();
        int month = calendar.get(Calendar.MONTH)+1;
        int year = calendar.get(Calendar.YEAR);

        int nextMonth = month<12 ? month+1 : 1;
        int nextYear = month<12 ? year : year+1;

        return new int[]{year, month, nextYear, nextMonth};
    }

    public static int calcTotalHours(int hours, int minutes, int seconds){
        if(seconds>59){
            minutes += seconds/60;
        }
        if(minutes>59){
            hours += minutes/60;
        }
        return hours;
    }

    public static String convertSeconds(long seconds){
        long hour = seconds/3600;
        seconds %= 3600;

        long minute = seconds/60;
        seconds %= 60;

        String hours = hour+"hour ";
        if(hour<10){
            hours = "0"+hours;
        }

        String minutes = minute+"minute ";
        if(minute<10){
            minutes = "0"+minutes;
        }

        return hours+minutes+seconds+"seconds";
    }

    /**
     * In the format of yyyy-MM-dd
     * @return
     */
    public static boolean isPassed(String date){
        try {
            Date dateD = projExpDateFormat.parse(date+" 23:59:59");
            Date now = Calendar.getInstance().getTime();
            LOG.info("Test is passed: date: " + dateD + ", now: " + now);
            if(dateD.after(now)){
                return false;
            }
        } catch (ParseException e) {
            LOG.error(e.getMessage(), e);
        }

        return true;
    }

    /**
     * date: mm/dd
     */
    public static int[] parseMonthDay(String date){
        String[] split = date.split("/");
        int month = NumUtil.readInt(split[0], 1);
        int day = NumUtil.readInt(split[1], 1);
        return new int[]{month, day};
    }

    /**
     * ts: m/d/y HH:mm:ss
     */
    public static boolean isWithin(String ts, int startMonth, int startDay, int endMonth, int endDay){
        String[] split = ts.split("/");
        String tsMonth = split[0];
        String tsDay = split[1];

        int tsMonthInt = NumUtil.readInt(tsMonth, 1);
        int tsDayInt = NumUtil.readInt(tsDay, 1);

        if(tsMonthInt>startMonth && tsMonthInt<endMonth){
            return true;
        }

        if(tsMonthInt==startMonth && tsDayInt>=startDay){
            return true;
        }

        if(tsMonthInt==endMonth && tsDayInt<=endDay){
            return true;
        }

        return false;
    }

    /**
     * dateTime: m/d/y HH:mm:ss
     */
    public static String getMonth(String dateTime){
        int slashIdx = dateTime.indexOf("/");
        if(slashIdx>0){
            return dateTime.substring(0, slashIdx);
        }
        return "0";
    }

    public static String getCurrentReadableTimestamp(){
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date dt = new Date();
        return sdf.format(dt);
    }

    public static String getReadableTimestamp(java.sql.Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        return sdf.format(date);
    }

    public static String getDate(java.sql.Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}
