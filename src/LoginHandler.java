import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;


public class LoginHandler extends Thread {
	private String strUsername;
	private String strPassword;
	private boolean bFinished;
	private boolean bError;
	private String strError;
	private String strLatestVersion;
	private String strDownloadTicket;
	private String strSessionId;
	private List<LoginHandlerListener> lListeners;
	
	public LoginHandler(String strUsername, String strPassword) {
		super();
		this.bFinished=this.bError=false;
		this.strUsername=strUsername;
		this.strPassword=strPassword;
		this.strError=this.strLatestVersion=this.strDownloadTicket=this.strSessionId="";
		lListeners=new LinkedList<LoginHandlerListener>();
	}
	
	public void addListener(LoginHandlerListener l) {
		lListeners.add(l);
	}
	
	public void removeListener(LoginHandlerListener l) {
		lListeners.remove(l);
	}
	
	private void onError(String strError) {
		this.strError=strError;
		this.bError=true;
		this.bFinished=true;
		for (LoginHandlerListener l : lListeners)
			l.loginHandlerFinished();
	}
	private void onFinished() {
		this.bFinished=true;
		for (LoginHandlerListener l : lListeners)
			l.loginHandlerFinished();
	}
	
	@Override
	public void run() {
		StringBuilder sbData=new StringBuilder();
		sbData.append("user=");
		try {
			sbData.append(URLEncoder.encode(strUsername,"UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			sbData.append(URLEncoder.encode(strUsername));
		}
		sbData.append("&password=");
		try {
			sbData.append(URLEncoder.encode(strPassword,"UTF-8"));
		}
		catch (UnsupportedEncodingException e) {
			sbData.append(URLEncoder.encode(strPassword));
		}
		sbData.append("&version=13");
		String result = Utils.HttpsPostData("https://login.minecraft.net/", sbData.toString());
		if (result==null) {
			onError("Can't connect to minecraft.net");
			return;
		}
		String[] data=result.split(":");
		if (data.length<4) {
			if (data.length==1 && data[0].trim().equals("Bad login")) {
				onError("Login failed");
				return;
			}
			if (data.length==1 && data[0].trim().equals("Old version")) {
				onError("Outdated launcher");
				return;
			}
			return;
		}
		strLatestVersion=data[0].trim();
		strDownloadTicket=data[1].trim();
		strUsername=data[2].trim();
		strSessionId=data[3].trim();
		onFinished();
	}
	
	public boolean hasFinished() {
		return bFinished;
	}
	
	public boolean hasError() {
		return bFinished && bError;
	}
	
	public String getError() {
		return hasError()?strError:null;
	}
	
	public String getUsername() {
		return (bFinished && !bError)?strUsername:null;
	}
	
	public String getSessionId() {
		return (bFinished && !bError)?strSessionId:null;
	}
}
