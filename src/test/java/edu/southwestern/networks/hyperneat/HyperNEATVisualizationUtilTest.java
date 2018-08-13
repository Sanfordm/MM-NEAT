package edu.southwestern.networks.hyperneat;

//import org.junit.Test;

import edu.southwestern.MMNEAT.MMNEAT;
import edu.southwestern.evolution.EvolutionaryHistory;
import edu.southwestern.evolution.genotypes.HyperNEATCPPNAndSubstrateArchitectureGenotype;
import edu.southwestern.evolution.genotypes.TWEANNGenotype;
import edu.southwestern.networks.TWEANN;
import edu.southwestern.parameters.Parameters;
import edu.southwestern.tasks.rlglue.tetris.HyperNEATTetrisTask;
import edu.southwestern.util.graphics.DrawingPanel;
import edu.southwestern.util.random.RandomNumbers;


public class HyperNEATVisualizationUtilTest {

	// This is not actually a test because it doesn't make any assertions.
	// It simply makes test visualizations pop up, but this behavior is undesirable for pure terminal environments.
	//@Test
	public void test_substrateVisualization() throws NoSuchMethodException {
		MMNEAT.clearClasses();
		HyperNEATTetrisTask.hardSubstrateReset();
		EvolutionaryHistory.archetypes = null;
		EvolutionaryHistory.setInnovation(0l);
		Parameters.initializeParameterCollections(new String[] {"runNumber:1", "randomSeed:1", "trials:3", "maxGens:500", "mu:50", "io:false", "netio:false", "mating:true", "task:edu.southwestern.tasks.rlglue.tetris.HyperNEATTetrisTask", "rlGlueEnvironment:org.rlcommunity.environments.tetris.Tetris", "rlGlueExtractor:edu.southwestern.tasks.rlglue.featureextractors.tetris.RawTetrisStateExtractor", "tetrisTimeSteps:true", "tetrisBlocksOnScreen:false", "rlGlueAgent:edu.southwestern.tasks.rlglue.tetris.TetrisAfterStateAgent", "splitRawTetrisInputs:true", "senseHolesDifferently:true", "hyperNEAT:true", "genotype:edu.southwestern.evolution.genotypes.HyperNEATCPPNAndSubstrateArchitectureGenotype", "allowMultipleFunctions:true", "ftype:1", "netChangeActivationRate:0.3", "substrateMapping:edu.southwestern.networks.hyperneat.BottomSubstrateMapping", "steps:500000", "perLinkMutateRate:0.05", "netLinkRate:0.4", "netSpliceRate:0.2", "crossoverRate:0.5", "HNProcessDepth:1", "HNProcessWidth:1", "convolution:true", "senseTetrisHolesAsPositive:true", "showNetworks:true", "showCPPN:true", "showWeights:true", "monitorSubstrates:true"});
		MMNEAT.loadClasses();
		EvolutionaryHistory.initArchetype(0);
		TWEANNGenotype.smallerGenotypes = false;

		HyperNEATCPPNAndSubstrateArchitectureGenotype hncasag = new HyperNEATCPPNAndSubstrateArchitectureGenotype();

		//Pair<DrawingPanel, DrawingPanel> panels = CommonTaskUtil.getDrawingPanels(hncasag);
		TWEANN net = hncasag.getPhenotype();
		double[] inputs = RandomNumbers.randomArray(net.numInputs());
		net.process(inputs);
		hncasag.cascadeExpansion(1, 6, 16, SubstrateConnectivity.CTYPE_CONVOLUTION);
//		panels.t1.dispose();
//		panels.t2.dispose();
		for(DrawingPanel dp: TWEANN.subsPanel) {
			dp.dispose();
		}

		net = hncasag.getPhenotype();
		//panels = CommonTaskUtil.getDrawingPanels(hncasag);
		//net.process(RandomNumbers.randomArray(net.numInputs()));
//		panels.t1.dispose();
//		panels.t2.dispose();		
		for(DrawingPanel dp: TWEANN.subsPanel) {
			dp.dispose();
		}
	
		
		hncasag.addLink(-1, -11, 1.0, EvolutionaryHistory.nextInnovation());
		hncasag.addLink(-2, -12, 1.0, EvolutionaryHistory.nextInnovation());
		hncasag.addLink(-3, -13, 1.0, EvolutionaryHistory.nextInnovation());
		
		net = hncasag.getPhenotype();
		//panels = CommonTaskUtil.getDrawingPanels(hncasag);
		//net.process(RandomNumbers.randomArray(net.numInputs()));

	}
}
