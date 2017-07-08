cd ..
cd ..
java -jar dist/MM-NEATv2.jar runNumber:%1 randomSeed:%1 base:othello trials:5 maxGens:200 mu:50 io:true netio:true mating:true task:edu.utexas.cs.nn.tasks.boardGame.SinglePopulationCompetativeCoevolutionBoardGameTask cleanOldNetworks:true fs:false log:Othello-HNSinglePopCompCoevolve10Rand5Champs50Gens saveTo:HNSinglePopCompCoevolve10Rand5Champs50Gens boardGame:boardGame.othello.Othello genotype:edu.utexas.cs.nn.evolution.genotypes.HyperNEATCPPNGenotype hyperNEAT:true boardGameOpponentHeuristic:boardGame.heuristics.StaticOthelloWPCHeuristic boardGameOpponent:boardGame.agents.treesearch.BoardGamePlayerMinimaxAlphaBetaPruning boardGamePlayer:boardGame.agents.treesearch.BoardGamePlayerMinimaxAlphaBetaPruning minimaxSearchDepth:2 minimaxRandomRate:0.10 hallOfFame:true hallOfFameXRandomChamps:true hallOfFameNumChamps:5 hallOfFameYPastGens:true hallOfFamePastGens:50 boardGameStaticOpponentRuns:5 boardGameOthelloFitness:true boardGameSimpleFitness:false