package tagtime.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import tagtime.Main;

public class UsernameInputWindow extends JFrame implements ActionListener {
	private static final long serialVersionUID = -7966256157012331613L;
	
	private static final String OK = "Ok";
	private static final String CANCEL = "Cancel";
	
	protected final JTextField inputText;
	
	public UsernameInputWindow() {
		//create the window
		super("Please enter your Beeminder username");
		
		setIconImage(Main.getIconImage());
		
		//set up the root pane
		JRootPane root = getRootPane();
		root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
		root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		
		//define the cancel and submit buttons
		final JButton cancelButton = new JButton(CANCEL);
		cancelButton.addActionListener(this);
		
		final JButton submitButton = new JButton(OK);
		submitButton.setActionCommand(OK);
		submitButton.addActionListener(this);
		
		//put the cancel and submit buttons in a JPanel
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(submitButton);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(cancelButton);
		
		//set up the input text field
		inputText = new JTextField();
		inputText.setBorder(new LineBorder(Color.BLACK));
		
		inputText.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				//the submit button should be enabled if and only if text
				//has been entered
				submitButton.setEnabled(inputText.getText().length() > 0);
			}
			
			@Override
			public void insertUpdate(DocumentEvent e) {
				submitButton.setEnabled(true);
			}
			
			@Override
			public void changedUpdate(DocumentEvent e) {
				//this doesn't seem to be called at any time
				System.out.println("changedUpdate()");
			}
		});
		
		//place the components
		root.add(inputText);
		root.add(buttonPane);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = new Dimension(400, 90);
		setSize(windowSize);
		setLocation(screenSize.width / 2 - windowSize.width / 2,
					screenSize.height / 2 - windowSize.height / 2);
		setResizable(false);
		
		//the submit button is selected when the user presses enter, but
		//only if it's enabled, and it doesn't get enabled until the user
		//enters a username
		submitButton.setEnabled(false);
		root.setDefaultButton(submitButton);
		
		//clean up when closed
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}
	
	@Override
	public void setVisible(boolean b) {
		if(b == isVisible()) {
			return;
		}
		
		if(b) {
			super.setVisible(true);
			inputText.requestFocus();
		} else {
			super.setVisible(false);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		
		if(action.equals(OK)) {
			if(!Main.BEEMINDER_USERNAME_MATCHER.matcher(inputText.getText()).matches()) {
				String text = inputText.getText();
				if(!Main.BEEMINDER_USERNAME_MATCHER.matcher(text).matches()) {
					final int carat = inputText.getCaretPosition();
					int newCarat = 0;
					
					final int textLen = text.length();
					StringBuilder newText = new StringBuilder(textLen);
					char c;
					for(int i = 0; i < textLen; i++) {
						c = text.charAt(i);
						if(c >= 65 && c < 91) {
							c += 32;
						}
						if(c >= 48 && c < 58 || c >= 97 && c < 123) {
							newText.append(c);
							if(i < carat) {
								newCarat++;
							}
						}
					}
					
					inputText.setText(newText.toString());
					inputText.setCaretPosition(newCarat);
				}
			} else {
				String username = inputText.getText();
				Main.registerUsername(username);
				Main.runTagTime(username);
				dispose();
			}
		} else if(action.equals(CANCEL)) {
			dispose();
		}
	}
	
	@Override
	public void dispose() {
		for(WindowListener listener : getWindowListeners()) {
			removeWindowListener(listener);
		}
		
		super.dispose();
	}
}
