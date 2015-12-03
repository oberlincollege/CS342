package mocknet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * Main frame
 * @author rms
 *
 */
public class MockNet extends JFrame {
	public final static int WID = 700, HT = 750; 
	
	public MockNet() {
		setTitle("Mock Net");
		getContentPane().setBackground(Color.white);
		Control control = new Control();
		Canvas canvas = new Canvas(control, 25, 0.50);
		control.setCanvas(canvas);
		getContentPane().add(canvas, BorderLayout.CENTER);
		getContentPane().add(control, BorderLayout.SOUTH);
	}
	
	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() != WindowEvent.WINDOW_CLOSING) {
			super.processWindowEvent(e);
			return;
		}
		System.exit(0);
	}

	public static void main(String[] args) {
		try  {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
		}
		MockNet frame = new MockNet();
		frame.setSize(new Dimension(WID, HT));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = frame.getSize();
		if (frameSize.height > screenSize.height)
			frameSize.height = screenSize.height;
		if (frameSize.width > screenSize.width)
			frameSize.width = screenSize.width;
		frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		frame.setVisible(true);
		frame.validate();  
	}


}
