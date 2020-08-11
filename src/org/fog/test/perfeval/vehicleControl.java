package org.fog.test.perfeval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

/**
 * Simulation setup for testing scalability of iFogSim, using a vehicular environment
 * @author Tzu-Tao, Chang
 *
 */
public class vehicleControl{
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();

	/* Parameters setup */
	static boolean CLOUD = false; // whether using a cloud-only architecture
	
	static int numOfRoadSection = 1;
	static int numOfRsusPerRoadSection = 4;
	static int numOfCarsPerRsu = 3;
	static double SENSOR_TRANSMISSION_TIME = 10; //ms
	
	public static void main(String[] args) {
		Log.printLine("Starting vehicular control...");
			
		try {
			Log.disable();
			
			/* CloudSim initiation */
			int numOfCloudUser = 1;
			Calendar calendar = Calendar.getInstance();
			boolean traceFlag = false;
			CloudSim.init(numOfCloudUser, calendar, traceFlag);
			
			/* Create Application */
			String appId = "vehicularControl";
			FogBroker fogBroker = new FogBroker("broker");
			Application application = createApplication(appId, fogBroker.getId());
			application.setUserId(fogBroker.getId());
			
			/* Create physical devices */
			createFogDevices(appId, fogBroker.getId());
			/* set fog broker Id */
			application.setUserId(fogBroker.getId());
			
			/* Create module mapping */
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
			moduleMapping.addModuleToDevice("Monitor", "cloud"); // add the Monitor module to cloud device
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("car")){
					moduleMapping.addModuleToDevice("Client", device.getName());   // add the Client module to car
				}
			}
			
			if(CLOUD) {
				moduleMapping.addModuleToDevice("Calculator", "cloud");
			} else {
				for(FogDevice device : fogDevices){
					if(device.getName().startsWith("rsu")){
						moduleMapping.addModuleToDevice("Calculator", device.getName()); 
					}
				}
			}
			
			/* Create controller */
			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
			controller.submitApplication(application, 0, 
					(CLOUD)?(new ModulePlacementMapping(fogDevices, application, moduleMapping))
							:(new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));
			
			/* Simulation */
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			System.out.println("DONE");
			Log.printLine("VRGame finished!");
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("SOMETHING WRONG");
			Log.printLine("Unwanted error happened.");
		}
	}
	
	private static Application createApplication(String appId, int fogBrokerId) {
		/* Create empty application */
		Application application = Application.createApplication(appId, fogBrokerId);
		
		/* Add modules to the application */
		application.addAppModule("Client", 10);
		application.addAppModule("Calculator", 10);
		application.addAppModule("Monitor", 10);
		
		/* Add edges between modules */
		application.addAppEdge("SENSOR", "Client", 4000, 500, "SENSOR", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("Client", "Calculator", 4500, 500, "CAR_CONDITION", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("Calculator", "Monitor", 100, 500, 500, "LOCAL_TRAFFIC_STATE", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("Calculator", "Client", 1000, 500, "MOVEMENT_INSTRUCTION", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("Client", "ACTUATOR", 1000, 500, "MOVEMENT_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);
		
		/* Define input-output relationship for each module */
		application.addTupleMapping("Client", "SENSOR", "CAR_CONDITION", new FractionalSelectivity(1.0));
		application.addTupleMapping("Calculator", "CAR_CONDITION", "MOVEMENT_INSTRUCTION", new FractionalSelectivity(1.0));
		application.addTupleMapping("Client", "MOVEMENT_INSTRUCTION", "MOVEMENT_UPDATE", new FractionalSelectivity(1.0));
		//application.addTupleMapping("Calculator", "CAR_CONDITION", "LOCAL_TRAFFIC_STATE", new FractionalSelectivity(0.8));
		
		/* Define application loop to monitor latency */
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(Arrays.asList("SENSOR", "Client", "Calculator", "Client", "ACTUATOR")));
		List<AppLoop> loops = new ArrayList<AppLoop>(Arrays.asList(loop1));
		application.setLoops(loops);
		
		
		
		return application;
	}
	
	private static void createFogDevices(String appId, int fogBrokerId) {
		/* create cloud */
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
		cloud.setParentId(-1);
		fogDevices.add(cloud);
		
		/* create proxy server */
		FogDevice proxy = createFogDevice("proxy", 2800, 4000, 10000, 10000, 1, 100.0, 0.0, 107.339, 83.4333); // creates the fog device Proxy (level=1)
		proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy 
		fogDevices.add(proxy);
		
		/* create devices in each road section */
		for(int i = 0; i < numOfRoadSection; ++i) {
			createRoadSection(i+"", appId, fogBrokerId, proxy.getId());
		}
	}
	
	private static void createRoadSection(String roadSectionName, String appId, int fogBrokerId, int proxyId) {
		for(int i = 0; i < numOfRsusPerRoadSection; ++i) {
			/* create road side unit */
			String rsuName = "rsu-" + roadSectionName + "-" + i;
			FogDevice rsu = createFogDevice(rsuName, 50000, 1000, 10000, 10000, 2, 6.0, 0.0, 107.339, 83.4333);
			rsu.setParentId(proxyId);
			
			
			
			/* create cars (sensors and actuators) */
			for(int j = 0; j < numOfCarsPerRsu; ++j) {
				createCar(roadSectionName + "-" + i + "-" + j, appId, fogBrokerId, rsu.getId());
			}
			fogDevices.add(rsu);
		}
	}
	
	private static void createCar(String subId, String appId, int fogBrokerId, int rsuId) {
		FogDevice car = createFogDevice("car-" + subId, 1000, 1000, 2000, 1000, 3, 3, 0, 87.53, 82.44);
		car.setParentId(rsuId);
		fogDevices.add(car);
		createSensor("sensor-" + subId, appId, fogBrokerId, car.getId());
		createActuator("actuator-" + subId, appId, fogBrokerId, car.getId());
	}
	
	private static FogDevice createFogDevice(String deviceName, long mips, int ram, long upBw, long downBw, int level, double uplinkLatency, double ratePerMips, double busyPower, double idlePower) {
		/* create list of processing elements */
		List<Pe> peList = new ArrayList<Pe>();
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
		
		/* create power host */
		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);
		
		/* create fog device characteristics */
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);
		
		/* create fog device */
		FogDevice fogdevice = null;
		try {	
			fogdevice = new FogDevice(deviceName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, uplinkLatency, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		fogdevice.setLevel(level);
		return fogdevice;
	}
	
	private static void createSensor(String sensorName, String appId, int fogBrokerId, int rsuId) {
		Sensor carSensor = new Sensor(sensorName, "SENSOR", fogBrokerId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME));
		carSensor.setGatewayDeviceId(rsuId);
		carSensor.setLatency(2.0);
		sensors.add(carSensor);
	}
	
	private static void createActuator(String actuatorName, String appId, int fogBrokerId, int rsuId) {
		Actuator carActuator = new Actuator(actuatorName, fogBrokerId, appId, "ACTUATOR");
		carActuator.setGatewayDeviceId(rsuId);
		carActuator.setLatency(1.0);
		actuators.add(carActuator);
	}

}