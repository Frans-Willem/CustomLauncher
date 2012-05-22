import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class PatchClassLoader extends URLClassLoader {

	public PatchClassLoader(URL[] urls) {
		super(urls);
	}
	
	public void publicAddURL(URL url) {
		addURL(url);
	}
	
}


public class Main {
	
	private static File getClassDirectory() {
		File fileMe=new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		File fileDir=fileMe.getParentFile();
		return fileDir;
	}
	
	private static void loadPatchFromDir(CustomClassLoader cl, File patch) {
		URLClassLoader pcl=null;
		System.out.println("Loading patch: "+patch.getName());
		try {
			pcl=new URLClassLoader(new URL[] { patch.toURI().toURL() });
		}
		catch (MalformedURLException e) {
			return;
		}
		for (File fileClass : patch.listFiles()) {
			String name=fileClass.getName();
			if (fileClass.isFile() && name.startsWith("patch_") && name.endsWith(".class")) {
				name=name.substring(0,name.length()-6);
				try {
					Class clsPatch=pcl.loadClass(name);
					Object o=clsPatch.newInstance();
					if (o instanceof CustomPatch)
						cl.addPatch((CustomPatch)o);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private static void loadPatchFromJar(CustomClassLoader cl, File patch) {
		URLClassLoader pcl=null;
		System.out.println("Loading patch: "+patch.getName());
		try {
			pcl=new URLClassLoader(new URL[] { patch.toURI().toURL() });
		}
		catch (MalformedURLException e) {
			return;
		}
		try {
			ZipInputStream zip = new ZipInputStream(new FileInputStream(patch));
			ZipEntry entry;
			while ((entry=zip.getNextEntry())!=null) {
				String name=entry.getName();
				if (!entry.isDirectory() && name.startsWith("patch_") && name.endsWith(".class")) {
					name=name.substring(0,name.length()-6);
					try {
						Class clsPatch=pcl.loadClass(name);
						Object o=clsPatch.newInstance();
						if (o instanceof CustomPatch)
							cl.addPatch((CustomPatch)o);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					} catch (InstantiationException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static void main(String[] args) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		/*PatchClassLoader pcl=new PatchClassLoader(new URL[0]);*/
		CustomClassLoader cl=new CustomClassLoader(new URL[0],CustomClassLoader.class.getClassLoader());
		
		
		// Load patches first, in order ofcourse.
		File fileDir=getClassDirectory();
		File filePatches=new File(fileDir,"patches");
		if (filePatches.isDirectory()) {
			for (File patch : filePatches.listFiles()) {
				if (patch.isDirectory()) {
					loadPatchFromDir(cl,patch);
				} else {
					loadPatchFromJar(cl,patch);
				}
			}
		}
		
		cl.addRewriteClass("CustomLauncher");
		Class clsCustomLauncher=cl.loadClass("CustomLauncher");
		Method m=clsCustomLauncher.getMethod("post_patch_main", new Class[] { String[].class });
		m.invoke(0, new Object[] { args });
	}
}
