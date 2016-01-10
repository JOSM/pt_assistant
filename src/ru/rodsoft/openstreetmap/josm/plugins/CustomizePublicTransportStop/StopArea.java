package ru.rodsoft.openstreetmap.josm.plugins.CustomizePublicTransportStop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.coor.LatLon;
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
	public static final String TRAIN_TAG = "train";
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
	public static final String AMENITY_TAG = "amenity";
	public static final String BUS_STATION_TAG_VALUE = "bus_station";
	
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
	 * Flag of bus station stop area
	 */
	public Boolean isBusStation = false;
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
	public Boolean isAssignTransportType = false;
	/**
	 * Flag of bench near platform
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
	 * Separate node of bus stop or bus station
	 */
	public Node separateBusStopNode = null;
	
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
	 * Bus station objects
	 */
	public OsmPrimitive busStationObject = null;

	
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
	 * Assign tag value to osm object
	 * @param commands Command list
	 * @param target Target OSM object
	 * @param tag Tag name
	 * @param tagValue Tag value
	 * @return Resulting command list
	 */
	public static List<Command> assignTag(List<Command> commands, OsmPrimitive target, String tag, String tagValue)
	{
		if(commands == null)
			commands = new ArrayList<Command>();
		commands.add(new ChangePropertyCommand(target, tag, tagValue));
		return commands;
	}
	
	/**
	 * Clear tag value of osm object
	 * @param commands Command list
	 * @param target Target OSM object
	 * @param tag Tag name
	 * @return Resulting command list
	 */
	public static List<Command> clearTag(List<Command> commands, OsmPrimitive target, String tag)
	{
		return assignTag(commands, target, tag, null);
	}
	
    /**
     * Calculation of center of platform, if platform is way
     * @param platform Platform primitive
     * @return Coordinates of center of platform
     */
    public LatLon getCenterOfWay(OsmPrimitive platform)
    {
		if(platform instanceof Way)
		{ 
			//p = mapView.getPoint((Node) stopArea.selectedObject);
			Double sumLat = 0.0;
			Double sumLon = 0.0;
			Integer countNode = 0;
			for(Node node : ((Way) platform).getNodes())
			{
				LatLon coord = node.getCoor();
				sumLat += coord.getX();
				sumLon += coord.getY();
				countNode++;
			}
			return new LatLon(sumLon / countNode, sumLat / countNode);		
		}
		return null;
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
		if(BUS_STATION_TAG_VALUE.equals(member.getKeys().get(AMENITY_TAG)))
		{
			this.isBusStation = true;
		}
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
	 * Test, are transport types assigned to platforms
	 * @param platform Platform object
	 * @return true, if transport types assigned to this platforms
	 */
	public boolean testIsTransportTypeAssigned(OsmPrimitive platform)
	{
		String[] transportTypes = new String[] { BUS_TAG, TROLLEYBUS_TAG, SHARE_TAXI_TAG, TRAM_TAG, TRAIN_TAG };
		for(String transportType : transportTypes)
		{
			if(YES_TAG_VALUE.equals(platform.getKeys().get(transportType)))
				return true;			
		}
		return false;
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
		for(OsmPrimitive platform : this.platforms)
		{
			if(testIsTransportTypeAssigned(platform))
			{
				this.isAssignTransportType = true;
				break;
			}
		}
		if(!(this.isBus || this.isTrolleybus || this.isShareTaxi) && selectedObject != null && (compareTag(selectedObject, HIGHWAY_TAG, BUS_STOP_TAG_VALUE) || isBusStation))
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
    	
    	commands = assignTag(commands, target, NAME_TAG, "".equals(name) ? null : name);
    	commands = assignTag(commands, target, NAME_EN_TAG, "".equals(nameEn) ? null : nameEn);
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
    	
    	commands = nameTagAssign(target,commands);
    	commands = assignTag(commands, target, NETWORK_TAG, "".equals(network) ? null : network);
    	commands = assignTag(commands, target, OPERATOR_TAG, "".equals(operator) ? null : operator);
    	commands = assignTag(commands, target, SERVICE_TAG, null == service || "city".equals(service) ? null : service);
    	
    	transportTypeTagAssign(target, commands, isStopPoint);
    	return commands;
    }

    /**
     * Assign transport type tags to node
     * @param target Josm object for tag assigning 
     * @param commands Command list
     * @param isStopPoint Flag of stop point
     */
	protected void transportTypeTagAssign(OsmPrimitive target,
			List<Command> commands, Boolean isStopPoint) 
	{
		if(isStopPoint)
		{
			if(isTrainStop || isTrainStation)
	    	{
				commands = clearTag(commands, target, BUS_TAG);
				commands = clearTag(commands, target, SHARE_TAXI_TAG);
				commands = clearTag(commands, target, TROLLEYBUS_TAG);
				commands = clearTag(commands, target, TRAM_TAG);
	    		commands = assignTag(commands, target, TRAIN_TAG, YES_TAG_VALUE);
	    	}
	    	else
	    	{
	    		commands = assignTag(commands, target, BUS_TAG, isBus ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, SHARE_TAXI_TAG, isShareTaxi ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, TROLLEYBUS_TAG, isTrolleybus ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, TRAM_TAG, isTram ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, TRAIN_TAG, isTrainStation || isTrainStop ? YES_TAG_VALUE : null);
	    	}
		}
		else
		{
			if(this.isAssignTransportType)
	    	{
	    		commands = assignTag(commands, target, BUS_TAG, isBus ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, SHARE_TAXI_TAG, isShareTaxi ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, TROLLEYBUS_TAG, isTrolleybus ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, TRAM_TAG, isTram ? YES_TAG_VALUE : null);
	    		commands = assignTag(commands, target, TRAIN_TAG, isTrainStation || isTrainStop ? YES_TAG_VALUE : null);
	    	}
			else
			{
	    		commands = clearTag(commands, target, BUS_TAG);
	    		commands = clearTag(commands, target, SHARE_TAXI_TAG);
	    		commands = clearTag(commands, target, TROLLEYBUS_TAG);
	    		commands = clearTag(commands, target, TRAM_TAG);
	    		commands = clearTag(commands, target, TRAIN_TAG);    		
			}
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
    public List<Command> platformTagAssign(OsmPrimitive target, List<Command> commands, Boolean isFirst)
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
        		if(isFirst && !this.isBusStation)
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
/*        if(stopArea.separateBusStopNode == null && isFirst && stopArea.isBusStation)
        {
    		commands.add(new ChangePropertyCommand(target, AMENITY_TAG, BUS_STATION_TAG_VALUE));
        }*/
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
		try
		{
			List<Command> commands = new ArrayList<Command>();
			separateBusStopNode = searchBusStop(StopArea.AMENITY_TAG, StopArea.BUS_STATION_TAG_VALUE);
			if(separateBusStopNode == null)
				separateBusStopNode = searchBusStop(StopArea.HIGHWAY_TAG, StopArea.BUS_STOP_TAG_VALUE);
			if(this.isBusStation)
			{
				clearExcessTags(commands, HIGHWAY_TAG, BUS_STOP_TAG_VALUE);
			}
			else
			{
				clearExcessTags(commands, AMENITY_TAG, BUS_STATION_TAG_VALUE);
			}
			if(!(this.isBus || this.isShareTaxi || this.isTrolleybus))
			{
				clearExcessTags(commands, HIGHWAY_TAG, BUS_STOP_TAG_VALUE);
			}
			Boolean isFirst = true;
			for(Node node : stopPoints)
			{
				commands = stopPointTagAssign(node, commands, isFirst);
				isFirst = false;
			}
			isFirst = true;
			OsmPrimitive firstPlatform = null;
			for(OsmPrimitive platform : platforms)
			{
				commands = platformTagAssign(platform, commands, isFirst);
				if(isFirst)
					firstPlatform = platform;
				isFirst = false;
			}
			if(needBusStop(firstPlatform))
			{
				if(isBusStation)
				{
					if(separateBusStopNode == null)
					{
						commands = createSeparateBusStopNode(commands, firstPlatform, AMENITY_TAG, BUS_STATION_TAG_VALUE);
					}	
					else
					{
						commands = assignTag(commands, separateBusStopNode, AMENITY_TAG, BUS_STATION_TAG_VALUE);
					}
				}
				else
				{
					if(separateBusStopNode == null)
					{
						commands = createSeparateBusStopNode(commands, firstPlatform, HIGHWAY_TAG, BUS_STOP_TAG_VALUE);
					}	
					else
					{
						commands = assignTag(commands, separateBusStopNode, HIGHWAY_TAG, BUS_STOP_TAG_VALUE);
					}
				}
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
			{
				commands = generalTagAssign(thisRelation, commands, false);
				commands = addNewRelationMembers(commands);
			}
			return commands;
		}
		catch(Exception ex)
		{
			MessageBox.ok(ex.getMessage());
		}
		return null;
	}

	/**
	 * Adding of new stop area members to relation
	 * @param commands Original command list
	 * @param targetRelation Stop area relation
	 * @param member Stop area relation member
	 * @param roleName Role name
	 * @return Resulting command list
	 */
	public static List<Command> addNewRelationMember(List<Command> commands, Relation targetRelation, OsmPrimitive member, String roleName)
	{
		for(RelationMember relationMember : targetRelation.getMembers())
		{
			if(relationMember.getMember() == member)
			{
				return commands;
			}
		}
		targetRelation.addMember(new RelationMember(roleName, member));
		commands.add(new ChangeCommand(targetRelation, targetRelation));
		return commands;	
	}
	
	/**
	 * Adding new stop area members to relation
	 * @param commands Original command list
	 * @return Resulting command list
	 */
	private List<Command> addNewRelationMembers(List<Command> commands) 
	{
		for(OsmPrimitive stopPoint : stopPoints)
		{
			commands = addNewRelationMember(commands, this.thisRelation, stopPoint, STOP_ROLE);
		}
		for(OsmPrimitive platform : platforms)
		{
			commands = addNewRelationMember(commands, this.thisRelation, platform, PLATFORM_ROLE);
		}
		for(OsmPrimitive otherMember : this.otherMembers)
		{
			commands = addNewRelationMember(commands, this.thisRelation, otherMember, null);
		}
		return commands;
	}

	/**
	 * Testing, is josm object bus stop node or contains bus stop node
	 * @param member Josm object
	 * @return true, if josm object is bus stop node or contains bus stop node
	 */
	private Node searchBusStop(OsmPrimitive member, String tag, String tagValue)
	{
		if(member instanceof Node)
		{			
			if(compareTag(member, tag, tagValue))
			{
				return (Node)member;
			}
		}
		else
		{
			Way memberWay = (Way) member;
			for(Node node : memberWay.getNodes())
			{
				if(compareTag(node, tag, tagValue))
				{
					return node;
				}					
			}
		}
		return null;
	}
	
	/**
	 * Testing, do stop area contains bus stop node
	 * @return true, if stop area contains bus stop node
	 */
	public Node searchBusStop(String tag, String tagValue) 
	{
		for(OsmPrimitive platform : platforms)
		{
			Node busStopNode = searchBusStop(platform, tag, tagValue);
			if(busStopNode != null)
				return busStopNode;
		}
		for(OsmPrimitive otherMember : this.otherMembers)
		{
			Node busStopNode = searchBusStop(otherMember, tag, tagValue);
			if(busStopNode != null)
				return busStopNode;
		}
		return null;
	}
	
	/**
	 * Testing, must stop area to have separate bus stop node
	 * @return True, stop area must to have separate bus stop node
	 */
	public boolean needBusStop(OsmPrimitive firstPlatform)
	{
		if(((this.isBus || this.isShareTaxi || this.isTrolleybus) && (firstPlatform instanceof Way)) || this.isBusStation)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Testing, is josm object bus stop node or contains bus stop node
	 * @param member Josm object
	 * @return true, if josm object is bus stop node or contains bus stop node
	 */
	private void clearExcessTags(List<Command> commands, OsmPrimitive target, String tag, String tagValue)
	{
		if(compareTag(target, tag, tagValue))
		{
			commands = clearTag(commands, target, tag);
		}
		if(target instanceof Way)
		{
			Way memberWay = (Way) target;
			for(Node node : memberWay.getNodes())
			{
				if(compareTag(node, tag, tagValue))
				{
					commands = clearTag(commands, target, tag);
				}					
			}
		}
	}

	/**
	 * Clear excess tags
	 * @param commands Command list
	 * @param tag Tag name
	 * @param tagValue Tag value
	 */
	public void clearExcessTags(List<Command> commands, String tag, String tagValue)
	{
		for(OsmPrimitive stopPoint : stopPoints)
		{
			clearExcessTags(commands, stopPoint, tag, tagValue);
		}
		for(OsmPrimitive platform : platforms)
		{
			clearExcessTags(commands, platform, tag, tagValue);
		}
		for(OsmPrimitive otherMember : this.otherMembers)
		{
			clearExcessTags(commands, otherMember, tag, tagValue);
		}
 	}
	
	/**
	 * Create separate bus stop node or assign bus stop tag to platform node
	 * @param commands Original command list
	 * @param firstPlatform First platform in stop area relation
	 * @param tag Tag name
	 * @param tagValue Tag value
	 * @return Resulting command list
	 */
	protected List<Command> createSeparateBusStopNode(List<Command> commands, OsmPrimitive firstPlatform, String tag, String tagValue)
	{
		LatLon centerOfPlatform = getCenterOfWay(firstPlatform);
		if(firstPlatform instanceof Way)
		{
			if(centerOfPlatform != null)
			{
				Node newNode =new Node();
				newNode.setCoor(centerOfPlatform);
		    	Main.main.undoRedo.add(new AddCommand(newNode));
		    	Main.main.undoRedo.add(new ChangePropertyCommand(newNode, tag, tagValue));
				commands = assignTag(commands, newNode, tag, tagValue);
				otherMembers.add(newNode);
			}
		}
		else
		{
    		commands = assignTag(commands, firstPlatform, tag, tagValue);
		}
		return commands;
	}
	
}