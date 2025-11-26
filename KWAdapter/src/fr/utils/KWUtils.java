 package fr.utils;
 
 import de.kwsoft.mtext.util.misc.PasswordEncryptor;
 import java.net.URI;
 import java.net.URISyntaxException;
 import java.net.URL;
 
 
 
 
 
 
 public class KWUtils
 {
   public static String[] getUrlForJavaAPI(String wsURL) throws URISyntaxException {
     URI uri = new URI(wsURL);
     return getUrlForJavaAPI(uri);
   }
 
   
   public static String[] getUrlForJavaAPI(URL wsURL) {
     boolean ssl = (wsURL.getProtocol().compareToIgnoreCase("https") == 0);
     
     String url = ssl ? "https-remoting" : (("http-remoting://" + wsURL.getHost() + ":" + wsURL.getPort() + "/" + wsURL.getPath() != null) ? wsURL.getPath() : "");
     return new String[] { url };
   }
 
 
 
 
   
   public static String[] getUrlForJavaAPI(URI wsURL) {
     URI uri = wsURL;
     boolean ssl = (uri.getScheme().compareToIgnoreCase("https") == 0);
     
     String url = String.valueOf(ssl ? "https-remoting" : "http-remoting") + "://" + uri.getHost() + ":" + uri.getPort() + "/" + ((uri.getPath() != null) ? uri.getPath() : "");
     return new String[] { url };
   }
 
   
   public static String crypt(String pwd) {
     return PasswordEncryptor.encode(pwd);
   }
 
   
   public static String uncrypt(String pwd) {
     return PasswordEncryptor.decode(pwd);
   }
 }


