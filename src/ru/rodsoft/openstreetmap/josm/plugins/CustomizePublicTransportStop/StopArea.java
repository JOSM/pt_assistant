package ru.rodsoft.openstreetmap.josm.plugins.CustomizePublicTransportStop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * 
 * @author Rodion Scherbakov
 * Stop area settings
 */
public class StopArea {
	public static final String AREA_TAG = "area";
	public static final String COVERED_TAG = "covered";
	public static final String SHELTER_TAG = "shelter";
	private static final String BENCH_TAG = "bench";
	public static final String TRAIN_TAG_VALUE = "train";
	public static final String STOP_POSITION_TAG_VALUE = "stop_position";
	public static final String STATION_TAG_VALUE = "station";
	public static final String HALT_TAG_VALUE = "halt";
	public static final String YES_TAG_VALUE = "yes";
	public static final String RAILWAY_TAG = "railway";
	public static final String TRAM_STOP_TAG_VALUE = "tram_stop";
	public static final String TRAM_TAG = "tram";
	public static final String SHARE_TAXI_TAG = "share_taxi";
	public static final String TROLLEYBUS_TAG = "trolleybus";
	public static final String BUS_TAG = "bus";
	public static final String NETWORK_TAG = "network";
	public static final String OPERATOR_TAG = "operator";
	public static final String NAME_EN_TAG = "name:en";
	public static final String NAME_TAG = "name";
	public static final String HIGHWAY_TAG = "highway";
	public static final String BUS_STOP_TAG_VALUE = "bus_stop";
	public static final String TYPE_TAG = "type";
	public static final String PUBLIC_TRANSPORT_TAG = "public_transport";
	public static final String STOP_AREA_TAG_VALUE = "stop_area";
	public static final String STOP_ROLE = "stop";
	public static final String PLATFORM_ROLE = "platform";
	public static final String PLATFORM_TAG_VALUE = "platform";
	public static final String SERVICE_TAG = "service";
    public static final String LONG_DISTANCE_NETWORK_TAG_VALUE = "long_distance";
	public static final String REGIONAL_NETWORK_TAG_VALUE = "regional";
	public static final String LOCAL_NETWORK_TAG_VALUE = "local";
	public static final String CITY_NETWORK_TAG_VALUE = "city";


	/**
	 * Name of stop area
	 */
	public String name = null;
	/**
	 * English name of stop area
	 */
	public String nameEn = null;
	/**
	 * Operator of stop area
	 */
	public String operator = null;
	/**
	 * Network name
	 */
	public String network = null;
	/**
	 * Level of network including this stop area
	 */
	public String service = null;
	/**
	 * Flag of bus stop area
	 */
	public Boolean isBus = false;
	/**
	 * Flag of trolleybus stop area
	 */
	public Boolean isTrolleybus = false;
	/**
	 * Flag of share taxi stop area
	 */
	public Boolean isShareTaxi = false;
	/**
	 * Flag of tram stop area
	 */
	public Boolean isTram = false;
	/**
	 * Flag of railway stop area
	 */
	public Boolean isTrainStop = false;
	/**
	 * Flag of railway station
	 */
	public Boolean isTrainStation = false;
	/**
	 * Flag of bench on selected platform
	 */
	public Boolean isBench = false;
	/**
	 * Flag of covered platform 
	 */
	public Boolean isCovered = false;
	/**
	 * Flag of shelter on selected platform
	 */
	public Boolean isShelter = false;
	/**
	 * Relation of stop area
	 */
	public Relation thisRelation = null;
	/**
	 * Flag of existing of stop position
	 */
	public Boolean isStopPointExists = false;
	/**
	 * Flag of area platform
	 */
	public Boolean isArea = false;
	/**
	 * List of nodes of stop positions
	 */
	public final ArrayList<Node> stopPoints = new ArrayList<Node>();
	/**
	 * List of josm objects of platforms
	 */
	public final ArrayList<OsmPrimitive> platforms = new ArrayList<OsmPrimitive>();
	/**
	 * List of non stop positions or platform stop area members
	 */
	public final ArrayList<OsmPrimitive> otherMembers = new ArrayList<OsmPrimitive>();
	
	/**
	 * Selected josm objects
	 */
	public OsmPrimitive selectedObject = null;
	
	/**
	 * Constructor of stop area object
	 */
	public StopArea()
	{ }
	
	/**
	 * Constructor of stop area object from selected object
	 * @param selectedObject Selected object
	 */
	public StopArea(OsmPrimitive selectedObject)
	{
		this.selectedObject = selectedObject;
		fromSelectedObject(selectedObject);
	}
	
	/**
	 * Comparing of value of tag for josm object
	 * @param osmObject Josm object
	 * @param tagName Tag name
	 * @param tagValue Tag value
	 * @return true, if tag exists and equals tagValue
	 */
	public static Boolean compareTag (OsmPrimitive osmObject, String tagName, String tagValue)
	{
		String value = osmObject.getKeys().get(tagName);
		if(value != null)
			return value.equals(tagValue);
		return false;
	}
	
	/**
	 * Parsing of josm object tags and customizing of stop area object
	 * @param member Josm object
	 */
	public void ParseTags(OsmPrimitive member)
	{
		if(name == null)
			this.name = member.getKeys().get(NAME_TAG);
		if(nameEn == null)
			this.nameEn = member.getKeys().get(NAME_EN_TAG);
		if(operator == null)
			this.operator = member.getKeys().get(OPERATOR_TAG);
		if(network == null)
			this.network = member.getKeys().get(NETWORK_TAG);
		if(service == null)
			this.service = member.getKeys().get(SERVICE_TAG);
		if(YES_TAG_VALUE.equals(member.getKeys().get(BUS_TAG)))
			this.isBus = true;
		if(YES_TAG_VALUE.equals(member.getKeys().get(TROLLEYBUS_TAG)))
			this.isTrolleybus = true;
		if(YES_TAG_VALUE.equals(member.getKeys().get(SHARE_TAXI_TAG)))
			this.isShareTaxi = true;
		if(!(this.isBus || this.isShareTaxi || this.isTrolleybus) && BUS_STOP_TAG_VALUE.equals(member.getKeys().get(HIGHWAY_TAG)))
			this.isBus = true;
		if(YES_TAG_VALUE.equals(member.getKeys().get(TRAM_TAG)) || TRAM_STOP_TAG_VALUE.equals(member.getKeys().get(RAILWAY_TAG)))
			this.isTram = true;
		if(HALT_TAG_VALUE.equals(member.getKeys().get(RAILWAY_TAG)))
			this.isTrainStop = true;
		if(STATION_TAG_VALUE.equals(member.getKeys().get(RAILWAY_TAG)))
			this.isTrainStation = true;
		if(member == this.selectedObject)
		{
			if(YES_TAG_VALUE.equals(member.getKeys().get(BENCH_TAG)))
				this.isBench = true;
			if(YES_TAG_VALUE.equals(member.getKeys().get(SHELTER_TAG)))
				this.isShelter = true;
			if(YES_TAG_VALUE.equals(member.getKeys().get(COVERED_TAG)))
				this.isCovered = true;
			if(YES_TAG_VALUE.equals(member.getKeys().get(AREA_TAG)))
				this.isArea = true;
		}
	}
	
	/**
	 * Get selected in editor node
	 * @return Selected node or null
	 */
	public Node getSelectedNode()
	{
		if(selectedObject instanceof Node)
			return (Node)selectedObject;
		return null;
	}
	
	/**
	 * Get selected way
	 * @return Selected way or null
	 */
	public Way getSelectedWay()
	{
		if(selectedObject instanceof Way)
			return (Way)selectedObject;
		return null;
	}
	
	/**
	 * Setting of stop area from selected josm object
	 * @param selectedObject Selected josm object
	 */
	public void fromSelectedObject(OsmPrimitive selectedObject)
	{
		Collection<OsmPrimitive> selectedObjects = new ArrayList<OsmPrimitive>();
		selectedObjects.add(selectedObject);
		for(Relation rel : OsmPrimitive.getParentRelations(selectedObjects))
		{
			if(compareTag(rel, TYPE_TAG, PUBLIC_TRANSPORT_TAG) && compareTag(rel, PUBLIC_TRANSPORT_TAG, STOP_AREA_TAG_VALUE))
			{
				thisRelation = rel;
			}
			if(thisRelation != null)
				break;
		}
		
		if(thisRelation != null)
		{
			ParseTags(thisRelation);
			ParseTags(selectedObject);
			for(RelationMember member : thisRelation.getMembers())
			{				
				if(member.getMember() instanceof Node && STOP_POSITION_TAG_VALUE.equals(member.getMember().getKeys().get(PUBLIC_TRANSPORT_TAG)))
				{
					this.isStopPointExists = true;
					this.stopPoints.add(member.getNode());
				}
				else
					if(PLATFORM_TAG_VALUE.equals(member.getMember().getKeys().get(PUBLIC_TRANSPORT_TAG)))
					{
						this.platforms.add(member.getMember());
						this.ParseTags(member.getMember());
					}
					else
					{
						this.otherMembers.add(member.getMember());
					}
				this.ParseTags(member.getMember());
			}
			if(!this.platforms.contains(selectedObject))
				this.platforms.add(selectedObject);
		}
		else
		{
			this.ParseTags(selectedObject);
			this.platforms.add(selectedObject);
		}
		if(!(this.isBus || this.isTrolleybus || this.isShareTaxi) && selectedObject != null && compareTag(selectedObject, HIGHWAY_TAG, BUS_STOP_TAG_VALUE))
		{
			this.isBus = true;
		}
	}
	
	/**
	 * Forming commands for josm for saving name and name:en attributes stop of area members and relation attributes
	 * @param target Stop area member or relation
	 * @param commands List of commands
	 * @return Resulting list of commands
	 */
    public List<Command> nameTagAssign(OsmPrimitive target, List<Command> commands)
    {
    	if(commands == null)
    		commands = new ArrayList<Command>();
    	
    	StopArea stopArea = this;
    	commands.add(new ChangePropertyCommand(target, NAME_TAG, "".equals(stopArea.name) ? null : stopArea.name));
    	commands.add(new ChangePropertyCommand(target, NAME_EN_TAG, "".equals(stopArea.nameEn) ? null : stopArea.nameEn));
    	return commands;
    }
	
	/**
	 * Forming commands for josm for saving general attributes of stop area members and relation 
	 * @param target Stop area member or relation
	 * @param commands List of commands
	 * @return Resulting list of commands
	 */
    public List<Command> generalTagAssign(OsmPrimitive target, List<Command> commands, Boolean isStopPoint)
    {
    	if(commands == null)
    		commands = new ArrayList<Command>();
    	
    	StopArea stopArea = this;
    	commands = nameTagAssign(target,commands);
    	commands.add(new ChangePropertyCommand(target, NETWORK_TAG, "".equals(stopArea.network) ? null : stopArea.network));
    	commands.add(new ChangePropertyCommand(target, OPERATOR_TAG, "".equals(stopArea.operator) ? null : stopArea.operator));
    	commands.add(new ChangePropertyCommand(target, SERVICE_TAG, null == stopArea.service || "city".equals(stopArea.service) ? null : stopArea.service));
    	
    	transportTypeTagAssign(target, commands, isStopPoint, stopArea);
    	if(target instanceof Relation)
    		return commands;
    	commands.add(new ChangePropertyCommand(target, TRAIN_TAG_VALUE, stopArea.isTrainStop || stopArea.isTrainStation ? YES_TAG_VALUE : null));
    	return commands;
    }

	protected void transportTypeTagAssign(OsmPrimitive target,
			List<Command> commands, Boolean isStopPoint, StopArea stopArea) {
		if(isStopPoint && (stopArea.isTrainStop || stopArea.isTrainStation))
    	{
    		
    		commands.add(new ChangePropertyCommand(target, BUS_TAG, null));
    		commands.add(new ChangePropertyCommand(target, SHARE_TAXI_TAG, null));
    		commands.add(new ChangePropertyCommand(target, TROLLEYBUS_TAG, null));
    		commands.add(new ChangePropertyCommand(target, TRAM_TAG, null));
    	}
    	else
    	{
    		commands.add(new ChangePropertyCommand(target, BUS_TAG, stopArea.isBus ? YES_TAG_VALUE : null));
    		commands.add(new ChangePropertyCommand(target, SHARE_TAXI_TAG, stopArea.isShareTaxi ? YES_TAG_VALUE : null));
    		commands.add(new ChangePropertyCommand(target, TROLLEYBUS_TAG, stopArea.isTrolleybus ? YES_TAG_VALUE : null));
    		commands.add(new ChangePropertyCommand(target, TRAM_TAG, stopArea.isTram ? YES_TAG_VALUE : null));
    	}
	}
    
    /**
     * Forming commands for josm for saving stop position attributes
     * @param target Stop position node
     * @param commands Original command list
     * @param isFirst true, if target is first stop position in stop area
     * @return Resulting command list
     */
    public List<Command> stopPointTagAssign(OsmPrimitive target, List<Command> commands, Boolean isFirst)
    {
    	if(commands == null)
    		commands = new ArrayList<Command>();
    	
    	StopArea stopArea = this;
    	generalTagAssign(target, commands, true);
    	if(isFirst)
    	{
    		if(stopArea.isTrainStop)
    		{
    			commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, HALT_TAG_VALUE));
    		}
    		else
    			if(stopArea.isTrainStation)
    			{
    				commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, STATION_TAG_VALUE));
    			}
    			else
    				if(stopArea.isTram)
    					commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, TRAM_STOP_TAG_VALUE));
    				else
    					commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, null));
    	}
    	else
    	{
    		commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, null));
    	}
    	if(BUS_STOP_TAG_VALUE.equals(target.getKeys().get(HIGHWAY_TAG)))
    		commands.add(new ChangePropertyCommand(target, HIGHWAY_TAG, null));
    	commands.add(new ChangePropertyCommand(target, PUBLIC_TRANSPORT_TAG, STOP_POSITION_TAG_VALUE));
    	return commands;		
    }
    
    /**
     * Forming commands for josm for saving platform attributes
     * @param target Platform node or way
     * @param commands Original command list
     * @param isSelected true, if this platform is selected in editor
     * @param isFirst true, if this platform is first in stop area
     * @return Resulting command list
     */
    public List<Command> platformTagAssign(OsmPrimitive target, List<Command> commands, Boolean isSelected, Boolean isFirst)
    {
    	if(commands == null)
    		commands = new ArrayList<Command>();
    	generalTagAssign(target, commands, false);
    	
    	StopArea stopArea = this;
    	if(HALT_TAG_VALUE.equals(target.getKeys().get(RAILWAY_TAG)) || STATION_TAG_VALUE.equals(target.getKeys().get(RAILWAY_TAG)))
    		commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, null));
    	if(target instanceof Way && (stopArea.isTrainStop || stopArea.isTrainStation || stopArea.isTram))
    		commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, PLATFORM_TAG_VALUE));
    	if(stopArea.isBus || stopArea.isShareTaxi || stopArea.isTrolleybus)
    	{
        	if(target instanceof Way)
        		commands.add(new ChangePropertyCommand(target, HIGHWAY_TAG, PLATFORM_TAG_VALUE));
        	else
        		if(isFirst)
        			commands.add(new ChangePropertyCommand(target, HIGHWAY_TAG, BUS_STOP_TAG_VALUE));
    	}
        commands.add(new ChangePropertyCommand(target, PUBLIC_TRANSPORT_TAG, PLATFORM_TAG_VALUE));
        if(target == selectedObject)
        {
    		commands.add(new ChangePropertyCommand(target, BENCH_TAG, stopArea.isBench ? YES_TAG_VALUE : null));
    		commands.add(new ChangePropertyCommand(target, SHELTER_TAG, stopArea.isShelter ? YES_TAG_VALUE : null));
    		commands.add(new ChangePropertyCommand(target, COVERED_TAG, stopArea.isCovered ? YES_TAG_VALUE : null));
    		commands.add(new ChangePropertyCommand(target, AREA_TAG, stopArea.isArea ? YES_TAG_VALUE : null));
        }
    	return commands;
    }
    
    /**
     * Forming commands for josm for saving attributes of non stop position or platform
     * @param target Member of stop area relation 
     * @param commands Original command list
     * @return Resulting command list
     */
    public List<Command> otherMemberTagAssign(OsmPrimitive target, List<Command> commands)
    {
    	if(commands == null)
    		commands = new ArrayList<Command>();
    	generalTagAssign(target, commands, false);
    	commands.add(new ChangePropertyCommand(target, NAME_TAG, "".equals(name) ? null : target.hasKey(NAME_TAG) ? name : null));
    	commands.add(new ChangePropertyCommand(target, NAME_EN_TAG, "".equals(nameEn) ? null : target.hasKey(NAME_EN_TAG) ? nameEn : null));
    	commands.add(new ChangePropertyCommand(target, NETWORK_TAG, "".equals(network) ? null : target.hasKey(NETWORK_TAG) ? network : null));
    	commands.add(new ChangePropertyCommand(target, OPERATOR_TAG, "".equals(operator) ? null : target.hasKey(OPERATOR_TAG) ? operator : null));
    	commands.add(new ChangePropertyCommand(target, SERVICE_TAG, null == service || "city".equals(service) ? null : service));
    	if(HALT_TAG_VALUE.equals(target.getKeys().get(RAILWAY_TAG)) || STATION_TAG_VALUE.equals(target.getKeys().get(RAILWAY_TAG)))
    		commands.add(new ChangePropertyCommand(target, RAILWAY_TAG, null));
    	return commands;
    } 	
    	
    /**
     * Forming commands for josm for saving stop area relation attributes
     * @param commands Original command list
     * @return Resulting command list
     */
	private List<Command> createStopAreaRelation(List<Command> commands) {
		Relation newRelation = new Relation();
		for(Node node : stopPoints)
		{
			newRelation.addMember(new RelationMember(STOP_ROLE, node));
		}
		for(OsmPrimitive platform : platforms)
		{
			newRelation.addMember(new RelationMember(PLATFORM_ROLE, platform));
		}
		for(OsmPrimitive otherMember : otherMembers)
		{
			newRelation.addMember(new RelationMember("", otherMember));
		}
		Main.main.undoRedo.add(new AddCommand(newRelation));
    	if(commands == null)
    		commands = new ArrayList<Command>();
		generalTagAssign(newRelation, commands, false);
    	commands.add(new ChangePropertyCommand(newRelation, TYPE_TAG, PUBLIC_TRANSPORT_TAG));
    	commands.add(new ChangePropertyCommand(newRelation, PUBLIC_TRANSPORT_TAG, STOP_AREA_TAG_VALUE));
    	return commands;
	}

	/**
	 * Forming commands for josm for saving stop area members and relation attributes
	 * @return Resulting command list
	 */
	public List<Command> customize()
	{
		List<Command> commands = null;
		Boolean isFirst = true;
		for(Node node : stopPoints)
		{
			commands = stopPointTagAssign(node, commands, isFirst);
			isFirst = false;
		}
		isFirst = true;
		for(OsmPrimitive platform : platforms)
		{
			commands = platformTagAssign(platform, commands, platform == selectedObject, isFirst);
			isFirst = false;
		}
		for(OsmPrimitive otherMember : otherMembers)
		{
			commands = nameTagAssign(otherMember, commands);
		}
		if(thisRelation == null)
		{
			if(stopPoints.size() + platforms.size() + otherMembers.size() > 1)
			{
				commands = createStopAreaRelation(commands);
			}
		}
		else
			commands = generalTagAssign(thisRelation, commands, false);
		return commands;
	}

	/**
	 * Testing, is josm object bus stop node or contains bus stop node
	 * @param member Josm object
	 * @return true, if josm object is bus stop node or contains bus stop node
	 */
	private boolean searchBusStop(OsmPrimitive member)
	{
		if(member instanceof Node)
		{
			if(compareTag(member, HIGHWAY_TAG, BUS_STOP_TAG_VALUE))
			{
				return true;
			}
		}
		else
		{
			Way memberWay = (Way) member;
			for(Node node : memberWay.getNodes())
			{
				if(compareTag(node, HIGHWAY_TAG, BUS_STOP_TAG_VALUE))
				{
					return true;
				}					
			}
		}
		return false;
	}
	
	/**
	 * Testing, do stop area contains bus stop node
	 * @return true, if stop area contains bus stop node
	 */
	public boolean searchBusStop() 
	{
		for(OsmPrimitive platform : platforms)
		{
			if(searchBusStop(platform))
				return true;
		}
		for(OsmPrimitive otherMember : this.otherMembers)
		{
			if(searchBusStop(otherMember))
				return true;
		}
		return false;
	}
	
	/**
	 * Testing, must stop area to have separate bus stop node
	 * @return True, stop area must to have separate bus stop node
	 */
	public boolean needBusStop()
	{
		if((this.isBus || this.isShareTaxi || this.isTrolleybus) && (selectedObject instanceof Way))
		{
			return true;
		}
		return false;
	}
	
}