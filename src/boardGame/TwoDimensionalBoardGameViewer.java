package boardGame;

import java.awt.*;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import boardGame.checkers.Checkers;
import boardGame.othello.Othello;
import boardGame.ttt.TicTacToe;
import edu.utexas.cs.nn.networks.TWEANN;
import edu.utexas.cs.nn.util.graphics.DrawingPanel;

/**
 * Viewer for TwoDimensionalBoardGames
 * 
 * @author johnso17
 */
public class TwoDimensionalBoardGameViewer<S  extends TwoDimensionalBoardGameState,T extends TwoDimensionalBoardGame<S>> {

	public static TwoDimensionalBoardGameViewer current = null;
	private static final long serialVersionUID = 1L;
	
	private T board;
	private S start;
	public DrawingPanel panel;
	
	public static final int HEIGHT = 600;
	public static final int WIDTH = 600;
	
	public static final int GRID_WIDTH = 60;
		
	public TwoDimensionalBoardGameViewer(T bGame){
		board = bGame;
		start = board.getStartingState();
		
		int boardWidth = start.getBoardWidth();
		int boardHeight = start.getBoardHeight();
		
		panel = new DrawingPanel((boardWidth*GRID_WIDTH), (boardHeight*GRID_WIDTH), board.getName());
		
		panel.setLocation(TWEANN.NETWORK_VIEW_DIM, 0);
		reset(start);
		current = this;		
	}
	
	/**
	 * Resets the graphics for the view
	 */
	public void reset(TwoDimensionalBoardGameState newBoard) {
		Graphics2D g = panel.getGraphics();
		g.setColor(Color.black); // Set to Black to distinguish the Background from the Board
		g.fillRect(0, 0, WIDTH, HEIGHT);
		renderBoard(g, newBoard);
	}
	
	public void renderBoard(Graphics2D g, TwoDimensionalBoardGameState newBoard){
		int boardWidth = newBoard.getBoardWidth();
		int boardHeight = newBoard.getBoardHeight();
		
		Color[] colors = newBoard.getPlayerColors();
				
		int[] pieces = new int[newBoard.getDescriptor().length];
		int index = 0;
		for(double d : newBoard.getDescriptor()){
			pieces[index++] = (int) d;
		}
		
		for(int i = 0; i < boardWidth; i++){ // Cycles through the Board and Draws the Grid
			for(int j = 0; j < boardHeight; j++){				
				
				if((i + j) % 2 == 0){ // If Even
					g.setColor(Color.lightGray);
				}else{
					g.setColor(Color.gray);
				}
				
				g.fillRect((i*GRID_WIDTH), (j*GRID_WIDTH), GRID_WIDTH, GRID_WIDTH); // Fills the Grid Square with the appropriate background Color
				
				if(pieces[i+(j*boardWidth)] != TwoDimensionalBoardGameState.EMPTY){
					g.setColor(colors[pieces[i+(j*boardWidth)]]);
					g.fillOval((i*GRID_WIDTH), (j*GRID_WIDTH), GRID_WIDTH, GRID_WIDTH);
				}
			
			}
		}
		
	}
	
	
	public static void main(String[] args) {
		new TwoDimensionalBoardGameViewer(new TicTacToe());
	}
	
}