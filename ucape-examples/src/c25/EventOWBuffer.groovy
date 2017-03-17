package c25

import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventOWBuffer implements CSProcess
{	
	def ChannelInput inChannelFromRec
	def ChannelInput getChannelFromCon
	def ChannelOutput outChannelToCon
	 
	void run () {
	  def owbAlt = new ALT ( [inChannelFromRec, getChannelFromCon] )
	  
	  def INCHANNEL = 0
	  def GETCHANNEL = 1
	  def preCon = new boolean[2]
	  preCon[INCHANNEL] = true
	  preCon[GETCHANNEL] = true
	  
	  def event
	  def reading = false
	  def eventList = []
	  while (true) {

		// chose between in from reciever and get channel
		def index = owbAlt.priSelect ( preCon )
		


		switch ( index ) {
		  case INCHANNEL:

			event = inChannelFromRec.read().copy()
			eventList.add(event)
			println eventList
			// enrol player event has been read * don't set event to null
			reading = true
			break
			
		  case GETCHANNEL:

			def s = getChannelFromCon.read()

			// if not read from enrolplayer event is null
			if (!reading)
				eventList.clear()//event = null
				
			outChannelToCon.write ( eventList)
			
			// reading is finished
			reading = false
			break
			
		}  // end switch
	  }  // end while
	}  // end run


}
