package c25

import org.jcsp.awt.*
import org.jcsp.lang.*
import org.jcsp.util.*
import org.jcsp.groovy.*

class Controller implements CSProcess {
	int maxPlayers = 5
	
	void run(){
		def dList = new DisplayList()
		def gameCanvas = new ActiveCanvas()	
		gameCanvas.setPaintable(dList)	
		def statusConfig = Channel.createOne2One()
		def IPlabelConfig = Channel.createOne2One()
		def pairsConfig = Channel.createOne2One()
		def playerNames = Channel.createOne2One(maxPlayers)
		def pairsWon = Channel.createOne2One(maxPlayers)
		def playerNamesIn = new ChannelInputList(playerNames)
		def playerNamesOut = new ChannelOutputList(playerNames)
		def pairsWonIn = new ChannelInputList(pairsWon)
		def pairsWonOut = new ChannelOutputList(pairsWon)
		
		def enrolToBuffer = new Channel().createOne2One()
		def receiveEvent = new Channel().createOne2One()
		def getEvent = new Channel().createOne2One()
		
		def network = [ 
			new EventReceiver (IPlabelConfig: IPlabelConfig.out(),
				eventOut: enrolToBuffer.out()),
			
			
			new EventOWBuffer (inChannelFromRec: enrolToBuffer.in(),
				getChannelFromCon: getEvent.in(),
				outChannelToCon: receiveEvent.out()),
			new ControllerManager ( dList: dList,
												statusConfig: statusConfig.out(),
												receiveEvent: receiveEvent.in(),
												getEvent: getEvent.out(),
												pairsConfig: pairsConfig.out(),
												playerNames: playerNamesOut,
												pairsWon: pairsWonOut,
												maxPlayers: maxPlayers
											  ),
						new ControllerInterface( gameCanvas: gameCanvas,
												 statusConfig: statusConfig.in(),
												 IPlabelConfig: IPlabelConfig.in(),
												 pairsConfig: pairsConfig.in(),
												 playerNames: playerNamesIn,
												 pairsWon: pairsWonIn
											   )
				  ]
		new PAR (network).run()
	}

}
