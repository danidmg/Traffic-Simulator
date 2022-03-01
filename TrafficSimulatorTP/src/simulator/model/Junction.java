package simulator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class Junction extends SimulatedObject {

	List<Road> _inRoads;
    List<List<Vehicle>> _queues; // the i-th queue is of the i-th road in _inRoads
    Map<Road, List<Vehicle>> _queueByRoad; // this is just for fast searching of queues
    Map<Junction, Road> _outRoadByJunction; // to tell how to go to a junction
    int _greenLightIndex;   // the index of the road (in _inRoads) that has green light (-1 all red)
    int _lastSwitchingTime; // the last time we switched the green light from one road to another
    LightSwitchingStrategy _lss;
    DequeuingStrategy _dqs;
    int xCoor;
    int yCoor;
    
    Junction(String id, LightSwitchingStrategy _lss, DequeuingStrategy _dqs, int xCoor, int yCoor) {
		super(id);
		
		if(_lss == null) 
			 throw new IllegalArgumentException("Light Switching Strategy is null");
		if(_dqs == null) 
			 throw new IllegalArgumentException("Dequeueing Switching Strategy is null");
		if( xCoor < 0 ) 
			 throw new IllegalArgumentException("X Coordinate is null");
		if( yCoor < 0 ) 
			 throw new IllegalArgumentException("Y Coordinate is null");
	
		
		this._lss = _lss;
		this._dqs = _dqs;
		this.xCoor = xCoor;
		this.yCoor = yCoor;
		this._greenLightIndex = -1;
		this._lastSwitchingTime = 0;
		
		this._inRoads = new ArrayList<Road>();
		this._queues = new ArrayList<List<Vehicle>>();
		this._queueByRoad = new HashMap<Road, List<Vehicle>>();
		this._outRoadByJunction = new HashMap<Junction, Road>();
	}

    public void addIncomingRoad(Road r) {
        if (r.getDest() == this) {
            this._inRoads.add(r);
            List <Vehicle> queue = new ArrayList<>();
            this._queues.add(queue);
            this._queueByRoad.put(r, queue);
        }
        else
        	throw new IllegalArgumentException ("Invalid Incoming Road");
         
    }

    public void addOutgoingRoad(Road r){
        if(r.getSrc() == this){
            this._outRoadByJunction.put(r.getDest(), r);
        }
        else
        	throw new IllegalArgumentException ("Invalid Incoming Road");
    }

    //Adds the vehicle to the queue of that road
    public void enter(Vehicle v){
    	Road r = v.getRoad();
    	_queueByRoad.get(r).add(v);
    }

    public Road roadTo(Junction j){
        return this._outRoadByJunction.get(j);
    }

    public void advance(int time){
        if (_greenLightIndex != -1) {
        	List<Vehicle> q = _queueByRoad.get(_inRoads.get(_greenLightIndex));
        	List<Vehicle> l = _dqs.dequeue(q);
        	
        	for (Vehicle v : l) {
   	            try {
					v.moveToNextRoad();
					q.remove(v);
				} catch (Exception e) {
					e.printStackTrace();
				} 
   		  	}
        }
        int newGreen = _lss.chooseNextGreen(_inRoads, _queues, _greenLightIndex, _lastSwitchingTime, time);
        if (newGreen != _greenLightIndex)
        {
        	_greenLightIndex = newGreen;
        	_lastSwitchingTime = time;
        }
    }
    
	@Override
	public JSONObject report() {
		 JSONObject jo = new JSONObject();

	        jo.put("id", this._id);
	        if (_greenLightIndex == -1)
	        	jo.put("green", "none");
	        jo.put("green", _inRoads.get(_greenLightIndex));
	        
	        int i = 1;
	        jo.put("queues", _queues);
	        for(List<Vehicle> q : this._queues){
	        	
	        	jo.put("Q", i);
	        	jo.put("road", q.get(0).getRoad());
	        	jo.put("vehicles", q);
	        	i++;
	        }
	       
		return jo;
	}

}
