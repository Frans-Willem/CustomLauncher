import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedList;
import java.util.List;

public class LoginPanel extends Panel implements ActionListener, ItemListener {
	private TextField txtUsername;
	private TextField txtPassword;
	private Button btnLogin;
	private Checkbox chkRemember;
	private List<LoginPanelListener> listListeners;
	private Label lblError;
	
	public LoginPanel() {
		listListeners=new LinkedList<LoginPanelListener>();
		
		GridBagLayout gbl=new GridBagLayout();
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.insets=new Insets(2,2,2,2);
		setLayout(gbl);
		Component c;
		
		lblError=new Label("Error!",Label.CENTER);
		lblError.setVisible(false);
		lblError.setForeground(Color.RED);
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		gbl.setConstraints(lblError,gbc);
		gbc.fill=GridBagConstraints.NONE;
		gbc.gridwidth=1;
		add(lblError);
		
		c=new Label("Username:",Label.RIGHT);
		gbc.anchor=GridBagConstraints.EAST;
		gbl.setConstraints(c,gbc);
		gbc.anchor=GridBagConstraints.CENTER;
		add(c);
		
		txtUsername=new TextField();
		txtUsername.addActionListener(this);
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbl.setConstraints(txtUsername,gbc);
		gbc.fill=GridBagConstraints.NONE;
		gbc.weightx=0.0;
		add(txtUsername);
		
		c=new Panel();
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		gbl.setConstraints(c, gbc);
		gbc.gridwidth=1;
		add(c);
		
		c=new Label("Password:",Label.RIGHT);
		gbc.anchor=GridBagConstraints.EAST;
		gbl.setConstraints(c,gbc);
		gbc.anchor=GridBagConstraints.CENTER;
		add(c);
		
		txtPassword=new TextField();
		txtPassword.addActionListener(this);
		txtPassword.setEchoChar('*');
		gbc.fill=GridBagConstraints.HORIZONTAL;
		gbc.weightx=1.0;
		gbl.setConstraints(txtPassword,gbc);
		gbc.fill=GridBagConstraints.NONE;
		gbc.weightx=0.0;
		add(txtPassword);
		
		btnLogin=new Button("Login");
		btnLogin.addActionListener(this);
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		gbl.setConstraints(btnLogin, gbc);
		gbc.gridwidth=1;
		add(btnLogin);
		
		c=new Panel();
		gbl.setConstraints(c, gbc);
		add(c);
		
		chkRemember=new Checkbox("Remember password");
		chkRemember.addItemListener(this);
		gbl.setConstraints(chkRemember,gbc);
		add(chkRemember);
		
		c=new Panel();
		gbc.gridwidth=GridBagConstraints.REMAINDER;
		gbl.setConstraints(c, gbc);
		gbc.gridwidth=GridBagConstraints.RELATIVE;
		add(c);
	}
	
	@Override
	public Insets getInsets() {
		return new Insets(2,2,2,2);
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource()==txtUsername || event.getSource()==txtPassword || event.getSource()==btnLogin) {
			String strUsername=txtUsername.getText();
			String strPassword=txtPassword.getText();
			boolean bRemember=chkRemember.getState();
			for (LoginPanelListener l : listListeners)
				l.loginRequested(strUsername, strPassword, bRemember);
		}
	}
	
	public void addLoginListener(LoginPanelListener l) {
		listListeners.add(l);
	}
	
	public void removeLoginListener(LoginPanelListener l) {
		listListeners.remove(l);
	}
	
	public void setUsername(String strUsername) {
		txtUsername.setText(strUsername);
	}
	
	public void setPassword(String strPassword) {
		txtPassword.setText(strPassword);
	}
	
	public void setRemember(boolean bRemember) {
		chkRemember.setState(bRemember);
	}
	
	public void setError(String strError) {
		if (strError!=null)
			lblError.setText(strError);
		lblError.setVisible(strError!=null);
	}

	@Override
	public void itemStateChanged(ItemEvent event) {
		if (event.getSource()==chkRemember && event.getStateChange()==ItemEvent.DESELECTED) {
			for (LoginPanelListener l : listListeners)
				l.loginForgetRequested();
		}
	}
}
