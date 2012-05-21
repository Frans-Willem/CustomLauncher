import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;


public class CustomClassLoader extends URLClassLoader {
	Set<URL> setUrls;
	Set<String> setRewriteSources;
	Set<String> setNoRewriteSources;
	List<CustomPatch> listPatches;
	
	CustomClassLoader(URL[] a, ClassLoader p) {
		super(a,p);
		setUrls=new HashSet<URL>();
		for (URL u : a)
			setUrls.add(u);
		setRewriteSources=new HashSet<String>();
		setNoRewriteSources=new HashSet<String>();
		listPatches=new LinkedList<CustomPatch>();
	}
	@Override
	protected void addURL(URL url) {
		//Only actually add URLs not already in the classpath. Don't want to clog up the classpath with double [builtin] mods.
		if (setUrls.add(url)) {
			super.addURL(url);
		}
	}
	//To allow addURL to be called from outside this package
	//e.g. classes loaded through this classloader have this as classloader, while this classloader has another.
	public void publicAddURL(URL url) {
		addURL(url);
	}
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		//Self reference, so classes loaded by this classloader can actually access this classloader
		if (name.equals("CustomClassLoader"))
			return CustomClassLoader.class;
		//Is the class already loaded?
		Class<?> ret = findLoadedClass(name);
		if (ret!=null)
			return ret;
		//Locate the class
		String strClassfile=name.replace('.','/')+".class";
		URL urlResource=getResource(strClassfile);
		//Is it in the list of sources that are forced to be rewritten?
		boolean bForceRewrite=false;
		boolean bPreventRewrite=false;
		if (urlResource!=null) {
			for (String s : setRewriteSources)
				if (urlResource.toString().startsWith(s))
					bForceRewrite=true;
			for (String s : setNoRewriteSources)
				if (urlResource.toString().startsWith(s))
					bPreventRewrite=true;
		}
		if (!bForceRewrite) {
			//Maybe it's a system class?
			try {
				ret=findSystemClass(name);
				if (ret!=null)
					return ret;
			}
			catch (ClassNotFoundException e) {
				ret=null;
			}
		}
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
		if (!bPreventRewrite) {
			ClassReader cr=new ClassReader(bBuffer);
			ClassWriter cw=new ClassWriter(cr,0);
			ClassVisitor cv=cw;
			for (CustomPatch p : listPatches) {
				cv=p.create(cv);
			}
			cr.accept(cv, 0);
			bBuffer=cw.toByteArray();
		}
		
		return this.defineClass(null,bBuffer, 0, bBuffer.length,protectionDomain);
	}
	
	public void addRewriteClass(String name) {
		String strClassfile=name.replace('.','/')+".class";
		URL urlResource=getResource(strClassfile);
		if (urlResource==null)
			return;
		String strResource=urlResource.toString();
		strResource=strResource.substring(0,strResource.length()-strClassfile.length());
		setRewriteSources.add(strResource);
	}
	
	public void addNoRewriteClass(String name) {
		String strClassfile=name.replace('.','/')+".class";
		URL urlResource=getResource(strClassfile);
		if (urlResource==null)
			return;
		String strResource=urlResource.toString();
		strResource=strResource.substring(0,strResource.length()-strClassfile.length());
		setNoRewriteSources.add(strResource);
	}
	
	public void addPatch(CustomPatch p) {
		if (p!=null)
			listPatches.add(p);
	}
}