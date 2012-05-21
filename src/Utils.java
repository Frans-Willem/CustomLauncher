import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;


public class Utils {
	public static void reverse(String[] data) {
	    int left = 0;
	    int right = data.length - 1;

	    while( left < right ) {
	        // swap the values at the left and right indices
	        String temp = data[left];
	        data[left] = data[right];
	        data[right] = temp;

	        // move the left and right index pointers in toward the center
	        left++;
	        right--;
	    }
	}
	
	private static boolean CheckMCCertificates(Certificate[] certs) {
		byte[] bPkMinecraft=null;
		try {
			InputStream is=Utils.class.getClassLoader().getResourceAsStream("minecraft.key");
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			byte bBuffer[]=new byte[2048];
			int nRead;
			while ((nRead=is.read(bBuffer))!=-1)
				bos.write(bBuffer,0,nRead);
			bos.flush();
			bPkMinecraft=bos.toByteArray();
		}
		catch (IOException e) {
			return false;
		}
		//for (Certificate c : certs) {
			PublicKey pk=certs[0].getPublicKey();
			byte[] bpk=pk.getEncoded();
			if (Arrays.equals(bpk,bPkMinecraft))
				return true;
		//}
		return false;
	}
	
	public static String HttpsPostData(String strURL, String strParameters) {
		try {
			byte[] bParameters=strParameters.getBytes();
			URL url=new URL(strURL);
			HttpsURLConnection conn=(HttpsURLConnection)url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length",Integer.toString(bParameters.length));
			conn.setRequestProperty("Content-Language","en-US");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.connect();
			if (!CheckMCCertificates(conn.getServerCertificates())) {
				conn.disconnect();
				return null;
			}
			OutputStream os=conn.getOutputStream();
			os.write(bParameters);
			os.flush();
			os.close();
			InputStream is=conn.getInputStream();
			ByteArrayOutputStream bos=new ByteArrayOutputStream();
			byte bBuffer[]=new byte[1024];
			int nRead;
			while ((nRead=is.read(bBuffer))!=-1)
				bos.write(bBuffer,0,nRead);
			bos.flush();
			return new String(bos.toByteArray());	
		}
		catch (Exception e) {
			return null;
		}
	}
}
