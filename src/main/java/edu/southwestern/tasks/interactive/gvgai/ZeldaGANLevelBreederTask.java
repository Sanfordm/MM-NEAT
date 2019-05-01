package edu.southwestern.tasks.interactive.gvgai;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.southwestern.MMNEAT.MMNEAT;
import edu.southwestern.parameters.Parameters;
import edu.southwestern.tasks.gvgai.GVGAIUtil;
import edu.southwestern.tasks.gvgai.GVGAIUtil.GameBundle;
import edu.southwestern.tasks.gvgai.zelda.ZeldaGANUtil;
import edu.southwestern.tasks.interactive.InteractiveGANLevelEvolutionTask;
import edu.southwestern.tasks.mario.gan.GANProcess;
import edu.southwestern.util.datastructures.ArrayUtil;
import edu.southwestern.util.datastructures.Pair;
import gvgai.core.game.BasicGame;
import gvgai.core.game.Game;
import gvgai.core.vgdl.VGDLFactory;
import gvgai.core.vgdl.VGDLParser;
import gvgai.core.vgdl.VGDLRegistry;
import gvgai.tracks.singlePlayer.tools.human.Agent;

/**
 * Evolve Zelda rooms using a GAN
 * 
 * @author Jacob Schrum
 */
public class ZeldaGANLevelBreederTask extends InteractiveGANLevelEvolutionTask {

	private static final String GAME_FILE = "zelda";
	private static final String FULL_GAME_FILE = LevelBreederTask.GAMES_PATH + GAME_FILE + ".txt";
	private static final int ZELDA_HEIGHT = 256;//Parameters.parameters.integerParameter("zeldaImageHeight");
	private static final int ZELDA_WIDTH = 176;//Parameters.parameters.integerParameter("zeldaImageWidth");

	public ZeldaGANLevelBreederTask() throws IllegalAccessException {
		super();
		VGDLFactory.GetInstance().init();
		VGDLRegistry.GetInstance().init();
	}

	@Override
	protected String getWindowTitle() {
		return "ZeldaGAN Level Breeder";
	}

	/**
	 * Take the latent vector and use the ZeldaGAN to create a level,
	 * and then a GameBundle used for playing the game.
	 * @param phenotype Latent vector
	 * @return GameBundle for playing GVG-AI game
	 */
	public GameBundle setUpGameWithLevelFromLatentVector(ArrayList<Double> phenotype) {
		double[] latentVector = ArrayUtil.doubleArrayFromList(phenotype);
		String[] level = ZeldaGANUtil.generateGVGAILevelFromGAN(latentVector, new Point(8,8));
		int seed = 0; // TODO: Use parameter?
		Agent agent = new Agent();
		agent.setup(null, seed, true); // null = no log, true = human 
		Game game = new VGDLParser().parseGame(FULL_GAME_FILE); // Initialize the game	

		return new GameBundle(game, level, agent, seed, 0);
	}

	@Override
	protected BufferedImage getButtonImage(ArrayList<Double> phenotype, int width, int height, double[] inputMultipliers) {
		GameBundle bundle = setUpGameWithLevelFromLatentVector(phenotype);
		BufferedImage levelImage = GVGAIUtil.getLevelImage(((BasicGame) bundle.game), bundle.level, (Agent) bundle.agent, width, height, bundle.randomSeed);
		return levelImage;
	}

	@Override
	public void configureGAN() {
		GANProcess.type = GANProcess.GAN_TYPE.ZELDA;
	}

	@Override
	public String getGANModelParameterName() {
		return "zeldaGANModel";
	}

	@Override
	public Pair<Integer, Integer> resetAndReLaunchGAN(String model) {
		int oldLength = GANProcess.latentVectorLength(); // for old model
		// Need to parse the model name to find out the latent vector size
		String dropDataSource = model.substring(model.indexOf("_")+1);
		String dropEpochs = dropDataSource.substring(dropDataSource.indexOf("_")+1);
		String latentSize = dropEpochs.substring(0,dropEpochs.indexOf("."));
		int size = Integer.parseInt(latentSize);
		Parameters.parameters.setInteger("GANInputSize", size);

		GANProcess.terminateGANProcess();
		// Because Python process was terminated, latentVectorLength will reinitialize with the new params
		int newLength = GANProcess.latentVectorLength(); // new model
		return new Pair<>(oldLength, newLength);
	}

	@Override
	public String getGANModelDirectory() {
		return "src"+File.separator+"main"+File.separator+"python"+File.separator+"GAN"+File.separator+"ZeldaGAN";
	}

	@Override
	public void playLevel(ArrayList<Double> phenotype) {
		GameBundle bundle = setUpGameWithLevelFromLatentVector(phenotype);
		// Must launch game in own thread, or won't animate or listen for events
		new Thread() {
			public void run() {
				// True is to watch the game being played
				GVGAIUtil.runOneGame(bundle, true);
			}
		}.start();
	}
	
	/*
	 * TODO: Documentation
	 */
	@Override
	public void makeDungeon(ArrayList<ArrayList<Double>> phenotypes, int numRooms, boolean rotate) {
		
		Node[][] table = new Node[numRooms][numRooms]; //map of rooms
		Random r = new Random();
		System.out.println("Start make dungeon");
		//make panel
		System.out.println("Spawn Frame");
		JFrame dungeon = new JFrame("ZeldaGAN Full Dungeon"); 
		JPanel roomGrid = new JPanel();
		roomGrid.setLayout(new GridLayout(numRooms, numRooms)); //row, col
		
		//populate grid
		System.out.println("populating grid with rooms");
		ArrayList<Node> placed = new ArrayList(); //list of all rooms placed on map
		Node anchor = new Node(numRooms/2, numRooms/2, null, phenotypes.get(0));
		table[numRooms/2][numRooms/2] =  anchor; //place initial room on map
		placed.add(anchor);
		for(int i = 1; i < phenotypes.size(); i++) { //populate table
			System.out.println("populating pheno " +  i + " into grid");
			anchor = placed.get(r.nextInt(placed.size())); //get random anchor point from placed rooms
			if(anchor.getNumNeighbors() < 4) { // if has room for neighbors
				int x = anchor.X, y = anchor.Y;
				boolean loop = true;
				while(loop) { // find neighbor position not occupied
					switch(r.nextInt(4)) { //select random position for neighbor
					case 0: //up
						if(y + 1 < numRooms && !anchor.hasNeighborAtPos(x, y + 1)) {
							Node n = new Node(x, y + 1, anchor, phenotypes.get(i));
							placed.add(n);
							table[x][y + 1] = n;
							loop = false;
							break;
						}
					case 1: //left
						if(x - 1 < numRooms && !anchor.hasNeighborAtPos(x - 1, y)) {
							Node n = new Node(x - 1, y, anchor, phenotypes.get(i));
							placed.add(n);
							table[x - 1][y] = n;
							loop = false;
							break;
						}
					case 2: //right
						if(x + 1 < numRooms && !anchor.hasNeighborAtPos(x + 1, y)) {
							Node n = new Node(x + 1, y, anchor, phenotypes.get(i));
							placed.add(n);
							table[x + 1][y] = n;
							loop = false;
							break;
						}
					case 3: //down
						if(y - 1 < numRooms && !anchor.hasNeighborAtPos(x, y - 1)) {
							Node n = new Node(x, y - 1, anchor, phenotypes.get(i));
							placed.add(n);
							table[x][y - 1] = n;
							loop = false;
							break;
						}
					}
				}
			} else { //placed selection is fully surrounded, remove from placed
				placed.remove(placed.indexOf(anchor));
				i--;
			}
			
		}
		System.out.println("actually making the dungeon?");
		for(int i = 0; i < numRooms; i++) {
			for(int j = 0; j < numRooms; j++) {
				if(table[i][j] != null) {
					Node n = table[i][j];
					BufferedImage level = getButtonImage(false, n.data, ZELDA_WIDTH, ZELDA_HEIGHT, inputMultipliers); //creates image rep. of level)
					ImageIcon img = new ImageIcon(level.getScaledInstance(ZELDA_WIDTH, ZELDA_HEIGHT, Image.SCALE_DEFAULT)); //creates image of level
					JLabel imageLabel = new JLabel(img); // places level on label
					roomGrid.add(imageLabel); //add label to panel
				}
				else {
					//make it black
					JLabel blankText = new JLabel("Ignore pls");
					blankText.setForeground(Color.WHITE);
					JPanel blankBack = new JPanel();
					blankBack.setBackground(Color.BLACK);
					blankBack.add(blankText);
					roomGrid.add(blankBack);
				}
				
			}
		}

		dungeon.setSize(new Dimension(1500,750));
		dungeon.add(roomGrid);
		dungeon.pack();
		dungeon.setVisible(true);
	};
	
	/*
	 * Private class for dungeon tracking. Provides parameter data to track
	 * the location in a dungeon grid, its neighbor rooms, and phenotype
	 * @param int x
	 * @param int y
	 * @param Node origin
	 * @param ArrayList<Double> phenotype
	 */
	private class Node {
		private Node neighbor1;
		private Node neighbor2;
		private Node neighbor3;
		private Node neighbor4;
		private ArrayList<Double> data;
		private int numNeighbors = 1;
		protected int X;
		protected int Y;
		
		public Node(int x, int y, Node origin, ArrayList<Double> d) {
			this.X = x;
			this.Y = y;
			this.setNeighbor(origin);
			this.data = d;
		}
		public boolean hasNeighborAtPos(int x, int y) {
			for(int i = 1; i <= 4; i++)
				if(this.getNeighbor(i) != null && this.getNeighbor(i).X == x && this.getNeighbor(i).Y == y)
					return true;
			return false;
		}
		public boolean setNeighbor(Node n) {
			if(neighbor1 == null) {
				neighbor1 = n;
				numNeighbors++;
				return true;
			}
			else if(neighbor2 == null) {
				neighbor2 = n;
				numNeighbors++;
				return true;
			}
			else if(neighbor3 == null) {
				neighbor3 = n;
				numNeighbors++;
				return true;
			}
			else
				return false;
		}
		public int getNumNeighbors() {
			return this.numNeighbors;
		}
		public Node getNeighbor(int i) {
			if(i > 4)
				throw new IllegalArgumentException("Value must be between 1 and 4 inclusive\n");
			switch(i) {
			case 1:
				return this.neighbor1;
			case 2:
				return this.neighbor2;
			case 3:
				return this.neighbor3;
			case 4:
				return this.neighbor4; 
			default:
				return null;
			}
		}
	}

	public static void main(String[] args) {
		try {
			MMNEAT.main(new String[]{"runNumber:0","randomSeed:1","trials:1","mu:16","zeldaGANModel:ZeldaDungeonsAll_5000_10.pth","maxGens:500","io:false","netio:false","GANInputSize:10","mating:true","fs:false","task:edu.southwestern.tasks.interactive.gvgai.ZeldaGANLevelBreederTask","genotype:edu.southwestern.evolution.genotypes.BoundedRealValuedGenotype","watch:false","cleanFrequency:-1","simplifiedInteractiveInterface:false","saveAllChampions:true","cleanOldNetworks:false","ea:edu.southwestern.evolution.selectiveBreeding.SelectiveBreedingEA","imageWidth:2000","imageHeight:2000","imageSize:200"});
		} catch (FileNotFoundException | NoSuchMethodException e) {
			e.printStackTrace();
		}
	}


}
