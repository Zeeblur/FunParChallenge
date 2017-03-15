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

class EventReceiver implements CSProcess {
	
	ChannelOutput IPlabelConfig
	ChannelOutput eventOut // To OWBuffer

	
	public void run()
	{
		// create a Node and the fromPlayers net channel
		def nodeAddr = new TCPIPNodeAddress (3000)
		Node.getInstance().init (nodeAddr)
		IPlabelConfig.write(nodeAddr.getIpAddress())
		//println "Controller IP address = ${nodeAddr.getIpAddress()}"

		def fromPlayers = NetChannel.net2one()
		def fromPlayersLoc = fromPlayers.getLocation()
		
	//	println "Controller: fromPlayer channel location - ${fromPlayersLoc.toString()}"

		eventOut.write(fromPlayers.read())
	}

}
