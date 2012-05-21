import java.awt.*;
import java.awt.event.*;


public class WaitComponent extends Label implements Runnable {
	private Thread threadUpdate;
	private boolean bRunning;
	private String strText;
	
	
	public WaitComponent() {
		threadUpdate=null;
		strText="";
		bRunning=false;
	}
	
	@Override
	public void setText(String strText) {
		super.setText(strText);
		this.strText=strText;
	}
	
	@Override
	public String getText() {
		return this.strText;
	}

	public void stop() {
		if (!bRunning)
			return;
		bRunning=false;
		if (threadUpdate!=null) {
			threadUpdate.interrupt();
			while (threadUpdate.isAlive()) {
				try {
					threadUpdate.join();
				} catch (InterruptedException e) {
				}
			}
			threadUpdate=null;
		}
	}

	public void start() {
		if (bRunning)
			return;
		bRunning=true;
		if (threadUpdate==null) {
			threadUpdate=new Thread(this);
			threadUpdate.start();
		}
	}

	@Override
	public void run() {
		int nDots=0;
		String strDots="..........";
		while (bRunning) {
			super.setText(strText+strDots.substring(0,nDots));
			nDots=(nDots+1)%strDots.length();
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
		}
	}
}
