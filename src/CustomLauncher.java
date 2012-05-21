import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.*;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
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
	private String strSavedUsername;
	private String strSavedPassword;
	private boolean bRemember;
	
	private static File getClassDirectory() {
		File fileMe=new File(CustomLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		File fileDir=fileMe.getParentFile();
		return fileDir;
	}
	
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
		
		strSavedUsername=strSavedPassword="";
		bRemember=false;
		try {
			readLoginInfo();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		panelLoginPanel.setUsername(strSavedUsername);
		panelLoginPanel.setPassword(strSavedPassword);
		panelLoginPanel.setRemember(!strSavedPassword.equals(""));
		
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
	
	private Component getShownComponent() {
		return panelMain.getComponent(0);
	}
	
	public void startApplet() throws MalformedURLException, ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, SecurityException, NoSuchMethodException, InvocationTargetException {
		ClassLoader loader=CustomLauncher.class.getClassLoader();
		CustomClassLoader cl=null;
		if (loader instanceof CustomClassLoader) {
			System.out.println("Re-using CustomClassLoader");
			cl=(CustomClassLoader)loader;
		} else {
			System.out.println("Creating new CustomClassLoader");
			cl=new CustomClassLoader(new URL[0],loader);
		}
		//File fileMe=new File(CustomLauncher.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		File fileDir=getClassDirectory();
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
		
		for (URL u : arrUrls)
			cl.publicAddURL(u);

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
	
	public static void post_patch_main(String[] args) {
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
		Thread timeout=new Thread() {
			public void run() {
				try {
					Thread.sleep(30000);
				}
				catch (InterruptedException e) {
					//e.printStackTrace();
				}
				System.out.println("Exiting");
				System.exit(0);
			}
		};
		timeout.start();
		Component cmp=getShownComponent();
		if (cmp instanceof Applet) {
			System.out.println("Closing Applet");
			Applet a=(Applet)cmp;
			a.stop();
			a.destroy();
		}
		System.out.println("Clean exit");
		timeout.interrupt();
		try {
			timeout.join();
		}
		catch (InterruptedException ie) {
			
		}
		System.out.println("Exiting from main thread");
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
			strSavedUsername=strUsername;
			strSavedPassword=(bRemember?strPassword:"");
			this.bRemember=bRemember;
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
			String strUsername=handlerLogin.getUsername();
			//If it's only a recapitalization, save it.
			if (strSavedUsername.length()==strUsername.length())
				strSavedUsername=strUsername;
			try {
				writeLoginInfo();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
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

	@Override
	public void loginForgetRequested() {
		strSavedPassword="";
		bRemember=false;
		try {
			writeLoginInfo();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Cipher getCipher(int mode, String password) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException  {
		Random r=new Random(43287234L);
		byte[] salt=new byte[8];
		r.nextBytes(salt);
		PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 5);

	    SecretKey pbeKey = SecretKeyFactory.getInstance("PBEWithMD5AndDES").generateSecret(new PBEKeySpec(password.toCharArray()));
	    Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");
	    cipher.init(mode, pbeKey, pbeParamSpec);
	    return cipher;
	}
	
	private void writeLoginInfo() throws IOException {
		File lastLogin=new File(getClassDirectory(),"lastlogin");
		Cipher cipher=null;
		try {
			cipher=getCipher(Cipher.ENCRYPT_MODE,"passwordfile");
		}
		catch (Exception e) {
			cipher=null;
		}
		DataOutputStream dos;
		if (cipher!=null)
			dos=new DataOutputStream(new CipherOutputStream(new FileOutputStream(lastLogin),cipher));
		else
			dos=new DataOutputStream(new FileOutputStream(lastLogin));
		dos.writeUTF(strSavedUsername);
		dos.writeUTF(bRemember?strSavedPassword:"");
		dos.flush();
		dos.close();
	}
	
	private void readLoginInfo() throws IOException {
		File lastLogin=new File(getClassDirectory(),"lastlogin");
		Cipher cipher=null;
		try {
			cipher=getCipher(Cipher.DECRYPT_MODE,"passwordfile");
		}
		catch (Exception e) {
			cipher=null;
		}
		DataInputStream dis;
		if (cipher!=null)
			dis=new DataInputStream(new CipherInputStream(new FileInputStream(lastLogin),cipher));
		else
			dis=new DataInputStream(new FileInputStream(lastLogin));
		strSavedUsername=dis.readUTF();
		strSavedPassword=dis.readUTF();
		bRemember=(strSavedPassword.equals(""));
		dis.close();
	}
}
