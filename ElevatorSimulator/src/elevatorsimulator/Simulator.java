package elevatorsimulator;
import java.util.*;

import elevatorsimulator.schedulers.*;

/**
 * The main class for the simulator
 * @author Anton Jansson
 *
 */
public class Simulator {
	private final SimulatorSettings settings;
	private final SimulatorClock clock;
	
//	private final long randSeed = System.currentTimeMillis();
	private final long randSeed = 1337;
	private Random random;
	
	private final SimulatorStats stats;
	
	private final Building building;
	private final ControlSystem controlSystem;
	
	private long passengerId = 0;
	
	private final boolean enableLog = false;
	private final boolean debugMode = false;
	
	/**
	 * Creates a new simulator
	 * @param scenario The scenario
	 * @param settings The settings
	 * @param scheduler The scheduler
	 */
	public Simulator(Scenario scenario, SimulatorSettings settings, SchedulerCreator schedulerCreator) {
		this.settings = settings;
		this.clock = new SimulatorClock(settings.getTimeStep());
		this.building = scenario.createBuilding();
		this.controlSystem = new ControlSystem(this, schedulerCreator.createScheduler(this.building));
		this.stats = new SimulatorStats(this);
		this.random = new Random(this.randSeed);
	}
	
	/**
	 * Returns the simulator clock
	 */
	public SimulatorClock getClock() {
		return clock;
	}
	
	/**
	 * Returns the simulator statistics
	 */
	public SimulatorStats getStats() {
		return stats;
	}
	
	/**
	 * Returns the random generator
	 */
	public Random getRandom() {
		return random;
	}
	
	/**
	 * Returns the building
	 */
	public Building getBuilding() {
		return building;
	}
	
	/**
	 * Returns the control system
	 */
	public ControlSystem getControlSystem() {
		return controlSystem;
	}
	
	/**
	 * Moves the simulation forward one time step
	 * @param duration The elapsed time since the last time step
	 */
	public void moveForward(long duration) {
		this.building.update(this, duration);
		this.controlSystem.update(duration);
	}
	
	/**
	 * Logs the given line
	 * @param line The line
	 */
	public void log(String line) {
		if (enableLog) {
			long simulatedTime = this.clock.elapsedSinceRealTime(0);
			double numHours = simulatedTime / (60.0 * 60.0 * SimulatorClock.NANOSECONDS_PER_SECOND);
			double numMin = 60 * (numHours - (int)numHours);
			double numSec = 60 * (numMin - (int)numMin);
			
			int hour = (int)numHours;
			int min = (int)numMin;
			int sec = (int)numSec;
			
			String timeStr = (hour < 10 ? "0" + hour : hour) + ":" + (min < 10 ? "0" + min : min) + ":" + (sec < 10 ? "0" + sec : sec);
			System.out.println(timeStr + ": " + line);
		}
	}
	
	/**
	 * Logs the given line for an elevator
	 * @param elevatorId The id of the elevator
	 * @param line The line
	 */
	public void elevatorLog(int elevatorId, String line) {
		this.log("Elevator " + elevatorId + ": " + line);
	}
	
	/**
	 * Logs the given debug line for an elevator
	 * @param elevatorId The id of the elevator
	 * @param line The line
	 */
	public void elevatorDebugLog(int elevatorId, String line) {
		if (this.debugMode) {
			this.elevatorLog(elevatorId, line);
		}
	}
	
	/**
	 * Marks that an arrival has been generated
	 * @param passenger The passenger
	 */
	public void arrivalGenerated(Passenger passenger) {
		this.stats.generatedPassenger(passenger);
	}
	
	/**
	 * Indicates if new arrivals can be generated
	 */
	public boolean canGenerateArrivals() {
		return this.clock.simulatedTime() < this.settings.getSimulationTimeInSec() * SimulatorClock.NANOSECONDS_PER_SECOND;
	}
			
	/**
	 * Indicates if all floors are empty
	 */
	private boolean floorsEmpty() {
		for (Floor floor : this.building.getFloors()) {
			if (!floor.getWaitingQueue().isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Indicates if all the elevator cars are empty
	 */
	private boolean elevatorsEmpty() {
		for (ElevatorCar elevator : this.building.getElevatorCars()) {
			if (!elevator.getPassengers().isEmpty()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Runs the simulation
	 */
	public void run() {
		System.out.println(new Date() + ": Simulation started.");
		
		while (true) {
			moveForward((long)(this.settings.getTimeStep() * SimulatorClock.NANOSECONDS_PER_SECOND));
			clock.step();
			
			if (!this.canGenerateArrivals()) {
				if (this.floorsEmpty() && this.elevatorsEmpty()) {
					break;
				}
			}
		}	
		
		System.out.println(new Date() + ": Simulation finished.");		
		System.out.println("--------------------" + this.controlSystem.getSchedulerName() + "--------------------");
		this.stats.printStats();		
	}
	
	private boolean run = false;
		
	/**
	 * Starts the simulator
	 */
	public void start() {
		this.run = true;
	}
	
	/**
	 * Resets the simulator
	 */
	public void reset() {
		this.controlSystem.reset();
		this.building.reset();
		this.clock.reset();
		this.stats.reset();
		this.random = new Random(this.randSeed);
	}
	
	/**
	 * Advances the simulator one step
	 * @return True if there are any more steps
	 */
	public boolean advance() {
		if (this.run) {
			moveForward((long)(this.settings.getTimeStep() * SimulatorClock.NANOSECONDS_PER_SECOND));
			this.clock.step();
			
			if (!this.canGenerateArrivals()) {				
				if (this.floorsEmpty() && this.elevatorsEmpty()) {
					this.run = false;				
					return false;
				}
			}
		} else {
			return false;
		}

		return true;
	}
	
	/**
	 * Prints the statistics
	 */
	public void printStats() {
		this.stats.printStats();
	}
	
	public static void main(String[] args) {
		int[] floors = new int[] {
			0, 80, 70, 90, 80, 115, 120, 90, 80, 90, 80, 100, 80, 80, 50
		};
		
		TrafficProfile profile = TrafficProfiles.WEEK_DAY_PROFILE;
		
		TrafficProfile.Interval[] arrivalRates = new TrafficProfile.Interval[1];
		//arrivalRates[0] = new TrafficProfile.Interval(0.12, 1.0, 0.0);
		//arrivalRates[0] = new TrafficProfile.Interval(0.03, 0.45, 0.45);
//		arrivalRates[0] = new TrafficProfile.Interval(0.03, 0.1, 0.9);
//		arrivalRates[0] = new TrafficProfile.Interval(0.03, 0, 1.0);
		arrivalRates[0] = new TrafficProfile.Interval(0.06, 1, 0);
//		arrivalRates[0] = new TrafficProfile.Interval(0.03, 0.45, 0.45);
				
		SchedulerCreator creator = new SchedulerCreator() {		
			@Override
			public SchedulingAlgorithm createScheduler(Building building) {
//				return new CollectiveControl();
				return new Zoning(building.getElevatorCars().length, building);
//				return new LongestQueueFirst();
//				return new RoundRobin(building, false);
			}
		};
		
		Simulator simulator = new Simulator(
			new Scenario(
				3,
				ElevatorCarConfiguration.defaultConfiguration(),
				floors,
				new TrafficProfile(arrivalRates)),
//				profile),
			new SimulatorSettings(0.01, 1 * 60 * 60),
			creator);
		
		simulator.run();
	}
	
	/**
	 * Returns the next passenger id
	 */
	public long nextPassengerId() {
		return this.passengerId++;
	}
}
