package plugins.masoud.multifreticy;

import icy.plugin.abstract_.PluginActionable;

public class MultiFretIcy extends PluginActionable {

	Prestart PS;
	
	public void main() {
		run();
	}

	@Override
	public void run() {
		System.out.println("###### MFI Version 1.394 ######");
		PS = new Prestart();
		PS.run();
	}

}
