package ru.rodsoft.openstreetmap.josm.plugins.CustomizePublicTransportStop;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * 
 * @author Rodion Scherbakov
 * Class of plugin of customizing of stop area
 * Plugin for josm editor
 */
public class CustomizePublicTransportStopPlugin  extends Plugin
{
	private CustomizeStopAction stopAreaCreatorAction;

    public CustomizePublicTransportStopPlugin(PluginInformation info) {
        super(info);
        stopAreaCreatorAction = CustomizeStopAction.createCustomizeStopAction();
        Main.main.menu.toolsMenu.add(stopAreaCreatorAction);
        System.out.println(getPluginDir());
    }
    
}
