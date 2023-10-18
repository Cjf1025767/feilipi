package tab;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class RESTDateParam {
    private final SimpleDateFormat dt = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    private final SimpleDateFormat d = new SimpleDateFormat( "yyyy-MM-dd" );
    private final SimpleDateFormat t = new SimpleDateFormat( "HH:mm:ss" );
    private java.util.Date date;
 
    public RESTDateParam( String dateStr ) throws WebApplicationException {
        try {
        	int nDash = 0,nColon = 0;
        	for(int i=0;i<dateStr.length();++i){
        		if(dateStr.charAt(i)=='-')nDash++;
        		else if(dateStr.charAt(i)==':')nColon++;
        	}
        	if(nDash>1 && nColon>1){
        		if(dateStr.indexOf("T")>0) {
        			date = java.util.Date.from(LocalDateTime.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toInstant());
        		}else {
        			date = new java.util.Date( dt.parse( dateStr ).getTime() );
        		}
        	}else if(nDash>1){
        		date = new java.util.Date( d.parse( dateStr ).getTime() );
        	}else if(StringUtils.isNumeric(dateStr)){
        		date = new java.util.Date(NumberUtils.createLong(dateStr ));
        	}else {
        		date = new java.util.Date( t.parse( dateStr ).getTime() );
        	}
        } catch ( final ParseException ex ) {
            throw new WebApplicationException( ex );
        }
    }
 
    public java.util.Date getStartDate() {
    	java.util.Calendar cal = Calendar.getInstance();
    	cal.setTime(date);
    	cal.set(Calendar.HOUR_OF_DAY, 0);
    	cal.set(Calendar.MINUTE, 0);
    	cal.set(Calendar.SECOND, 0);
    	cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
    
    public java.util.Date getDate() {
        return date;
    }
    
    public java.util.Date getEndDate() {
    	java.util.Calendar cal = Calendar.getInstance();
    	cal.setTime(date);
    	cal.set(Calendar.HOUR_OF_DAY, 23);
    	cal.set(Calendar.MINUTE, 59);
    	cal.set(Calendar.SECOND, 59);
    	cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    @Override
    public String toString() {
        if ( date != null ) {
            return date.toString();
        } else {
            return "";
        }
    }
}
