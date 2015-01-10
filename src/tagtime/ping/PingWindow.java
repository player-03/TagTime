/*
 * Copyright 2011-2012 Joseph Cloutier
 * 
 * This file is part of TagTime.
 * 
 * TagTime is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * TagTime is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TagTime. If not, see <http://www.gnu.org/licenses/>.
 */

package tagtime.ping;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.JProgressBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import tagtime.Main;
import tagtime.TagTime;
import tagtime.settings.SettingType;
import tagtime.util.TagCount;

/**
 * The popup window displayed for each ping.
 */
public class PingWindow extends JFrame implements ActionListener, PropertyChangeListener {
	private static final long serialVersionUID = 1489384636886031541L;
	
	public final TagTime tagTimeInstance;
	
	private static final String SUBMIT = "Submit";
	private static final String CANCEL = "Cancel";
	
	private static final int GRID_WIDTH = 3;
	private static final Insets ZERO_INSETS = new Insets(0, 0, 0, 0);
	
	final JTextArea inputText;
	final JList<String> quickTags;
	JProgressBar ticker;
	
	private PingJob ownerJob;
	
	public PingWindow(TagTime tagTimeInstance, PingJob ownerJob, List<TagCount> tagCounts) {
		//create the window
		super("Pinging " + tagTimeInstance.username + " - TagTime");
		
		this.tagTimeInstance = tagTimeInstance;
		
		setIconImage(Main.getIconImage());
		setLocation(tagTimeInstance.settings.getIntValue(SettingType.WINDOW_X),
					tagTimeInstance.settings.getIntValue(SettingType.WINDOW_Y));
		
		//record the job that created this window
		this.ownerJob = ownerJob;
		
		//set up the root pane
		JRootPane root = getRootPane();
		//root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
		root.setLayout(new GridBagLayout());
		root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		GridBagConstraints constraints = new GridBagConstraints();
		
		//define the cancel and submit buttons
		final JButton cancelButton = new JButton(CANCEL);
		cancelButton.addActionListener(this);
		
		final JButton submitButton = new JButton(SUBMIT);
		submitButton.setActionCommand(SUBMIT);
		submitButton.addActionListener(this);
		
		//get the last tags submitted
		String ditto = tagTimeInstance.log.getLastTags();
		if(ditto != null && (ditto.length() == 0 || ditto.indexOf(' ') == -1)) {
			ditto = null;
		}
		
		//convert the given list of TagCount objects to a list of strings
		String[] cachedTags = new String[tagCounts.size() + (ditto != null ? 1 : 0)];
		for(int i = tagCounts.size() - 1; i >= 0; i--) {
			cachedTags[i + (ditto != null ? 1 : 0)] = tagCounts.get(i).getTag();
		}
		
		//add the "ditto" tags in front of the list if appropriate
		if(ditto != null) {
			cachedTags[0] = ditto;
		}
		
		Dimension windowSize = new Dimension(
					tagTimeInstance.settings.getIntValue(SettingType.WINDOW_WIDTH),
					tagTimeInstance.settings.getIntValue(SettingType.WINDOW_HEIGHT));
		
		//set up the list of previously-submitted tags
		quickTags = new JList<String>(cachedTags);
		quickTags.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		quickTags.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				addSelectedTag();
			}
		});
		
		//prepare the list to be displayed
		JScrollPane listDisplay = new JScrollPane(quickTags);
		
		//set up the input text field
		inputText = new JTextArea();
		inputText.setRows(2);
		inputText.setLineWrap(true);
		inputText.setWrapStyleWord(true);
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
		
		//put the input text in a scrolling area
		JScrollPane inputTextScrollPane = new JScrollPane(inputText);
		Dimension inputTextDimension = new Dimension(windowSize.width,
					2 * getFontMetrics(inputText.getFont()).getHeight());
		inputTextScrollPane.setMinimumSize(inputTextDimension);
		inputTextScrollPane.setMaximumSize(inputTextDimension);
		inputTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		// time left
		ticker = new JProgressBar( 0, tagTimeInstance.settings.getIntValue(SettingType.WINDOW_TIMEOUT) );
		ticker.setValue( 0 );
		Task timeLeft = new Task();
		timeLeft.addPropertyChangeListener(this);
		timeLeft.execute();
		
		//create the heading text
		JLabel label = new JLabel("<html>It's tag time! " +
							"What are you doing <i>right now</i>?</html>");
		
		/*** place the components ***/

		// progress bar to warn afk or slow user
		resetConstraints(constraints);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridwidth = GRID_WIDTH;
		constraints.insets = new Insets(2, 0, 0, 2);
		root.add(ticker, constraints);
		
		//the label goes across the top
		resetConstraints(constraints);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridy = 1;
		constraints.gridwidth = GRID_WIDTH;
		constraints.insets = new Insets(0, 0, 8, 0);
		root.add(label, constraints);
		
		//the list goes below the label, goes all the way across,
		//and is the only one with vertical weight
		resetConstraints(constraints);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridy = 2;
		constraints.gridwidth = GRID_WIDTH;
		constraints.weighty = 1;
		constraints.insets = new Insets(0, 0, 3, 0);
		root.add(listDisplay, constraints);
		
		//the input text goes below the list
		resetConstraints(constraints);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridy = 3;
		constraints.gridwidth = GRID_WIDTH;
		constraints.insets = new Insets(0, 0, 5, 0);
		root.add(inputTextScrollPane, constraints);
		
		//the cancel button goes in the bottom right
		resetConstraints(constraints);
		constraints.gridx = GRID_WIDTH - 1;
		constraints.gridy = 4;
		constraints.weightx = 0;
		root.add(cancelButton, constraints);
		
		//the submit button goes next to the cancel button
		resetConstraints(constraints);
		constraints.gridx = GRID_WIDTH - 2;
		constraints.gridy = 4;
		constraints.weightx = 0;
		constraints.insets = new Insets(0, 0, 0, 8);
		root.add(submitButton, constraints);
		
		//an invisible box goes next to the submit and cancel buttons,
		//to push them to the side
		resetConstraints(constraints);
		constraints.gridy = 4;
		root.add(Box.createRigidArea(new Dimension(3, 3)), constraints);
		
		setSize(windowSize);
		
		//the submit button is selected when the user presses enter, but
		//only if it's enabled, and it doesn't get enabled until the user
		//enters a tag
		submitButton.setEnabled(false);
		root.setDefaultButton(submitButton);
		
		//clean up when closed
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		//if the user moves/resizes this window, record the new location/size
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				PingWindow.this.tagTimeInstance.settings.setValue(SettingType.WINDOW_X, getX());
				PingWindow.this.tagTimeInstance.settings.setValue(SettingType.WINDOW_Y, getY());
			}
			
			@Override
			public void componentResized(ComponentEvent e) {
				PingWindow.this.tagTimeInstance.settings.setValue(SettingType.WINDOW_WIDTH,
							getWidth());
				PingWindow.this.tagTimeInstance.settings.setValue(SettingType.WINDOW_HEIGHT,
							getHeight());
			}
		});
	}
	
	private void resetConstraints(GridBagConstraints constraints) {
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.EAST;
		constraints.insets = ZERO_INSETS;
	}
	
	@Override
	public void setVisible(boolean b) {
		if(b == isVisible()) {
			return;
		}
		
		if(b) {
			//focus on this window and the input text only if the window
			//is allowed to steal focus
			if(tagTimeInstance.settings.getBooleanValue(SettingType.STEAL_FOCUS)) {
				super.setVisible(true);
				inputText.requestFocus();
			} else {
				//in some environments, setting visible to true causes the
				//window to steal focus
				setFocusableWindowState(false);
				super.setVisible(true);
				setFocusableWindowState(true);
			}
			
			playSound();
		} else {
			super.setVisible(false);
		}
	}
	
	/**
	 * Plays the sound associated with opening a window.
	 */
	private void playSound() {
		File soundFile = new File(Main.getSoundDirectory() + "/"
					+ tagTimeInstance.settings.getStringValue(SettingType.SOUND_TO_PLAY));
		
		if(!soundFile.exists()) {
			return;
		}
		
		Clip soundClip;
		try {
			soundClip = AudioSystem.getClip();
			AudioInputStream inputStream = AudioSystem.
						getAudioInputStream(soundFile);
			soundClip.open(inputStream);
		} catch(UnsupportedAudioFileException e) {
			e.printStackTrace();
			return;
		} catch(IOException e) {
			e.printStackTrace();
			return;
		} catch(LineUnavailableException e) {
			e.printStackTrace();
			return;
		}
		
		soundClip.loop(0);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		
		if(action.equals(SUBMIT)) {
			ownerJob.submit(inputText.getText());
			dispose();
			
			//flush all saved data upon submission (multiple windows
			//may be canceled at once, but there should be a minimum of
			//a few seconds between submissions)
			tagTimeInstance.settings.flush();
		} else if(action.equals(CANCEL)) {
			dispose();
		}
	}
	
	@Override
	public void dispose() {
		for(WindowListener listener : getWindowListeners()) {
			removeWindowListener(listener);
		}
		
		//this will do nothing if the data was submitted normally
		ownerJob.submitCanceled();
		
		super.dispose();
	}
	
	protected void addSelectedTag() {
		Object selectedValue = quickTags.getSelectedValue();
		String currentText = inputText.getText();
		
		//append the selected tag only if it isn't already there
		if(selectedValue != null && !currentText.contains(selectedValue.toString())) {
			//add a space if needed
			if(currentText.length() > 0
						&& currentText.charAt(currentText.length() - 1) != ' ') {
				inputText.append(" ");
			}
			
			inputText.append(selectedValue.toString());
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
	   if ("progress" == evt.getPropertyName()) {
		   int progress = (Integer) evt.getNewValue();
		   ticker.setValue(progress);
	   }
	}

	private class Task extends SwingWorker<Void, Void> {
    	int max = tagTimeInstance.settings.getIntValue(SettingType.WINDOW_TIMEOUT);

		@Override
        public Void doInBackground() {
        	int progress = 0;
            //Initialize progress property.
            setProgress(0);
            while (progress < max) {
            	//Sleep for up to one second.
            	try {
                	Thread.sleep(1000);
                } catch (InterruptedException anotherShowedUp) {
                	Thread.yield( );
                }
            	progress += 1;
            	setProgress(progress);
            }
            return null;
		}

		// Executed in event dispatching thread
		@Override
		public void done() {
			ticker.setValue( max );
			// Just a warning, not a nanny. Others will close the frame.
		}
	}
}
