import java.util.EventListener;


public interface LoginPanelListener extends EventListener {
	public void loginRequested(String strUsername, String strPassword, boolean bRemember);
}
