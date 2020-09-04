/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package plugins.MasoudR.multifreticy;

import java.awt.Desktop;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import icy.gui.frame.progress.ToolTipFrame;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import plugins.MasoudR.multifreticy.Main.Prestart;

/*
 * TODO
 *x SU1 exits before screencap
 * milestones take up the space of the splitpane, make it get a scrollpane itself
 * make right splitpane deactive when running
 */


public class MultiFretIcy extends PluginActionable {

	public static Prestart PS;
	
	@Override
	public void run() {
		FileInputStream in;
		Properties cp = new Properties();
		File f = new File(System.getProperty("user.home") + "\\MFIoptions.cfg");
		if(!f.exists()) { 	
			try {
				f.createNewFile();
			} catch (IOException e) {
				System.out.println("Could not write settings file!");
				e.printStackTrace();
				return;
			}
		}
		try {
			in = new FileInputStream(System.getProperty("user.home") + "\\MFIoptions.cfg");		
		cp.load(in);
		in.close();

		Boolean agree = Boolean.parseBoolean(cp.getProperty("agree", "false"));
		System.out.println("Agree is " + agree);
		if (!agree) {
			if (JOptionPane.showConfirmDialog(null, getDisclaimer()) == JOptionPane.OK_OPTION) {
				cp.setProperty("agree", "true");
				cp.store(new FileOutputStream(System.getProperty("user.home") + "\\MFIoptions.cfg"), null);
				run2();
			}		
		} else {run2();}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private JEditorPane getDisclaimer(){
	    // for copying style
	    JLabel label = new JLabel();
	    Font font = label.getFont();
	
	    // create some css from the label's font
	    StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
	    style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
	    style.append("font-size:" + font.getSize() + "pt;");
	
	    // html content
	    JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
	            				+ "This program is distributed in the hope that it will be useful,<br> "
	    					    + "but WITHOUT ANY WARRANTY; without even the implied warranty <br>"
	    					    + "of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. <br><br>"
	    					    + "See the GNU General Public License for more details.<br>" 
	    					    + "MultiFretIcy is available under a GNU public “copyleft” license.<br>" 
	    					    + "For questions about the GNU license refer to the <a href=\"http://www.gnu.org/licenses/gpl-faq.html\">Frequently Asked Questions about the GNU licenses page.</a><br>"
	    					    + "The text can be found <a href=\"http://www.gnu.org/licenses/gpl-3.0-standalone.html\">here.</a>"	            
	    					    + "</body></html>");
	
	    // handle link events
	    ep.addHyperlinkListener(new HyperlinkListener()
	    {
	        @Override
	        public void hyperlinkUpdate(HyperlinkEvent e)
	        {
	            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
					try {
						Desktop.getDesktop().browse(new URI(e.getURL().toString()));
					} catch (IOException | URISyntaxException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} // roll your own link launcher or use Desktop if J6+
	        }
	    });
	    ep.setEditable(false);
	    ep.setBackground(label.getBackground());
	
	    // show
	    //JOptionPane.showMessageDialog(null, ep);
		return ep;
	}
	
	
	public void run2() {		
		System.out.println("hi");
		System.out.printf("MFlocX = %s, MFlocY = %s%n MFwid = %s, MFhei = %s%n",
				Icy.getMainInterface().getMainFrame().getBounds().getLocation().getX(),
				Icy.getMainInterface().getMainFrame().getBounds().getLocation().getY(),
				Icy.getMainInterface().getMainFrame().getBounds().getWidth(),
				Icy.getMainInterface().getMainFrame().getBounds().getHeight());
		
		System.out.printf("DPlocX = %s, DPlocY = %s%n DPwid = %s, DPhei = %s%n",
				Icy.getMainInterface().getDesktopPane().getBounds().getLocation().getX(),
				Icy.getMainInterface().getDesktopPane().getBounds().getLocation().getY(),
				Icy.getMainInterface().getDesktopPane().getBounds().getWidth(),
				Icy.getMainInterface().getDesktopPane().getBounds().getHeight());
		
		System.out.println("###### MFI Version 0.0.4.1 ######");
		new ToolTipFrame("<html> <body bgcolor=\"#0036d8\"> <font color=\"#f9f1a4\"><b>Running MFI version 0.0.4.1</b></font><br>"
				+ "<font color=\"#14ff0c\"><sup>28/06/2020</sup>Changelog for this version:"
				+ "<br>•Fixed an issue with the template function"
				+ "<br>•Data logs now contain milestone information"
				+ "<br>•Milestones can now be created before 'Start' is clicked, during the ROI drawing stage"
				+ "<br>•Graphs aesthetic has been updated, and should pick axis AutoRange in a more desirable fashion"
				+ "<br>•Deprecated Multipos switch removed"
				+ "<br>•Minor performance tweaks"
				+ "<br>•Exit prompt added, allowing users to choose how to end the experiment"				
				+ "<br>•Workspace functionality added, enabling this will place all viewports into the MultiFret Workspace tab"
				+ "<br>•Transformation functionality added, enabling this will activate enhanced misalignment correction algorithms"
				+ "<br>•Custom corrections functionality added, this allows users to write their own corrections"
				, 20);
		PS = new Prestart();
		PS.run();
	}	
}
