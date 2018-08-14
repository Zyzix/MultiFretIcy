package plugins.masoud.multifreticy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

import javax.swing.WindowConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.MMVersion;
import org.micromanager.acquisition.AcquisitionEngine;
import org.micromanager.acquisition.AcquisitionWrapperEngine;
import org.micromanager.acquisition.TaggedImageQueue;
import org.micromanager.api.IAcquisitionEngine2010;
import org.micromanager.api.MMTags;
import org.micromanager.api.SequenceSettings;
import org.micromanager.api.TaggedImageAnalyzer;
import org.micromanager.utils.MDUtils;
import org.micromanager.utils.ReportingUtils;

import icy.common.Version;
import icy.file.FileUtil;
import icy.gui.dialog.MessageDialog;
import icy.gui.frame.TitledFrame;
import icy.gui.frame.progress.FailedAnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.system.thread.ThreadUtil;
import icy.util.ClassUtil;
import icy.util.StringUtil;
import mmcorej.CMMCore;
import mmcorej.MMCoreJ;
import mmcorej.StrVector;
import mmcorej.TaggedImage;
import plugins.tprovoost.Microscopy.MicroManager.core.AcquisitionResult;
import plugins.tprovoost.Microscopy.MicroManager.event.AcquisitionListener;
import plugins.tprovoost.Microscopy.MicroManager.event.LiveListener;
import plugins.tprovoost.Microscopy.MicroManager.gui.LoadFrame;
import plugins.tprovoost.Microscopy.MicroManager.gui.LoadingFrame;
import plugins.tprovoost.Microscopy.MicroManager.gui.MMMainFrame;
import plugins.tprovoost.Microscopy.MicroManager.tools.MMUtils;
import plugins.tprovoost.Microscopy.MicroManager.tools.StageMover;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicromanagerPlugin;
import plugins.tprovoost.Microscopy.MicroManagerForIcy.MicroscopePlugin;

import plugins.tprovoost.Microscopy.MicroManager.MicroManager;
import icy.plugin.abstract_.PluginActionable;
//TODO access micromanager core instead of using microscopeplugin
public class MM extends MicroscopePlugin implements LiveListener {
@Override
public void start() {
	try {
		MicroManager.registerListener(this); //Register as listener to acquire notifications from live image feed
		MicroManager.startLiveMode(); //start live acquisition
	} catch (Exception e) {
		//live already running exception
	}	
}
public void liveImgReceived(TaggedImage newImage) {
	try {
		System.out.println(newImage.tags.get(MMTags.Image.WIDTH));
	}catch (JSONException e) { 
		//tags dont exist
	}
		shutdown();
	}

@Override public void liveStarted() {}
@Override public void liveStopped() {}
@Override
public void liveImgReceived(List<TaggedImage> images) {
	// TODO Auto-generated method stub
	System.out.println(images.size());
}


}
