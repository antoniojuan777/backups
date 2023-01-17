package ClienteRest; 

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import weblogic.net.http.SOAPHttpsURLConnection;

public class ClienteRest
{
    private final static String METODO_GET="GET";
    private final static String METODO_POST="POST";
    private Map queryParams;
    private JSONObject bodyObject;
    private JSONArray bodyArray;
    private String url;
    
    public ClienteRest(ClienteRestBuilder pClienteRestBuilder) {
        this.queryParams = pClienteRestBuilder.getQueryParams();
        this.bodyObject = pClienteRestBuilder.getBodyObject();
        this.bodyArray = pClienteRestBuilder.getBodyArray();
        this.url = pClienteRestBuilder.getUrl();
    }
    
    public static ClienteRestBuilder newBuilder() {
        return new ClienteRestBuilder();
    }
    
    private Object peticion(String urlWebService, String pMetodo, String pBody) throws Exception {
		String resultado = new String();
        URL url = new URL(urlWebService);
        HttpURLConnection conexion;
		conexion = (HttpURLConnection) url.openConnection();
        // InputStream aux = url.openStream();
        SOAPHttpsURLConnection prueba=null;
            
        conexion.setRequestMethod(pMetodo);
        
        if(pMetodo.equals(METODO_POST) && pBody!=null){
            conexion.setRequestProperty("Content-Type", "application/json");
            OutputStream output = null;
            try{
                conexion.setDoOutput(true);
                output = conexion.getOutputStream();
                output.write(pBody.getBytes("UTF-8"));
                output.flush();
            } finally {
                if(output != null)
                    output.close();
            }
            
        }
        
        int statusCode = conexion.getResponseCode();
        
		BufferedReader rd = null;
        
        try{
            rd = new BufferedReader(new InputStreamReader(conexion.getInputStream(), "UTF-8"));
			String linea;
			// Mientras el BufferedReader se pueda leer, agregar contenido a resultado
			while ((linea = rd.readLine()) != null) {
				resultado+=linea;
			}
		} finally {
            if(rd != null)
                rd.close();
            
		}
        if(statusCode >= 400 && statusCode<500){
            throw new ClienteRestException("Error de cliente: "+statusCode+", "+resultado);
        } else if(statusCode >= 500 && statusCode<600){
            throw new ClienteRestException("Error de servicio: "+statusCode+", "+resultado);
        }
        JSONParser parser = new JSONParser();
		return parser.parse(resultado);
	}
    
    public Object get () throws Exception {
        String url = this.url;
        if(this.queryParams!=null){
            url += "?";
            Iterator it = this.queryParams.entrySet().iterator();
            while(it.hasNext()) {
                String key=(String)it.next();
                String value = (String)this.queryParams.get(key);
                url += key + "=" + value + "&";
            }
        }
        if(this.bodyArray!=null || this.bodyObject!=null){
            System.out.println("el body no es tomado en cuenta en solicitudes GET");
        }
        return peticion(url, METODO_GET, null);
    }
    
    public Object post () throws Exception {
        String body = null;
        if(!this.queryParams.isEmpty()){
            System.out.println("los queryParams no son tomados en cuenta en solicitudes POST");
        }
        if(this.bodyArray!=null){
            body = this.bodyArray.toJSONString();
        } else if(this.bodyObject!=null){
            body = this.bodyObject.toJSONString();
        }
        return peticion(this.url, METODO_POST, body);
    }
    
    public static final class ClienteRestBuilder {
        private Map queryParams = new HashMap();
        private JSONObject bodyObject;
        private JSONArray bodyArray;
        private String url;
        
        public Map getQueryParams(){
            return this.queryParams;
        }
        public JSONObject getBodyObject(){
            return this.bodyObject;
        }
        public JSONArray getBodyArray(){
            return this.bodyArray;
        }
        public String getUrl(){
            return this.url;
        }
        
        public ClienteRestBuilder putQueryParam(String param, String valor) {
            this.queryParams.put(param,valor);
            return this;
        }

        public ClienteRestBuilder setBody(Object body) throws Exception {
            if(body instanceof JSONObject){
                this.bodyObject = (JSONObject)body;
            } else if (body instanceof JSONArray){
                this.bodyArray = (JSONArray)body;
            } else if (body.getClass().isArray()) {
                this.bodyArray = this.getJSONArray(body);
            } else {
                this.bodyObject = this.getJSONObject(body);
            }
            
            return this;
        }
        
        private JSONObject getJSONObject(Object objeto) throws Exception {
            JSONObject jsonObject = new JSONObject();
            for(int i=0;i<objeto.getClass().getDeclaredFields().length;i++){
                jsonObject.put(this.getClass().getDeclaredFields()[i].getName(),this.getClass().getDeclaredFields()[i].get(objeto));
            }
            return jsonObject;
        }
        
        private JSONArray getJSONArray(Object objeto) throws Exception {
            Object[] arrayObject = (Object[])objeto;
            JSONArray jsonArray = new JSONArray();
            for(int i=0;i<arrayObject.length;i++){
                jsonArray.add(this.getJSONObject(arrayObject[i]));
            }
            return jsonArray;
        }

        public ClienteRestBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public ClienteRestBuilder() {
        }

        public ClienteRest build() {
            if (this.url == null) {
                throw new IllegalArgumentException("url no especificada");
            }
            return new ClienteRest(this);
        }

    }
} 
