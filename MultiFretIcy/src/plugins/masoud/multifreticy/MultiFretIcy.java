package plugins.masoud.multifreticy;

import icy.plugin.abstract_.PluginActionable;

public class MultiFretIcy extends PluginActionable {

	Prestart PS;
	
	@Override
	public void run() {
		PS = new Prestart();
		PS.run();
		// TODO Auto-generated method stub
	}

}
