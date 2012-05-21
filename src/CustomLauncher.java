import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.*;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.*;
import java.security.cert.Certificate;
import java.security.PublicKey;
import javax.net.ssl.HttpsURLConnection;
import javax.swing.JPanel;
import javax.swing.UIManager;

import java.io.OutputStream;

public class CustomLauncher extends Frame implements WindowListener, AppletStub, LoginPanelListener, LoginHandlerListener {
	private Panel panelMain;
	private LoginPanel panelLoginPanel;
	private WaitComponent cmpWait;
	private LoginHandler handlerLogin;
	private Map<String,String> mapAppletParameters;
	
	public CustomLauncher() {
		super("Custom Minecraft Launcher");
		addWindowListener(this);
		setLayout(new BorderLayout());
		panelMain=new Panel();
		panelMain.setLayout(new BorderLayout());
		panelMain.setPreferredSize(new Dimension(854,480));
		add(panelMain,BorderLayout.CENTER);
		
		panelLoginPanel=new LoginPanel();
		panelLoginPanel.addLoginListener(this);
		cmpWait=new WaitComponent();
		cmpWait.setText("Logging in");
		cmpWait.setAlignment(Label.CENTER);
		
		setShownComponent(panelLoginPanel);
		
		handlerLogin=null;
		mapAppletParameters=new TreeMap<String,String>();
		mapAppletParameters.put("stand-alone", "true");
		
		pack();
		setLocationRelativeTo(null);
		validate();
	}
	
	private void setShownComponent(Component c) {
		for (Component x : panelMain.getComponents())
			panelMain.remove(x);
		panelMain.add(c,BorderLayout.CENTER);
		validate();
	}
	
	public void startApplet() throws MalformedURLException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, NoSuchMethodException, InvocationTargetException {
		ClassLoader loader=CustomLauncher.class.getClassLoader();
		if (loader instanceof URLClassLoader) {
			URLClassLoader ul=(URLClassLoader)loader;
			System.out.println("URLs: ");
			for (URL u : ul.getURLs()) {
				System.out.println(u.getFile());
			}
		}
		File fileMe=new File(CustomLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		File fileDir=fileMe.getParentFile();
		File fileDirBin=new File(fileDir,"bin");
		File fileDirMods=new File(fileDir,"mods");
		File fileDirNatives=new File(fileDirBin,"natives");
		System.out.println(fileDirBin);
		ArrayList<URL> arrUrls=new ArrayList<URL>();
		
		String[] arrBuiltinMods=fileDirMods.list();
		if (arrBuiltinMods==null) arrBuiltinMods=new String[0];
		Arrays.sort(arrBuiltinMods);
		Utils.reverse(arrBuiltinMods);
		
		for (String s : arrBuiltinMods) {
			if (s.startsWith("[builtin]"))
			arrUrls.add(new File(fileDirMods,s).toURI().toURL());
		}
		
		arrUrls.add(new File(fileDirBin,"lwjgl.jar").toURI().toURL());
		arrUrls.add(new File(fileDirBin,"jinput.jar").toURI().toURL());
		arrUrls.add(new File(fileDirBin,"lwjgl_util.jar").toURI().toURL());
		arrUrls.add(new File(fileDirBin,"minecraft.jar").toURI().toURL());
		
		System.setProperty("org.lwjgl.librarypath",fileDirNatives.getAbsolutePath());
		System.setProperty("net.java.games.input.librarypath",fileDirNatives.getAbsolutePath());

		ClassLoader cl=new CustomClassLoader(arrUrls.toArray(new URL[arrUrls.size()]),this.getClass().getClassLoader());
		Class classMinecraft=cl.loadClass("net.minecraft.client.Minecraft");
		for (Field f : classMinecraft.getDeclaredFields()) {
			if (f.getType() == File.class && Modifier.isStatic(f.getModifiers())) {
				f.setAccessible(true);
				System.out.println("Field "+f.getName());
				f.set(null, fileDir);
			}
		}

		Class classApplet=cl.loadClass("net.minecraft.client.MinecraftApplet");
		final Applet applet=(Applet)classApplet.newInstance();
		applet.setStub(this);
		applet.setPreferredSize(new Dimension(854,480));
		setShownComponent(applet);
		//applet.setSize(getWidth(),getHeight());
		applet.init();
		applet.start();
		this.setTitle("Minecraft");
	}
	
	public static void main(String[] args) {
		try {
	      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    }
	    catch (Exception localException) {
	    }
		CustomLauncher f = new CustomLauncher();
		f.setVisible(true);
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		System.out.println("windowClosing");
		System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void appletResize(int arg0, int arg1) {
	}

	@Override
	public AppletContext getAppletContext() {
		return null;
	}

	@Override
	public URL getCodeBase() {
		return null;
	}

	@Override
	public URL getDocumentBase() {
		try {
			return new URL("http://www.minecraft.net/game/");
		} catch (MalformedURLException e) {
			return null;
		}
	}

	@Override
	public String getParameter(String name) {
		System.out.println("getParameter "+name);
		return mapAppletParameters.get(name);
	}
	
	@Override
	public boolean isActive() {
		return true;
	}

	@Override
	public void loginRequested(String strUsername, String strPassword,
			boolean bRemember) {
			setShownComponent(cmpWait);
			cmpWait.start();
			handlerLogin=new LoginHandler(strUsername,strPassword);
			handlerLogin.addListener(this);
			handlerLogin.start();
	}
	
	@Override
	public void loginHandlerFinished() {
		cmpWait.stop();
		if (handlerLogin.hasError()) {
			panelLoginPanel.setError(handlerLogin.getError());
			setShownComponent(panelLoginPanel);
		} else {
			//panelLoginPanel.setError("Session ID: "+handlerLogin.getSessionId());
			//setShownComponent(panelLoginPanel);
			mapAppletParameters.put("username", handlerLogin.getUsername());
			mapAppletParameters.put("sessionid", handlerLogin.getSessionId());
			try {
				startApplet();
			}
			catch (Exception e) {
				panelLoginPanel.setError("Exception: "+e.toString());
				e.printStackTrace();
				setShownComponent(panelLoginPanel);
			}
		}
	}
}
