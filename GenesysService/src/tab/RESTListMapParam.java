package tab;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RESTListMapParam {
	private static Log logger = LogFactory.getLog(RESTListMapParam.class);
	private static ObjectMapper singleQuotesMapper = new ObjectMapper().configure(Feature.ALLOW_SINGLE_QUOTES, true);
	private java.util.List<java.util.Map<String, Object>> data = null;
	private String dataStr;
	public RESTListMapParam( String tagStr ) throws WebApplicationException {
		dataStr = tagStr;
		try {
			if(tagStr!=null && tagStr.length()>0) {
				data = singleQuotesMapper.readValue(tagStr, new TypeReference<java.util.List<java.util.Map<String, Object>>>(){});
			}
		} catch (IOException e) {
			logger.error(tagStr,e);
		}
    }
	public Object getData() {
		return data;
	}
	public String toString() {
		return dataStr;
	}
}
