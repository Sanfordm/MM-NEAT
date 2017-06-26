package edu.utexas.cs.nn.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.utexas.cs.nn.MMNEAT.MMNEAT;
import edu.utexas.cs.nn.evolution.genotypes.Genotype;
import edu.utexas.cs.nn.parameters.CommonConstants;
import edu.utexas.cs.nn.scores.MultiObjectiveScore;
import edu.utexas.cs.nn.scores.Score;
import edu.utexas.cs.nn.util.ClassCreation;
import edu.utexas.cs.nn.util.PopulationUtil;
import edu.utexas.cs.nn.util.datastructures.Pair;
import edu.utexas.cs.nn.util.graphics.DrawingPanel;
import edu.utexas.cs.nn.util.stats.Statistic;

public abstract class SinglePopulationCoevolutionTask<T> implements SinglePopulationTask<T>{

	private Statistic stat;

	public SinglePopulationCoevolutionTask() {
		try {
			stat = (Statistic) ClassCreation.createObject("noisyTaskStat");
		} catch (NoSuchMethodException ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	//most tasks dont need to do anything here,
	public void preEval(){};
	
	@Override
	public double getTimeStamp() {
		// Many Domains don't use TimeStamp
		return 0;
	}

	@Override
	public void finalCleanup() {
		// Default to empty
	}

	public abstract int groupSize();
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Score<T>> evaluateAll(ArrayList<Genotype<T>> population) {
		// Used to randomly group agents in the population
		ArrayList<Integer> groupOrder = new ArrayList<Integer>(population.size());
		// Track scores
		ArrayList<ArrayList<Pair<double[], double[]>>> allScores = new ArrayList<ArrayList<Pair<double[], double[]>>>(population.size());
		for(int i = 0; i < population.size(); i++) {
			groupOrder.add(i); // Index in population
			allScores.add(new ArrayList<Pair<double[], double[]>>(CommonConstants.trials)); // anticipate one batch of scores per trial
		}
		
		int groupSize = groupSize(); // Replace: make task dependent
		
		assert population.size()%groupSize == 0 : "Population size " + population.size() + " should be divisible by group size " + groupSize;
		
		double[] bestObjectives = minScores();
		Genotype<T>[] bestGenotypes = new Genotype[bestObjectives.length];
		Score<T>[] bestScores = new Score[bestObjectives.length];
		
		for(int i = 0; i < CommonConstants.trials; i++){
			Collections.shuffle(groupOrder); // Randomize who individuals are grouped with
			for(int j = 0; j < population.size(); j+= groupSize){ // for each group
				// Assign individuals to the group to be evaluated
				ArrayList<Genotype<T>> group = new ArrayList<Genotype<T>>(groupSize);
				for(int k = 0; k < groupSize; k++) {
					group.add(population.get(groupOrder.get(j+k)));
				}
				preEval();
				
				// Call getDrawingPanels here; every Genotype displays its control Network and CPPN panel
				List<Pair<DrawingPanel, DrawingPanel>> drawPanels = new ArrayList<>(); // Stores the DrawingPanels to be drawn
				
				if(CommonConstants.watch){
					for(Genotype<T> gene : group){ // Creates the DrawingPanels for each Genotype being evaluated
						Pair<DrawingPanel, DrawingPanel> panels = CommonTaskUtil.getDrawingPanels(gene);
						drawPanels.add(panels);
					}
					
					// Draw Panels here
					for(Pair<DrawingPanel, DrawingPanel> panelSet : drawPanels){
						if(panelSet.t1 != null)
							panelSet.t1.setVisibility(true);
						if(panelSet.t2 != null)
							panelSet.t2.setVisibility(true);
					}
				}
				
				// Get scores
				ArrayList<Pair<double[], double[]>> result = evaluateGroup(group);
				
				// Clean up all Panels here
				for(Pair<DrawingPanel, DrawingPanel> panelSet : drawPanels){
					if(panelSet.t1 != null)
						panelSet.t1.dispose();
					if(panelSet.t2 != null)
						panelSet.t2.dispose();
				}
				drawPanels.clear();
				
				// Save scores in the right place
				for(int k = 0; k < groupSize; k++) {
					assert j+k < groupOrder.size() : "Should have "+j+"+"+k+" < " + groupOrder.size();
					assert groupOrder.get(j+k) < allScores.size() : "Should have "+groupOrder.get(j+k)+" < "+allScores.size();
					assert k < result.size() : "Should have "+k+" < "+result.size();
					allScores.get(groupOrder.get(j+k)).add(result.get(k));
				}
			}
		}
		
		// Collect scores
		ArrayList<Score<T>> scores = new ArrayList<Score<T>>(population.size());
		
		for(int k = 0; k < population.size(); k++) {
			double[] fitness = new double[this.numObjectives()];
			// Aggregate each fitness score across all trials
			for (int i = 0; i < fitness.length; i++) {
				// Add aggregation overrides?
				fitness[i] = stat.stat(scoresFromIndividual(allScores.get(k), true, i));
			}
			double[] other = new double[this.numOtherScores()];
			// Aggregate each other score across all trials
			for (int i = 0; i < other.length; i++) {
				// Add aggregation overrides?
				other[i] = stat.stat(scoresFromIndividual(allScores.get(k), false, i));
			}
			// Need way to support behavioral diversity for coevolution
			Score<T> s = new MultiObjectiveScore<T>(population.get(k), fitness, null, other);
			scores.add(s);
		}

		// Cycles through the Population
		for(int i = 0; i < population.size(); i++){
			// Cycles through each Objective
			for(int j = 0; j < bestObjectives.length; j++){
				
				double objectiveScore = scores.get(i).scores[j];
				
				// i == 0 saves first member of the population as the tentative best until a better individual is found
				
				if(i == 0 || objectiveScore >= bestObjectives[j]){
					// update best individual in objective j
					bestGenotypes[j] = scores.get(j).individual;
					bestObjectives[j] = objectiveScore;
					bestScores[j] = scores.get(j);
				}
				
			}
		}
		
		if (CommonConstants.netio) {
			PopulationUtil.saveBestOfCurrentGen(bestObjectives, bestGenotypes, bestScores);
		}
		
		if(MMNEAT.hallOfFame != null){
			List<Genotype<T>> champs = new ArrayList<Genotype<T>>();
			for(Genotype<T> gene : bestGenotypes){
				champs.add(gene);
			}
			MMNEAT.hallOfFame.addChampions(MMNEAT.ea.currentGeneration(), champs);
		}
		
		return scores;
	}

	/**
	 * Helper method; Extracts the information from an ArrayList of Pairs of Double Arrays
	 * 
	 * Returns a double[] containing the data from a specific index of one specified double[] from all Pairs in the given ArrayList
	 * 
	 * @param arrayList The ArrayList from which to extract information
	 * @param fit Is the information needed from the Fitness Pair?
	 * @param column The specified Column to extract information from
	 * @return double[] containing the extracted column of information
	 */
	private double[] scoresFromIndividual(ArrayList<Pair<double[], double[]>> arrayList, boolean fit, int column) {
		double[] info = new double[arrayList.size()];
		int index = 0;
		
		for(Pair<double[], double[]> pair : arrayList){
			double[] doubleArray = fit ? pair.t1 : pair.t2;
			info[index++] = doubleArray[column];
		}
		return info;
	}

	public int numOtherScores() {
		return 0;
	}
	
	public double[] minScores() {
		return new double[this.numObjectives()];
	}
	
	/**
	 * Evaluates the Genotypes in a given ArrayList<Genotype<T>> and saves the fitness for each individual
	 * 
	 * @param group
	 * @return ArrayList<Pair<double[], double[]>> representing the fitness of the individuals in the group
	 */
	public abstract ArrayList<Pair<double[], double[]>> evaluateGroup(ArrayList<Genotype<T>> group);
}
