import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Set;
import java.util.HashSet;


public class CustomClassLoader extends URLClassLoader {
	Set<URL> setUrls;
	CustomClassLoader(URL[] a, ClassLoader p) {
		super(a,p);
		setUrls=new HashSet<URL>();
		for (URL u : a)
			setUrls.add(u);
	}
	@Override
	protected void addURL(URL url) {
		//Only actually add URLs not already in the classpath. Don't want to clog up the classpath with double [builtin] mods.
		if (setUrls.add(url)) {
			super.addURL(url);
		}
	}
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		//Is the class already loaded?
		Class<?> ret = findLoadedClass(name);
		if (ret!=null)
			return ret;
		//Maybe it's a system class?
		try {
			ret=findSystemClass(name);
			if (ret!=null)
				return ret;
		}
		catch (ClassNotFoundException e) {
			ret=null;
		}
		String strClassfile=name.replace('.','/')+".class";
		URL urlResource=getResource(strClassfile);
		InputStream is=this.getResourceAsStream(strClassfile);
		if (is==null || urlResource==null) {
			//Not found in the classpath, pass it on to URLClassLoader or further down the chain, maybe that classloader knows what to do.
			return super.loadClass(name,resolve);
		}
		//Turn the whole thing into a byte-array. Maybe at some point introduce code-rewriting here ?
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		int nRead; byte[] bBuffer=new byte[4096];
		try {
			while ((nRead=is.read(bBuffer,0,bBuffer.length))!=-1)
				os.write(bBuffer,0,nRead);
			os.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bBuffer=os.toByteArray();
		if (bBuffer.length==0)
			return super.loadClass(name,resolve);

		String strCodeSource;
		if (urlResource.getProtocol().equals("jar")) {
			strCodeSource=urlResource.getPath();
			strCodeSource=strCodeSource.substring(0,strCodeSource.length()-strClassfile.length());
			if (strCodeSource.endsWith("!/") || strCodeSource.endsWith("!\\"))
				strCodeSource=strCodeSource.substring(0,strCodeSource.length()-2);
		} else {
			strCodeSource=urlResource.toString();
			strCodeSource=strCodeSource.substring(0,strCodeSource.length()-strClassfile.length());
		}
		ProtectionDomain currentProtectionDomain=this.getClass().getProtectionDomain();
		URL urlCodeSource=null;
		try {
			urlCodeSource=new URL(strCodeSource);
		}
		catch(MalformedURLException e) {
			
		}
		ProtectionDomain protectionDomain=new ProtectionDomain(
				new CodeSource(urlCodeSource,new CodeSigner[0]),
				currentProtectionDomain.getPermissions(),
				this,
				currentProtectionDomain.getPrincipals());
		int nSplit=name.lastIndexOf('.');
		if (nSplit!=-1) {
			String strPackage=name.substring(0,nSplit);
			if (getPackage(strPackage)==null)
				definePackage(strPackage,"","","","","","",null);
		}
		return this.defineClass(null,bBuffer, 0, bBuffer.length,protectionDomain);
	}
}