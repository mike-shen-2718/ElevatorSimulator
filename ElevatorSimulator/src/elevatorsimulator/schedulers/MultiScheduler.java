package elevatorsimulator.schedulers;

import java.util.ArrayList;
import java.util.List;

import elevatorsimulator.ElevatorCar;
import elevatorsimulator.Passenger;
import elevatorsimulator.SchedulingAlgorithm;
import elevatorsimulator.Simulator;

/**
 * Represents a scheduler that can switch strategy
 * @author Anton Jansson
 *
 */
public class MultiScheduler implements SchedulingAlgorithm {
	private final List<SchedulingAlgorithm> schedulers = new ArrayList<SchedulingAlgorithm>();
	private int activeScheduler;
	
	/**
	 * Creates a new multi scheduler
	 * @param schedulers The schedulers
	 */
	public MultiScheduler(List<SchedulingAlgorithm> schedulers) {
		this.schedulers.addAll(schedulers);
	}
	
	/**
	 * Switches the active scheduler to the given
	 * @param scheduler The scheduler
	 */
	public void switchTo(int scheduler) {
		this.activeScheduler = scheduler;
	}
	
	@Override
	public void passengerArrived(Simulator simulator, Passenger passenger) {
		this.schedulers.get(this.activeScheduler).passengerArrived(simulator, passenger);
	}
	
	@Override
	public void passengerBoarded(Simulator simulator, ElevatorCar elevatorCar,	Passenger passenger) {
		this.schedulers.get(this.activeScheduler).passengerBoarded(simulator, elevatorCar, passenger);
	}

	@Override
	public void update(Simulator simulator) {
		this.schedulers.get(this.activeScheduler).update(simulator);
	}

	@Override
	public void onIdle(Simulator simulator, ElevatorCar elevatorCar) {
		this.schedulers.get(this.activeScheduler).onIdle(simulator, elevatorCar);
	}
}
