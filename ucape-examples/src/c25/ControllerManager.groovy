package c25

import org.jcsp.awt.*
import org.jcsp.lang.*
import org.jcsp.util.*
import org.jcsp.groovy.*
import java.awt.*
import java.awt.Color.*
import org.jcsp.net2.*;
import org.jcsp.net2.tcpip.*;
import org.jcsp.net2.mobile.*;

// modify this so that the controller asks the min and max number of pairs
// then generates them randomly
// it should automatically create a new game when all the pairs have been claimed
// this should be done with a random number of pairs
// that is the interaction to determine the number of pairs should be removed.
// when a person enrolls they are given the current state of the game
// which they can then join

class ControllerManager implements CSProcess{
	DisplayList dList

	ChannelOutput statusConfig
	ChannelOutput pairsConfig
	ChannelOutputList playerNames
	ChannelOutputList pairsWon

	ChannelInput receiveEvent
	ChannelOutput getEvent

	int maxPlayers = 8
	int side = 50
	int minPairs = 6
	int maxPairs = 18
	int boardSize = 6

	def currentGameState

	void run(){

		def int gap = 5
		def offset = [gap, gap]
		int graphicsPos = (side / 2)

		def rectSize = ((side+gap) *boardSize) + gap
		int pairsRange = maxPairs - minPairs

		def availablePlayerIds = ((maxPlayers-1) .. 0).collect{it}

		//println "$availablePlayerIds"
		def generatePairsNumber = { min, range ->
			def rng = new Random()
			def randomAmount = rng.nextInt(range)
			return min + randomAmount
		}
		def displaySize = 4 + (5 * boardSize * boardSize)
		GraphicsCommand[] display = new GraphicsCommand[displaySize]
		GraphicsCommand[] changeGraphics = new GraphicsCommand[5]
		changeGraphics[0] = new GraphicsCommand.SetColor(Color.WHITE)
		changeGraphics[1] = new GraphicsCommand.FillRect(0, 0, 0, 0)
		changeGraphics[2] = new GraphicsCommand.SetColor(Color.BLACK)
		changeGraphics[3] = new GraphicsCommand.DrawRect(0, 0, 0, 0)
		changeGraphics[4] = new GraphicsCommand.DrawString("   ",graphicsPos,graphicsPos)


		def createBoard = {
			display[0] = new GraphicsCommand.SetColor(Color.WHITE)
			display[1] = new GraphicsCommand.FillRect(0, 0, rectSize, rectSize)
			display[2] = new GraphicsCommand.SetColor(Color.BLACK)
			display[3] = new GraphicsCommand.DrawRect(0, 0, rectSize, rectSize)
			def cg = 4
			for ( x in 0..(boardSize-1)){
				for ( y in 0..(boardSize-1)){
					def int xPos = offset[0]+(gap*x)+ (side*x)
					def int yPos = offset[1]+(gap*y)+ (side*y)
					//print " $x, $y, $xPos, $yPos, $cg, "
					display[cg] = new GraphicsCommand.SetColor(Color.WHITE)
					cg = cg+1
					display[cg] = new GraphicsCommand.FillRect(xPos, yPos, side, side)
					cg = cg+1
					display[cg] = new GraphicsCommand.SetColor(Color.BLACK)
					cg = cg+1
					display[cg] = new GraphicsCommand.DrawRect(xPos, yPos, side, side)
					cg = cg+1
					xPos = xPos + graphicsPos
					yPos = yPos + graphicsPos
					display[cg] = new GraphicsCommand.DrawString("   ",xPos, yPos)
					//println "$cg"
					cg = cg+1
				}
			}
		} // end createBoard

		def colours = [Color.MAGENTA, Color.CYAN, Color.YELLOW, Color.PINK]
		def pairsMap =[:]

		def initPairsMap = {
			for ( x in 0 ..< boardSize){
				for ( y in 0 ..< boardSize){
					pairsMap.put([x,y], null)
				}
			}
		}

		def changePairs = {x, y, colour, p ->
			def int xPos = offset[0]+(gap*x)+ (side*x)
			def int yPos = offset[1]+(gap*y)+ (side*y)
			changeGraphics[0] = new GraphicsCommand.SetColor(colour)
			changeGraphics[1] = new GraphicsCommand.FillRect(xPos, yPos, side, side)
			changeGraphics[2] = new GraphicsCommand.SetColor(Color.BLACK)
			changeGraphics[3] = new GraphicsCommand.DrawRect(xPos, yPos, side, side)
			xPos = xPos + graphicsPos
			yPos = yPos + graphicsPos
			if ( p > -1)
				changeGraphics[4] = new GraphicsCommand.DrawString(" " + p, xPos, yPos)
			else
				changeGraphics[4] = new GraphicsCommand.DrawString("   ", xPos, yPos)
		}

		def createPairs = {np ->
			//println "createpairs: $np"
			/*
			 * have to check that all locations are distinct
			 * that is pairs map does not already contain a location that 
			 * is already in use
			 */
			def rng = new Random()
			initPairsMap()
			for (p in 1..np){
				def x1 = rng.nextInt(boardSize)
				def y1 = rng.nextInt(boardSize)
				//println "[x1, y1] = [$x1, $y1]"
				while ( pairsMap.get([x1,y1]) != null){
					//println "first repeated random location [$x1, $y1]"
					x1 = rng.nextInt(boardSize)
					y1 = rng.nextInt(boardSize)
				}
				pairsMap.put([x1, y1], [p, colours[p%4]])
				changePairs(x1, y1, colours[p%4], p)
				dList.change(changeGraphics, 4 + (x1*5*boardSize) + (y1*5))
				def x2 = rng.nextInt(boardSize)
				def y2 = rng.nextInt(boardSize)
				//println "[x2, y2] = [$x2, $y2]"
				while ( pairsMap.get([x2,y2]) != null){
					//println "second repeated random location [$x2, $y2]"
					x2 = rng.nextInt(boardSize)
					y2 = rng.nextInt(boardSize)
				}
				//println "final pairs: [$x1, $y1], [$x2, $y2] for $p"
				pairsMap.put([x2, y2], [p, colours[p%4]])
				changePairs(x2, y2, colours[p%4], p)
				dList.change(changeGraphics, 4 + (x2*5*boardSize) + (y2*5))
			}
		} // end createPairs


		def pairsMatch = {cp ->
			// cp is a list comprising two elements each of which is a list with the [x,y]
			// location of a square
			// returns 0 if only one square has been chosen so far
			//         1 if the two chosen squares have the same value (and colour)
			//         2 if the chosen squares have different values
			if (cp[1] == null) return 0
			else {
				if (cp[0] != cp[1]) {
					def p1Data = pairsMap.get(cp[0])
					def p2Data = pairsMap.get(cp[1])
					if (p1Data[0] == p2Data[0]) return 1 else return 2
				}
				else  return 2
			}
		}

		// removed node


		def toPlayers = new ChannelOutputList()
		def fromPlayers = new ChannelInputList()
		for ( p in 0..<maxPlayers) toPlayers.append(null)
		for ( p in 0..<maxPlayers) fromPlayers.append(null)
		def currentPlayerId = 0
		def playerMap = [:]

		createBoard()
		dList.set(display)
		def nPairs = 0
		def pairsUnclaimed = 0
		def gameId = 0

		def connectedNames = [maxPlayers]

		def checkforEnrol = {


			def s = 1
			// request for player enrolment (if any)
			getEvent.write(s)


			// read response
			def responseFromBuffer = receiveEvent.read()
			if (responseFromBuffer.size() > 0)
			{
				println "response " + responseFromBuffer

				for (r in 0..responseFromBuffer.size())
				{
					if (responseFromBuffer[r] instanceof EnrolEvent)
					{

						def playerDetails = (EnrolEvent)responseFromBuffer[r]
						def playerName = playerDetails.name

						println "player name ${playerName}"

						def playerToAddr = playerDetails.toPlayerChannelLocation

						println "$playerToAddr"
						def playerToChan = NetChannel.one2net(playerToAddr)

						//println "name: ${playerDetails.name}"
						if (availablePlayerIds.size() > 0) {
							currentPlayerId = availablePlayerIds. pop()
							playerNames[currentPlayerId].write(playerName)

							// add name
							connectedNames[currentPlayerId] = playerName


							pairsWon[currentPlayerId].write(" " + 0)
							toPlayers[currentPlayerId] = playerToChan

							println "curr player id: " + currentPlayerId + " " + toPlayers[currentPlayerId]

							fromPlayers[currentPlayerId] = NetChannel.net2one()
							def fromLoc = fromPlayers[currentPlayerId].getLocation()
							// send location to channel
							toPlayers[currentPlayerId].write(new SendGameDetails(gameId: gameId, playerId: currentPlayerId, controllerLocation: fromLoc))

							def a = fromPlayers[currentPlayerId].read()

							println "from $currentPlayerId $a"
							playerMap.put(currentPlayerId, [playerName, 0])
						}
						else
						{
							// no new players can join the game
							playerToChan.write(new EnrolDetails(id: -1))
						}
					}
				}
			}

		}

		while (true) {
			statusConfig.write("Creating")
			//			nPairs = generatePairsNumber(minPairs, pairsRange)
			nPairs = maxPairs
			pairsUnclaimed = nPairs
			pairsConfig.write(" "+ nPairs)
			gameId = gameId + 1
			createPairs (nPairs)
			statusConfig.write("Running")
			def running = (pairsUnclaimed != 0)

			while (running)
			{

				checkforEnrol()

				// main while loop for turns
				for (i in 0..(toPlayers.size()-1))
				{
					if (toPlayers[i] == null)continue;

					def chosenCards = [null, null]

					def currentPair = 0

					def matchOutcome = 0

					def currentSelection

					// players turn
					while(matchOutcome < 2)
					{
						checkforEnrol()

						updatePlayers(toPlayers, playerMap, pairsMap, gameId, i, chosenCards, connectedNames)

						// ask for cards
						toPlayers[i].write()
						def response = fromPlayers[i].read()
						println "back to controller read in $response"

						// card chosen
						if (response instanceof SquareCoords)
						{
							println "sqaure"
							chosenCards[currentPair] = response.location

							println "chosenp $chosenCards[currentPair]"
							currentPair = currentPair + 1

							updatePlayers(toPlayers, playerMap, pairsMap, gameId, i, chosenCards, connectedNames)

						}
						else if (response instanceof WithdrawFromGame)
						{
							def withdraw = (WithdrawFromGame)response
							def id = withdraw.id
							def playerState = playerMap.get(id)
							println "Player: ${playerState[0]} claimed ${playerState[1]} pairs"
							playerNames[id].write("       ")
							pairsWon[id].write("   ")
							toPlayers[id] = null
							availablePlayerIds << id
							availablePlayerIds =  availablePlayerIds.sort().reverse()
							println "hello"
							break
						}
			
						matchOutcome = pairsMatch(chosenCards)

						// check for match
						if (matchOutcome == 1)
						{
							pairsMap.remove(chosenCards[0])
							pairsMap.remove(chosenCards[1])
							println "after remove of"
							def playerState = playerMap.get(i)
							playerState[1] = playerState[1] + 1
							pairsWon[i].write(" " + playerState[1])
							playerMap.put(i, playerState)
							pairsUnclaimed = pairsUnclaimed - 1
							pairsConfig.write(" "+ pairsUnclaimed)

							chosenCards[0] = null
							chosenCards[1] = null
							currentPair = 0
						}
					}

					println "end turn"
				}

			}

			createBoard()
			dList.change(display, 0)

		} // end while true
	} // end run


	def updatePlayers(def toPlayers, def playerMap, def pairsMap, def gameId, def currPlayer, def cards, def names)
	{
		// update all players
		for(p in 0..(toPlayers.size()-1))
		{
			// if not null and not current player
			if (toPlayers[p] != null)
			{
				println "write to players $p"
				toPlayers[p].write(new GameDetails( playerDetails: playerMap,
				pairsSpecification: pairsMap,
				gameId: gameId,
				currentPlayer: currPlayer,
				currentSelection: cards,
				playerNames: names))
				println "update player $p"
			}
		}
	}
}