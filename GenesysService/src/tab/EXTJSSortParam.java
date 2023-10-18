package tab;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EXTJSSortParam {
	private String sProperty;
	private String sDirection;
	//{"property":"trunk","direction":"ASC"}
	public EXTJSSortParam( String sortStr ) throws WebApplicationException {
		java.util.List<java.util.Map<String, String>> data;
        try {
        	data = (new ObjectMapper()).configure(Feature.ALLOW_SINGLE_QUOTES, true).readValue(sortStr, new TypeReference<java.util.List<java.util.Map<String, String>>>(){});
        } catch (IOException e) {
        	throw new WebApplicationException( e );
        }
        if(data.size()>0){
	        sProperty = String.valueOf(data.get(0).get("property"));
	        sDirection = String.valueOf(data.get(0).get("direction"));
        }
    }
	public String getsProperty() {
		return sProperty;
	}
	public void setsProperty(String sProperty) {
		this.sProperty = sProperty;
	}
	public String getsDirection() {
		return sDirection;
	}
	public void setsDirection(String sDirection) {
		this.sDirection = sDirection;
	}
}
