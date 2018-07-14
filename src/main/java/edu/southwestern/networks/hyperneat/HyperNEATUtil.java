package edu.southwestern.networks.hyperneat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.southwestern.MMNEAT.MMNEAT;
import edu.southwestern.evolution.genotypes.HyperNEATCPPNGenotype;
import edu.southwestern.networks.ActivationFunctions;
import edu.southwestern.parameters.CommonConstants;
import edu.southwestern.parameters.Parameters;
import edu.southwestern.util.datastructures.Pair;
import edu.southwestern.util.datastructures.Quint;
import edu.southwestern.util.datastructures.Triple;
/**
 * Util class containing methods used by hyperNEAT and its tasks
 * 
 * @author Lauren Gillespie
 *
 */
public class HyperNEATUtil {
	//size of grid in substrate drawing. 
	/**
	 * Converts defined MMNEAT task into a HyperNEATTask and returns it
	 * 
	 * @return HyperNEATTask
	 */
	public static HyperNEATTask getHyperNEATTask() {
		HyperNEATTask hnt = (HyperNEATTask) MMNEAT.task;
		return hnt;
	}



	public static int numBiasOutputsNeeded() {
		return numBiasOutputsNeeded(getHyperNEATTask());
	}

	/**
	 * If HyperNEAT neuron bias values are evolved, then this method determines
	 * how many CPPN outputs are needed to specify them: 1 per non-input substrate layer.
	 * @param hnt HyperNEATTask that specifies substrate connectivity
	 * @return number of bias outputs needed by CPPN
	 */
	public static int numBiasOutputsNeeded(HyperNEATTask hnt) {
		// CPPN has no bias outputs if they are not being evolved
		if(!CommonConstants.evolveHyperNEATBias) return 0;

		// If substrate coordinates are inputs to the CPPN, then
		// biases on difference substrates can be different based on the
		// inputs rather than having separate outputs for each substrate.
		if(CommonConstants.substrateBiasLocationInputs) return 1;

		List<Substrate> subs = hnt.getSubstrateInformation();
		int count = 0;
		for(Substrate s : subs) {
			if(s.getStype() == Substrate.PROCCESS_SUBSTRATE || s.getStype() == Substrate.OUTPUT_SUBSTRATE) count++;
		}
		return count;
	}


	/**
	 * TODO: Generalize to handle convolutional connections as well!
	 * 
	 * Number of links that a fully connected substrate network would possess for the
	 * given HyperNEAT task (which defines potential substrate connectivity)
	 * @param hnt HyperNEATTask
	 * @return Total possible links
	 */
	public static int totalPossibleLinks(HyperNEATTask hnt) {

		assert !CommonConstants.convolution : "The totalPossibleLinks method should not be used in conjunction with convolutional networks yet";

	// extract substrate information from domain
	List<Substrate> subs = hnt.getSubstrateInformation();
	List<SubstrateConnectivity> connections = hnt.getSubstrateConnectivity();
	// Will map substrate names to index in subs List
	HashMap<String, Integer> substrateIndexMapping = new HashMap<String, Integer>();
	for (int i = 0; i < subs.size(); i++) {
		substrateIndexMapping.put(subs.get(i).getName(), i);
	}

	int count = 0;
	for(int i = 0; i < connections.size(); i++) {
		String source = connections.get(i).sourceSubstrateName;
		String target = connections.get(i).targetSubstrateName;
		Substrate subSource = subs.get(substrateIndexMapping.get(source));
		Substrate subTarget = subs.get(substrateIndexMapping.get(target));
		count += subSource.getSize().t1 * subSource.getSize().t2 * subTarget.getSize().t1 * subTarget.getSize().t2; 
	}

	return count;
	}

	/**
	 * Generalizes the retrieval of Substrate Information
	 * 
	 * @param inputWidth Width of each Input and Processing Board
	 * @param inputHeight Height of each Input and Processing Board
	 * @param numInputSubstrates Number of Input Boards
	 * @param output List<Triple<String, Integer, Integer>> that defines the name of the substrates, followed by their sizes
	 * 
	 * @return Substrate Information
	 */	
	public static List<Substrate> getSubstrateInformation(int inputWidth, int inputHeight, int numInputSubstrates, List<Triple<String, Integer, Integer>> output){


		List<Substrate> substrateInformation = new LinkedList<Substrate>();

		// Figure out input substrates

		// Different extractors correspond to different substrate configurations
		Pair<Integer, Integer> substrateDimension = new Pair<Integer, Integer>(inputWidth, inputHeight);

		for(int i = 0; i < numInputSubstrates; i++){
			Substrate inputSub = new Substrate(substrateDimension, Substrate.INPUT_SUBSTRATE, 
					new Triple<Integer, Integer, Integer>(i, 0, 0), // i is the x-coordinate: all are at the bottom level: y = 0, z = 0 
					"Input(" + i + ")");
			substrateInformation.add(inputSub);
		}
		if(!CommonConstants.hyperNEAT){
			Substrate biasSub = new Substrate(new Pair<>(1,1), Substrate.INPUT_SUBSTRATE, 
					new Triple<Integer, Integer, Integer>(numInputSubstrates, 0, 0), // to the right of all other input substrates 
					"bias");
			substrateInformation.add(biasSub);
		}				

		// End input substrates


		int processWidth = Parameters.parameters.integerParameter("HNProcessWidth");
		int processDepth = Parameters.parameters.integerParameter("HNProcessDepth");

		List<Substrate> hiddenSubstrateInformation = Parameters.parameters.booleanParameter("useHyperNEATCustomArchitecture") ?
				getHiddenSubstrateInformation(MMNEAT.substrateArchitectureDefinition.getNetworkHiddenArchitecture()) :
					getHiddenSubstrateInformation(inputWidth, inputHeight, processWidth, processDepth);

				if(Parameters.parameters.booleanParameter("useHyperNEATCustomArchitecture")) {
					// Depth depends on custom architecture
					processDepth = MMNEAT.substrateArchitectureDefinition.getNetworkHiddenArchitecture().size();
				}


				substrateInformation.addAll(hiddenSubstrateInformation);

				// Figure out output substrates

				for(int i = 0; i < output.size(); i++){
					Substrate outputSub = new Substrate(new Pair<Integer, Integer>(output.get(i).t2, output.get(i).t3), Substrate.OUTPUT_SUBSTRATE,
							new Triple<Integer, Integer, Integer>(i, (processDepth+1), 0), // i is the x-coordinate, y = one above the top processing layer, z = 0 
							output.get(i).t1);
					substrateInformation.add(outputSub);
				}

				// End output substrates

				return substrateInformation;
	}

	/**
	 * Produces hidden/process substrate information for each substrate in each layer of a given 
	 * 	networkHiddenArchitecture specification.  
	 * @param networkHiddenArchitecture: specification for the hidden layers of a network with the size of 
	 * 	the list being the number of hidden layers and coordinates of the substrate being encoded into each 
	 *  triple like this (layer, width of substrates in this layer, height of substrates in this layer)  
	 * @return list of initialized 2 dimension Substrate objects
	 */
	private static List<Substrate> getHiddenSubstrateInformation(List<HiddenSubstrateGroup> networkHiddenArchitecture) {
		List<Substrate> substrateInformation = new LinkedList<Substrate>();
		//Coordinates of substrate in vector space, (x,y,z). First 2 zeros are placeholder values.
		Triple<Integer, Integer, Integer> substrateCoordinates = new Triple<Integer, Integer, Integer>(0,0,0);

		// Add 2D hidden/processing layer(s)
		for(int i = 0; i < networkHiddenArchitecture.size(); i++) {			
			HiddenSubstrateGroup hiddenSubstrateGroup = networkHiddenArchitecture.get(i);
			Pair<Integer, Integer> hiddenSubstrateGroupSize = hiddenSubstrateGroup.substrateSize;
			Triple<Integer, Integer, Integer> hiddenSubstrateGroupStartLocation = hiddenSubstrateGroup.hiddenSubstrateGroupStartLocation;
			substrateCoordinates.t2 = hiddenSubstrateGroupStartLocation.t2;
			for(int x = 0; x < hiddenSubstrateGroup.numSubstrates; x++) {
				substrateCoordinates.t1 = hiddenSubstrateGroupStartLocation.t1 + x;
				Substrate processSubstrate = new Substrate(hiddenSubstrateGroupSize, Substrate.PROCCESS_SUBSTRATE, substrateCoordinates,
						"process(" + substrateCoordinates.t1 + "," + substrateCoordinates.t2 + ")", 
						CommonConstants.convolution ? ActivationFunctions.FTYPE_RE_LU : Substrate.DEFAULT_ACTIVATION_FUNCTION);
				substrateInformation.add(processSubstrate);
			}
		}
		return substrateInformation;
	}

	/**
	 * Generalizes the retrieval of Substrate Information for the Hidden layers only
	 * 
	 * @param inputWidth Width of each Input and Processing Board
	 * @param inputHeight Height of each Input and Processing Board
	 * @param processWidth Number of Processing Boards per Processing Layer
	 * @param processDepth Number of Processing Layers
	 * 
	 * @return Substrate Information
	 */
	public static List<Substrate> getHiddenSubstrateInformation(int inputWidth, int inputHeight, int processWidth, int processDepth) {		
		List<Substrate> substrateInformation = new LinkedList<Substrate>();

		// Convolutional network layer sizes depend on the size of the preceding layer,
		// along with the receptive field size, unless zero-padding is used
		boolean zeroPadding = Parameters.parameters.booleanParameter("zeroPadding");
		int receptiveFieldHeight = Parameters.parameters.integerParameter("receptiveFieldHeight");
		assert receptiveFieldHeight % 2 == 1 : "Receptive field size needs to be odd to be centered: " + receptiveFieldHeight;
		int yEdgeOffset = zeroPadding ? 0 : receptiveFieldHeight / 2;
		int receptiveFieldWidth = Parameters.parameters.integerParameter("receptiveFieldWidth");
		assert receptiveFieldWidth % 2 == 1 : "Receptive field size needs to be odd to be centered: " + receptiveFieldWidth;
		int xEdgeOffset = zeroPadding ? 0 : receptiveFieldWidth / 2;

		// Different extractors correspond to different substrate configurations
		Pair<Integer, Integer> substrateDimension = new Pair<Integer, Integer>(inputWidth, inputHeight);

		for(int i = 0; i < processDepth; i++) { // Add 2D hidden/processing layer(s)
			if(CommonConstants.convolution) {
				// Subsequent convolutional layers sometimes need to be smaller than preceding ones
				substrateDimension = new Pair<Integer, Integer>(substrateDimension.t1 - 2*xEdgeOffset, substrateDimension.t2 - 2*yEdgeOffset);
			}
			for(int k = 0; k < processWidth; k++) {
				// x coord = k, y = 1 + i because the height is the depth plus 1 (for the input layer)
				Triple<Integer, Integer, Integer> processSubCoord = new Triple<Integer, Integer, Integer>(k, 1 + i, 0);
				Substrate processSub = new Substrate(substrateDimension, Substrate.PROCCESS_SUBSTRATE, processSubCoord,
						"process(" + k + "," + i + ")", 
						CommonConstants.convolution ? ActivationFunctions.FTYPE_RE_LU : Substrate.DEFAULT_ACTIVATION_FUNCTION);
				substrateInformation.add(processSub);
			}
		}

		return substrateInformation;
	}

	/**
	 * Generalizes the creation of HyperNEAT Substrates
	 * 
	 * (Output layers are domain-specific)
	 * 
	 * @param numInputSubstrates Number of individual Input Boards being processed
	 * @param outputNames Names of the Output Layers (Domain-Specific)
	 * 
	 * @return Substrate connectivity
	 */
	public static List<SubstrateConnectivity> getSubstrateConnectivity(int numInputSubstrates, List<String> outputNames) {
		if(Parameters.parameters.booleanParameter("useHyperNEATCustomArchitecture")) {
			// TODO: HyperNEATSeed tasks currently do not support custom architectures
			return MMNEAT.substrateArchitectureDefinition.getSubstrateConnectivity((HyperNEATTask) MMNEAT.task);
		} else {
			int processWidth = Parameters.parameters.integerParameter("HNProcessWidth");
			int processDepth = Parameters.parameters.integerParameter("HNProcessDepth");
			return getSubstrateConnectivity(numInputSubstrates, processWidth, processDepth, outputNames, Parameters.parameters.booleanParameter("extraHNLinks"));
		}
	}

	/**
	 * Generalizes the creation of HyperNEAT Substrates
	 * 
	 * (Output layers are domain-specific)
	 * 
	 * @param numInputSubstrates Number of individual Input Boards being processed
	 * @param outputNames Names of the Output Layers (Domain-Specific)
	 * @param connectInputsToOutputs Should the Input Layer be directly connected to the Output Layer?
	 * 
	 * @return Substrate connectivity
	 */
	public static List<SubstrateConnectivity> getSubstrateConnectivity(int numInputSubstrates, List<String> outputNames, boolean connectInputsToOutputs){
		int processWidth = Parameters.parameters.integerParameter("HNProcessWidth");
		int processDepth = Parameters.parameters.integerParameter("HNProcessDepth");
		return getSubstrateConnectivity(numInputSubstrates, processWidth, processDepth, outputNames, connectInputsToOutputs);
	}

	/**
	 * Generalizes the creation of HyperNEAT Substrates
	 * 
	 * (Output layers are domain-specific)
	 * 
	 * @param numInputSubstrates Number of individual Input Boards being processed
	 * @param processWidth Number of Processing Boards per Processing Layer
	 * @param processDepth Number of Processing Layers
	 * @param outputNames Names of the Output Layers (Domain-Specific)
	 * 
	 * @return Substrate connectivity
	 */
	public static List<SubstrateConnectivity> getSubstrateConnectivity(int numInputSubstrates, int processWidth, int processDepth, List<String> outputNames){
		return getSubstrateConnectivity(numInputSubstrates, processWidth, processDepth, outputNames, Parameters.parameters.booleanParameter("extraHNLinks"));
	}

	/**
	 * Generalizes the creation of HyperNEAT Substrates
	 * 
	 * (Output layers are domain-specific)
	 * 
	 * @param numInputSubstrates Number of individual Input Boards being processed
	 * @param processWidth Number of Processing Boards per Processing Layer
	 * @param processDepth Number of Processing Layers
	 * @param outputNames Names of the Output Layers (Domain-Specific)
	 * @param connectInputsToOutputs Should the Input Layer be directly connected to the Output Layer?
	 * 
	 * @return Substrate connectivity
	 */
	public static List<SubstrateConnectivity> getSubstrateConnectivity(int numInputSubstrates, int processWidth, int processDepth, List<String> outputNames, boolean connectInputsToOutputs){
		List<SubstrateConnectivity> substrateConnectivity = new LinkedList<SubstrateConnectivity>();
		// Different extractors correspond to different substrate configurations
		if(processDepth > 0) {
			for(int k = 0; k < processWidth; k++) {
				// Link the input layer to the processing layer: allows convolution
				for(int i = 0; i < numInputSubstrates; i++){
					substrateConnectivity.add(new SubstrateConnectivity("Input(" + i + ")", "process(" + k + ",0)", SubstrateConnectivity.CTYPE_CONVOLUTION));
				}

				if(!CommonConstants.hyperNEAT){
					// connect bias to the bottom layer of processing substrates: no convolution
					substrateConnectivity.add(new SubstrateConnectivity("bias", "process(" + k + ",0)", SubstrateConnectivity.CTYPE_FULL));
				}
			}
		}

		// hidden layer connectivity is the same, regardless of input configuration
		for(int i = 0; i < (processDepth - 1); i++) {
			for(int k = 0; k < processWidth; k++) {
				for(int q = 0; q < processWidth; q++) {
					// Each processing substrate at one depth connected to processing subsrates at next depth
					substrateConnectivity.add(new SubstrateConnectivity("process("+k+","+i+")", "process("+q+","+(i + 1)+")", SubstrateConnectivity.CTYPE_CONVOLUTION));
				}

				if(!CommonConstants.hyperNEAT){
					// connect bias to each remaining processing substrate
					substrateConnectivity.add(new SubstrateConnectivity("bias", "process("+k+","+(i + 1)+")", SubstrateConnectivity.CTYPE_FULL));
				}

			}
		}

		if(processDepth > 0) {
			for(int k = 0; k < processWidth; k++) {
				// Link the final processing layer to the output layer
				for(String name : outputNames){
					substrateConnectivity.add(new SubstrateConnectivity("process(" + k + "," + (processDepth-1) + ")", name, SubstrateConnectivity.CTYPE_FULL));
				}
			}
		}

		if(connectInputsToOutputs) { // Connect each input substrate directly to the output neuron
			// Link the input layer to the output layer
			for(int i = 0; i < numInputSubstrates; i++){
				for(String name : outputNames) {
					substrateConnectivity.add(new SubstrateConnectivity("Input(" + i + ")", name, SubstrateConnectivity.CTYPE_FULL));
				}
			}

			// For HyperNEAT seeded tasks
			if(!CommonConstants.hyperNEAT){
				for(String name : outputNames){
					// Each output substrate has a bias connection
					substrateConnectivity.add(new SubstrateConnectivity("bias", name, SubstrateConnectivity.CTYPE_FULL));
				}
			}
		}

		return substrateConnectivity;
	}

	/**
	 * Number of outputs that CPPNs are supposed to have.
	 * May not work with coevolution in the case where different populations used different
	 * substrate configurations.
	 * @return
	 */
	public static int numCPPNOutputs() {
		HyperNEATTask hnt = HyperNEATUtil.getHyperNEATTask();
		if(CommonConstants.substrateLocationInputs) {
			// All substrate pairings use the same outputs
			return HyperNEATCPPNGenotype.numCPPNOutputsPerLayerPair + HyperNEATUtil.numBiasOutputsNeeded();
		} else if (Parameters.parameters.booleanParameter("useCoordConv")) {
			assert CommonConstants.convolution;
			//This is a  coordConv specific identification of substrate connections. In this scheme two connections that have the same source substrate size and layer, target name, and receptive
			//field size are equivalent. Each convolutional connection in the network that is unique according to this scheme will account for the addition of 2 connections in the
			//substrateConnectivies list, one for the iCoordConvSubstrate and one for the jCoordConvSubstrate.
			//This hashset will keep track of whether or not a connection is unique. If a connection id is in the hashset it has been accounted for.
			//Each identification is the hash of quint (source substrate size, source substrate layer, target substrate name, receptive field height, receptive field width)
			HashSet<Integer> accountedForCoordConvIdentifications = new HashSet<Integer>();
			List<SubstrateConnectivity> substrateConnectivities = hnt.getSubstrateConnectivity();
			List<Substrate> substrates = hnt.getSubstrateInformation();
			int numCoordConvConnections = 0;
			for(SubstrateConnectivity substrateConnectivity: substrateConnectivities) {
				if (substrateConnectivity.connectivityType == SubstrateConnectivity.CTYPE_CONVOLUTION) {
					Pair<Pair<Integer, Integer>, Integer> sourceSubstrateSizeAndLayer = getSubstrateSizeAndLayerFromName(substrates, substrateConnectivity.sourceSubstrateName);
					int coordConvIdentificationHash = new Quint<Pair<Integer, Integer>, Integer, String, Integer, Integer>(
									sourceSubstrateSizeAndLayer.t1, sourceSubstrateSizeAndLayer.t2, substrateConnectivity.targetSubstrateName, 
									substrateConnectivity.receptiveFieldHeight, substrateConnectivity.receptiveFieldWidth).hashCode();
					if(!accountedForCoordConvIdentifications.contains(coordConvIdentificationHash)) {
						accountedForCoordConvIdentifications.add(coordConvIdentificationHash);
						numCoordConvConnections += 2; //connections for both the i and j substrates
					}
				}
			}
			int numSubstratePairings = hnt.getSubstrateConnectivity().size();
			return numSubstratePairings * HyperNEATCPPNGenotype.numCPPNOutputsPerLayerPair + HyperNEATUtil.numBiasOutputsNeeded() + numCoordConvConnections;
		} else {
			// Each substrate pairing has a separate set of outputs
			int numSubstratePairings = hnt.getSubstrateConnectivity().size(); // TODO: Getting this information from the task could be a problem if we ever evolve the architectures too.
			return numSubstratePairings * HyperNEATCPPNGenotype.numCPPNOutputsPerLayerPair + HyperNEATUtil.numBiasOutputsNeeded();
		}
	}

	/**
	 * Number of CPPN inputs that there will actually be, which may be different from what
	 * the task wants (extra could be added)
	 * @return
	 */
	public static int numCPPNInputs() {
		return numCPPNInputs(HyperNEATUtil.getHyperNEATTask());
	}

	public static int numCPPNInputs(HyperNEATTask task) {
		assert task.numCPPNInputs() > 0 : "The number of CPPN in puts must be positive";
		// Adds four inputs if doing weight sharing or using substrate location inputs (substrate location inputs must be used with weight sharing):
		// Specifically, source and target locations of substrate (height and position within layer)
		return task.numCPPNInputs() + (CommonConstants.substrateLocationInputs || Parameters.parameters.booleanParameter("convolutionWeightSharing") ? 4 : 0);
	}

	/**
	 * Assumes that all input substrates have the same width and height, and returns
	 * the shape of the input volume. Meant to be used by convolutional networks in DL4J.
	 * @param substrates List of substrates for a task
	 * @return array of {width, height, channels} of the input substrates
	 */
	public static int[] getInputShape(List<Substrate> substrates) {
		assert substrates.get(0).getStype() == Substrate.INPUT_SUBSTRATE : "First substrate was not an input substrate: " + substrates;
		// First substrate must be an input sustrate
		Pair<Integer,Integer> size = substrates.get(0).getSize();
		int depth = 1;
		while(substrates.get(depth).getStype() == Substrate.INPUT_SUBSTRATE) {
			assert substrates.get(depth).getSize().equals(size) : "Input substrates had different dimensions: " + size + " not equal to " + substrates.get(depth).getSize();
			depth++;
		}
		// The 1 at the front is the mini-batch size, which is a single frame for RL domains (change? generalize?)
		return new int[] {1, depth, size.t2, size.t1};
	}

	/**
	 * Count the number of outputs across all output substrates
	 * @param substrates Substrate list
	 * @return Output count
	 */
	public static int getOutputCount(List<Substrate> substrates) {
		int outputCount = 0;
		for(int i = 0; i < substrates.size(); i++) {
			if(substrates.get(i).getStype() == Substrate.OUTPUT_SUBSTRATE) {
				Pair<Integer,Integer> size = substrates.get(i).getSize();
				outputCount += size.t1 * size.t2;
			}
		}
		return outputCount;
	}

	/**
	 * Adds the x/y coordinates as inputs into each hyperNEAT substrate 
	 * @param substrates the list of substrates that will have coordConv substrates added to it
	 * @param connections the list of connections that will have the connections to new coordConv substrates added to it
	 * @param numInputSubstrates the number of input substrates that are defined for this task
	 */
	public static void addCoordConvSubstrateAndConnections(List<Substrate> substrates, List<SubstrateConnectivity> connections, int numInputSubstrates) {
		//this implementation with naive coordConvNewSubLocation will cause problems with global coordinates.
		assert(!CommonConstants.substrateLocationInputs);
		//hash map of (size of sub, layer of sub) objects to (iCoordConvName, jCoordConvName) strings. 
		//Contains a size/layer combo if it has been added to the substrates list. If this combo has been added to the substrate list then this hashmap allows retrieval of its coord conv names
		HashMap<Pair<Pair<Integer, Integer>, Integer>, Pair<String, String>> coordConvSubSizeAndLayerToIAndJNames = new HashMap<Pair<Pair<Integer, Integer>, Integer>, Pair<String, String>>();
		int numCoordConvSubstratesAdded = 0; //this is for determining the location in vector space of each coord conv
		int coordConvSubstrateNameIndex = 0; //increments after the addition of the i and j coord convs
		int connectionsSize = connections.size(); //required to be separate from the for loop because we are adding to connections within the loop
		for(int i = 0; i < connectionsSize; i++) {
			SubstrateConnectivity substrateConnectivity = connections.get(i);
			if(substrateConnectivity.connectivityType == SubstrateConnectivity.CTYPE_CONVOLUTION) {
				Pair<Pair<Integer, Integer>, Integer> sourceSubstrateSizeAndLayer = getSubstrateSizeAndLayerFromName(substrates, substrateConnectivity.sourceSubstrateName); //new Pair<Pair<Integer, Integer>, Integer>(sourceSubstrateSize, substrateLayer);
				//this is also equivalent to the location in the substrate list that the new coordConvSubstrate will be added
				int iCoordConvXCoordinate = numInputSubstrates + numCoordConvSubstratesAdded;
				int jCoordConvXCoordinate = numInputSubstrates + numCoordConvSubstratesAdded + 1;
				String iCoordConvName;
				String jCoordConvName;
				if (coordConvSubSizeAndLayerToIAndJNames.containsKey(sourceSubstrateSizeAndLayer)) {
					Pair<String, String> CoordConvSourceNamesToConnect = coordConvSubSizeAndLayerToIAndJNames.get(sourceSubstrateSizeAndLayer);
					iCoordConvName = CoordConvSourceNamesToConnect.t1;
					jCoordConvName = CoordConvSourceNamesToConnect.t2;
				} else {
					iCoordConvName = "iCoordConv(" + coordConvSubstrateNameIndex + ",0)";
					jCoordConvName = "jCoordConv(" + coordConvSubstrateNameIndex + ",0)";
					Triple<Integer, Integer, Integer> iCoordConvNewSubLocation = new Triple<Integer, Integer, Integer>(iCoordConvXCoordinate, 0, 0);
					Triple<Integer, Integer, Integer> jCoordConvNewSubLocation = new Triple<Integer, Integer, Integer>(jCoordConvXCoordinate, 0, 0);
					Substrate iCoordConvSubstrate = new Substrate(sourceSubstrateSizeAndLayer.t1, Substrate.ICOORDCONV_SUBSTRATE, iCoordConvNewSubLocation,
							iCoordConvName, ActivationFunctions.FTYPE_ID);
					Substrate jCoordConvSubstrate = new Substrate(sourceSubstrateSizeAndLayer.t1, Substrate.JCOORDCONV_SUBSTRATE, jCoordConvNewSubLocation,
							jCoordConvName, ActivationFunctions.FTYPE_ID);
					substrates.add(iCoordConvXCoordinate, iCoordConvSubstrate);
					substrates.add(jCoordConvXCoordinate, jCoordConvSubstrate);
					coordConvSubSizeAndLayerToIAndJNames.put(sourceSubstrateSizeAndLayer, new Pair<String, String>(iCoordConvName, jCoordConvName));
					numCoordConvSubstratesAdded += 2;
					coordConvSubstrateNameIndex++;
				}
				boolean previouslyAdded = false;
				for(SubstrateConnectivity connection: connections) {
					if(connection.sourceSubstrateName.equals(iCoordConvName) && connection.targetSubstrateName.equals(substrateConnectivity.targetSubstrateName) 
							&& connection.receptiveFieldWidth == substrateConnectivity.receptiveFieldWidth && connection.receptiveFieldHeight == substrateConnectivity.receptiveFieldHeight) {
						previouslyAdded = true;
						break;
					}
				}
				if (!previouslyAdded) {
					connections.add(new SubstrateConnectivity(iCoordConvName, substrateConnectivity.targetSubstrateName, substrateConnectivity.receptiveFieldWidth, substrateConnectivity.receptiveFieldHeight));
					connections.add(new SubstrateConnectivity(jCoordConvName, substrateConnectivity.targetSubstrateName, substrateConnectivity.receptiveFieldWidth, substrateConnectivity.receptiveFieldHeight));
				}
			}
		}
	}

	/**
	 * For coord conv
	 * @param substrates list of substrates
	 * @param name substrate name
	 * @return the size and layer of the substrate that corresponds to this name
	 */
	public static Pair<Pair<Integer, Integer>, Integer> getSubstrateSizeAndLayerFromName(List<Substrate> substrates, String name) {
		Substrate sourceSubstrate = null;
		for(Substrate substrate: substrates) {
			if(substrate.getName().equals(name)) {
				sourceSubstrate = substrate;
				break;
			}
		}
		assert sourceSubstrate != null;
		int substrateLayer = sourceSubstrate.getSubLocation().t2; //layer and y location are equivalent
		return new Pair<Pair<Integer, Integer>, Integer>(sourceSubstrate.getSize(), substrateLayer);
	}
}
