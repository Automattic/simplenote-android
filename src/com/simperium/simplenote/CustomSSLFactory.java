package com.simperium.simplenote;


import java.io.IOException;
import java.net.*;
import java.security.*;
import java.security.cert.*;
import javax.net.ssl.X509TrustManager;
import org.apache.http.conn.ssl.SSLSocketFactory;

/* This class allows for a SSL connection to the simplenote server.
 * It is all code I found on the internet. It is supposed to verify certificates
 *  but all of the methods are blank so it doesn't do anything.
 */


public class CustomSSLFactory extends SSLSocketFactory
{
	public CustomSSLFactory(KeyStore truststore)
			throws NoSuchAlgorithmException, KeyManagementException,
			KeyStoreException, UnrecoverableKeyException {
		super(truststore);
		// TODO Auto-generated constructor stub
	}

	public class FullX509TrustManager implements X509TrustManager
	{

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() 
		{
			return new X509Certificate[] {};
		}	
	}

	@Override
	public Socket createSocket(Socket s, String host, int port,
			boolean autoClose) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
