package boardGame;

import java.util.List;

import edu.utexas.cs.nn.util.random.RandomNumbers;

public class BoardGamePlayerRandom<T extends BoardGameState> implements BoardGamePlayer<T> {
	
	/**
	 * Chooses a random action out of the list of possible actions at a given BoardGameState
	 * 
	 * @param current BoardGameState to be updated
	 * @return BoardGameState updated with the randomly selected move
	 */
	@Override
	public T takeAction(T current) {
		List<T> poss = current.possibleBoardGameStates(current);
		System.out.println("Size: " + poss.size());
		T temp = poss.get(RandomNumbers.randomGenerator.nextInt(poss.size()));
		System.out.println("I did something:\n" + temp.toString());
		return temp;
	}

}
