package tagtime.ping;

import java.awt.Insets;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import javax.swing.BorderFactory;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class FakePing extends JFrame implements PropertyChangeListener
{
	private static final long serialVersionUID = 1489389636896999991L;
	final JTextArea inputText;
	JProgressBar ticker;
	private final int MAX_SEC = 59;

	public FakePing()
	{
		super("Pinging you");
		final int GRID_WIDTH = 3;
		
		setLocation( 100, 200 );
		
		//set up the root pane
		JRootPane root = getRootPane();
		//root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
		root.setLayout(new GridBagLayout());
		root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		GridBagConstraints constraints = new GridBagConstraints();
		
		//define the cancel and submit buttons
		final JButton cancelButton = new JButton("CANCEL");
		cancelButton.addActionListener(null);
		
		final JButton submitButton = new JButton("SUBMIT");
		submitButton.addActionListener(null);
		
		//get the last tags submitted
		String ditto = "";
		if(ditto != null && (ditto.length() == 0 || ditto.indexOf(' ') == -1)) {
			ditto = null;
		}
		
		//convert the given list of TagCount objects to a list of strings
		String[] cachedTags = {"will be born", "am living", "died"};
		
		Dimension windowSize = new Dimension( 350, 400 );
		
		//prepare the list to be displayed
		JList<String> quickTags = new JList<String>(cachedTags);
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
		ticker = new JProgressBar( 0, MAX_SEC ); // min max seconds
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
		constraints.insets = new Insets(0, 0, 0, 0);
	}

	public void propertyChange(PropertyChangeEvent evt) {
	   if ("progress" == evt.getPropertyName()) {
		   int progress = (Integer) evt.getNewValue();
		   ticker.setValue(progress);
	   }
	}

	private class Task extends SwingWorker<Void, Void> {

		@Override
        public Void doInBackground() {
        	int progress = 0;
            //Initialize progress property.
            setProgress(0);
            while (progress < MAX_SEC) {
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
			ticker.setValue( MAX_SEC );
			// Just a warning, not a nanny. Others will close the frame.
		}
	}
}
