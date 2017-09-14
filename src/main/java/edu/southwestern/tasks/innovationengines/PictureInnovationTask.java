package edu.southwestern.tasks.innovationengines;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import org.deeplearning4j.zoo.util.imagenet.ImageNetLabels;
import org.nd4j.linalg.api.ndarray.INDArray;

import edu.southwestern.MMNEAT.MMNEAT;
import edu.southwestern.evolution.genotypes.Genotype;
import edu.southwestern.evolution.mapelites.Archive;
import edu.southwestern.evolution.mapelites.MAPElites;
import edu.southwestern.networks.Network;
import edu.southwestern.parameters.CommonConstants;
import edu.southwestern.scores.Score;
import edu.southwestern.tasks.LonerTask;
import edu.southwestern.tasks.interactive.picbreeder.PicbreederTask;
import edu.southwestern.util.MiscUtil;
import edu.southwestern.util.datastructures.ArrayUtil;
import edu.southwestern.util.graphics.DrawingPanel;
import edu.southwestern.util.graphics.GraphicsUtil;
import edu.southwestern.util.graphics.ImageNetClassification;

public class PictureInnovationTask<T extends Network> extends LonerTask<T> {
	// Not sure if this is necessary. Does the pre-processing do more than just resize the image?
	// Because the image is already the correct size. However, I read something about additional
	// processing steps somewhere in a DL4J example.
	private static final boolean PREPROCESS = true;
	
	@Override
	public int numObjectives() {
		return 1; // Score for the best label
	}

	@Override
	public double getTimeStamp() {
		return 0; // Not used
	}

	@Override
	public Score<T> evaluate(Genotype<T> individual) {
		Network cppn = individual.getPhenotype();
		BufferedImage image = GraphicsUtil.imageFromCPPN(cppn, ImageNetClassification.IMAGE_NET_INPUT_WIDTH, ImageNetClassification.IMAGE_NET_INPUT_HEIGHT);
		INDArray imageArray = ImageNetClassification.bufferedImageToINDArray(image);
		INDArray scores = ImageNetClassification.getImageNetPredictions(imageArray, PREPROCESS);
		double bestScore = ImageNetClassification.bestScore(scores);
		ArrayList<Double> behaviorVector = ArrayUtil.doubleVectorFromINDArray(scores);
		Score<T> result = new Score<>(individual, new double[]{bestScore}, behaviorVector);
		DrawingPanel picture = null;
		if(CommonConstants.watch || CommonConstants.netio) {
			picture = GraphicsUtil.drawImage(image, "Image", ImageNetClassification.IMAGE_NET_INPUT_WIDTH, ImageNetClassification.IMAGE_NET_INPUT_HEIGHT);
		}
		if(CommonConstants.watch) {
			// Prints top 4 labels
			String decodedLabels = new ImageNetLabels().decodePredictions(scores);
			System.out.println(decodedLabels);
			// Wait for user
			MiscUtil.waitForReadStringAndEnterKeyPress();
		}
		if(CommonConstants.netio) {
			// Lot of duplication of computation from Archive. Can that be fixed?
			@SuppressWarnings("unchecked")
			Archive<T> archive = ((MAPElites<T>) MMNEAT.ea).getArchive();
			// If saving networks, then also save pictures
			String fileName = "picture" + individual.getId() + ".bmp";
			String binLabel = archive.getBinMapping().binForScore(result);
			String binPath = archive.getArchiveDirectory() + File.separator + binLabel;
			new File(binPath).mkdirs(); // make directory if needed
			picture.save(binPath + File.separator + fileName);
		}
		if(picture != null) picture.dispose();
		return result;
	}

	public int numCPPNInputs() {
		return PicbreederTask.CPPN_NUM_INPUTS;
	}

	public int numCPPNOutputs() {
		return PicbreederTask.CPPN_NUM_OUTPUTS;
	}

}