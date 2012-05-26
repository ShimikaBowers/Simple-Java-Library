/**
 * 
 */
package simple.net.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.SSLHandshakeException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import simple.net.Uri;
import simple.util.logging.Log;
import simple.util.logging.LogFactory;
/**
 * <hr>
 * <br>
 * Created: Oct 16, 2011
 * 
 * @author Kenneth Pierce
 */
public final class Client{
	private static final Log log=LogFactory.getLogFor(Client.class);
	private final Hashtable<String,DefaultHttpClient> cache=new Hashtable<String,DefaultHttpClient>();
	private final BasicCookieStore cookies=new BasicCookieStore();
	public static final Header[] defaults=new Header[]{
		new BasicHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
		,new BasicHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.3")
		,new BasicHeader("Accept-Encoding","gzip,deflate")
		,new BasicHeader("Accept-Language","en-US,en;q=0.8")
	};
	/**
	 * The last response
	 */
	private HttpResponse response=null;
	public Header[] getHeader(String header){
		return response.getHeaders(header);
	}
	public HttpEntity getResponseEntity(){
		return response.getEntity();
	}
	private DefaultHttpClient getClient(Uri uri){
		DefaultHttpClient client=cache.get(uri.getHost());
		if(client==null){
			client=new DefaultHttpClient();
			client.setCookieStore(cookies);
			client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
			client.getParams().setIntParameter("http.socket.timeout",15000);
			client.setHttpRequestRetryHandler(RetryHandler);
			client.addRequestInterceptor(new HttpRequestInterceptor(){
				public void process(final HttpRequest request,final HttpContext context) throws HttpException,IOException{
					if(!request.containsHeader("Accept-Encoding")){
						request.addHeader("Accept-Encoding","gzip,deflate");
					}
				}
			});
			client.addResponseInterceptor(new HttpResponseInterceptor(){
				public void process(final HttpResponse response,final HttpContext context) throws HttpException,IOException{
					HttpEntity entity=response.getEntity();
					Header ceheader=entity.getContentEncoding();
					log.debug("CE",ceheader);
					if(ceheader!=null){
						HeaderElement[] codecs=ceheader.getElements();
						log.debug("Codecs",codecs);
						for(int i=0;i<codecs.length;i++){
							if(codecs[i].getName().equalsIgnoreCase("gzip")){
								response.setEntity(new GzipDecompressingEntity(response.getEntity()));
								log.debug("Content-Encoding","GZIP");
								return;
							}else if(codecs[i].getName().equalsIgnoreCase("deflate")){
								response.setEntity(new DeflaterDecompressingEntity(response.getEntity()));
								log.debug("Content-Encoding","DEFLATE");
								return;
							}
						}
					}
				}
			});
			cache.put(uri.getHost(),client);
		}
		return client;
	}
	public void addCookie(org.apache.http.cookie.Cookie cookie){
		cookies.addCookie(cookie);
	}
	public void addCookies(org.apache.http.cookie.Cookie[] cookie){
		cookies.addCookies(cookie);
	}
	public BasicCookieStore getCookieStore(){
		return cookies;
	}
	public HttpResponse get(Uri uri,Header[] headers) throws ClientProtocolException, IOException{
		return get(uri,headers,null);
	}
	public HttpResponse get(Uri uri,Header[] headers,HttpContext context) throws ClientProtocolException, IOException{
		DefaultHttpClient client=getClient(uri);
		HttpGet req=new HttpGet(uri.toString());
		log.debug(client.getCookieStore().getCookies().toArray());
		req.setHeaders(defaults);
		if(headers!=null){
			for(Header header:headers)
				req.setHeader(header);
		}
		if(context==null)
			response=client.execute(req);
		else
			response=client.execute(req,context);
		log.debug("Request",req.getRequestLine().toString());
		log.debug("Request",req.getAllHeaders());
		if(response.getStatusLine().getStatusCode()==301 || response.getStatusLine().getStatusCode()==302){// redirection
			Header location=response.getFirstHeader("Location");
			log.debug("Redirect",uri+" --TO-- "+location);
			if(location!=null) return get(new Uri(response.getFirstHeader("Location").getValue()),headers,context);
		}
		log.debug("Response",response);
		return response;
	}
	public HttpResponse get(Uri uri) throws ClientProtocolException, IOException{
		return get(uri,null,null);
	}
	public HttpResponse post(Uri uri,Header[] headers,String[][] data){
		return null;
	}
	public BufferedInputStream getInputStream() throws IllegalStateException,IOException{
		return new BufferedInputStream(response.getEntity().getContent());
	}
	private static final HttpRequestRetryHandler RetryHandler=new HttpRequestRetryHandler(){
		public boolean retryRequest(IOException exception,int executionCount,HttpContext context){
			if(executionCount>=5){
				// Do not retry if over max retry count
				return false;
			}
			if(exception instanceof NoHttpResponseException){
				// Retry if the server dropped connection on us
				return true;
			}
			if(exception instanceof SSLHandshakeException){
				// Do not retry on SSL handshake exception
				return false;
			}
			HttpRequest request=(HttpRequest)context.getAttribute(ExecutionContext.HTTP_REQUEST);
			boolean idempotent=!(request instanceof HttpEntityEnclosingRequest);
			if(idempotent){
				// Retry if the request is considered idempotent
				return true;
			}
			return false;
		}
	};
	public Client(){
		/*
		try{
			//TODO: add persistent cookie cache
			File cookies = App.getResourceAsFile("cookie.cache",getClass());
		}catch(FileNotFoundException e){
			//log.error(e);
			//e.printStackTrace();
		}//*/
		
	}
	public void addCookies(List<Cookie> cookies2) {
		for(Cookie cookie:cookies2){
			addCookie(cookie);
		}
	}
}
class GzipDecompressingEntity extends HttpEntityWrapper{
	public GzipDecompressingEntity(final HttpEntity entity){
		super(entity);
	}
	@Override
	public InputStream getContent() throws IOException,IllegalStateException{
		// the wrapped entity's getContent() decides about repeatability
		InputStream wrappedin=wrappedEntity.getContent();
		return new GZIPInputStream(wrappedin);
	}
	@Override
	public long getContentLength(){
		// length of ungzipped content is not known
		return -1;
	}
}
class DeflaterDecompressingEntity extends HttpEntityWrapper{
	public DeflaterDecompressingEntity(final HttpEntity entity){
		super(entity);
	}
	@Override
	public InputStream getContent() throws IOException,IllegalStateException{
		// the wrapped entity's getContent() decides about repeatability
		InputStream wrappedin=wrappedEntity.getContent();
		return new InflaterInputStream(wrappedin);
	}
	@Override
	public long getContentLength(){
		// length of unzipped content is not known
		return -1;
	}
}