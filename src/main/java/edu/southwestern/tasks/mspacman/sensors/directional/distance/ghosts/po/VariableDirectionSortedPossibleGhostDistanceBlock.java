package edu.southwestern.tasks.mspacman.sensors.directional.distance.ghosts.po;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import edu.southwestern.tasks.mspacman.facades.GameFacade;
import edu.southwestern.tasks.mspacman.sensors.directional.distance.VariableDirectionDistanceBlock;
import edu.southwestern.util.datastructures.Quad;
import edu.southwestern.util.datastructures.Triple;
import pacman.game.Constants.MOVE;

/**
 * Possible ghosts are those that we can see and those that we have a probability for.
 * This block sorts possible ghosts by distance and returns the orderth ghost away's location.
 * 
 * @author Will Price
 *
 */
public class VariableDirectionSortedPossibleGhostDistanceBlock extends VariableDirectionDistanceBlock{

	//NOTES:
	//SEE ANONYMOUS COMPARATOR IN:
	//NearestEscapeNodeThreatDistanceDifferencesBlock line 35
	//SEE HOW TO COMPARE GHOST DISTANCES IN:
	//DirectionalGhostComparator.compare
	//IMPLEMENT GET TARGETS IN A SIMILAR FASHION TO
	//VariableDirectionSortedGhostDistanceBlock
	
	private final int order;
	private final boolean sortThreats;
	private final boolean sortEdibles;
	
	public VariableDirectionSortedPossibleGhostDistanceBlock(int order, boolean sortThreats, boolean sortEdibles) {
		this(-1, order, sortThreats, sortEdibles);
	}
	
	public VariableDirectionSortedPossibleGhostDistanceBlock(int order) {
		this(-1, order, true, true);
	}
	
	
	/**
	 * Handles PO pacman. Considers possible ghost locations on top of ghost locations that are visible.
	 * @param dir the direction this block is observing
	 * @param order the orderith (nth) ghost away
	 */
	public VariableDirectionSortedPossibleGhostDistanceBlock(int dir, int order, boolean sortThreats, boolean sortEdibles) {
		super(dir);
		this.order = order;
		this.sortEdibles = sortEdibles;
		this.sortThreats = sortThreats;
	}

	@Override
	public String getType() {
		if(sortThreats && !sortEdibles) {
			return order + " Closest Possible Threat Ghost in " + dir + "direction";
		} else if(!sortThreats && sortEdibles) {
			return order + " Closest Possible Edible Ghost in " + dir + "direction";
		} else {
			return order + " Closest Possible Ghost in " + dir + "direction";	
		}
	}

	@Override
	public int[] getTargets(GameFacade gf) {
		
		System.out.println(this.getType());
		
		ArrayList<Quad<Integer, MOVE, Double, Double>> ghosts = gf.getPossibleGhostInfo();
		
		if(sortThreats && !sortEdibles) {
			//remove edible ghosts from the list
			for(int i = 0; i < ghosts.size(); i++) {
				if(ghosts.get(i).t4 > 0) {
					ghosts.remove(i);
				}
			}
		} else if(!sortThreats && sortEdibles) {
			//remove threat ghosts from the list
			for(int i = 0; i < ghosts.size(); i++) {
				if(ghosts.get(i).t4 <= 0) {
					ghosts.remove(i);
				}
			}
		} else {
			//DO NOTHING
		}
		
		if (order >= ghosts.size()) {
			return new int[0]; // Target in lair will result in distance of
								// infinity
		}
		
		Collections.sort(ghosts, new Comparator<Quad<Integer, MOVE, Double, Double>>(){

			@Override
			public int compare(Quad<Integer, MOVE, Double, Double> arg0, Quad<Integer, MOVE, Double, Double> arg1) {
				
				//the length of the path from pacman to arg0 (ghost) in dir
				int disToArg0 = gf.getDirectionalPath(gf.getPacmanCurrentNodeIndex(), arg0.t1, dir).length;
				//the length of the path from pacman to arg1 (ghost) in dir
				int disToArg1 = gf.getDirectionalPath(gf.getPacmanCurrentNodeIndex(), arg1.t1, dir).length;
				
				if(disToArg0 > disToArg1) {
					return 1;
				} else if (disToArg0 == disToArg1) {
					return 0;
				} else {
					return -1;
				}
				
			}
		});
		
		//returns the shortest path to the order (1st, 2nd, 3rd, etc) possible ghost away
		return new int[] { ghosts.get(order).t1 };
	}

}
