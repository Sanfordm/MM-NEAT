cd ..
cd ..
java -jar target/MM-NEAT-0.0.1-SNAPSHOT.jar runNumber:%1 randomSeed:%1 base:mariolevel trials:1 mu:50 maxGens:500 io:true netio:true mating:true fs:false task:edu.southwestern.tasks.mario.MarioLevelTask log:MarioLevel-AStar saveTo:AStar allowMultipleFunctions:true ftype:0 netChangeActivationRate:0.3 cleanFrequency:50 recurrency:false saveInteractiveSelections:false simplifiedInteractiveInterface:false saveAllChampions:false cleanOldNetworks:true logTWEANNData:false logMutationAndLineage:false marioLevelLength:120 marioStuckTimeout:20 watch:true
