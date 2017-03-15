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
	  preCon[GETCHANNEL] = false
	  //def e = new EventData ()
	  //def missed = -1
	  
	  def event
	  
	  while (true) {
		def index = owbAlt.priSelect ( preCon )
		
		switch ( index ) {
		  case INCHANNEL:
			event = inChannelFromRec.read().copy()// may need copy
			preCon[GETCHANNEL] = true
			break
			
		  case GETCHANNEL:
			def s = getChannelFromCon.read()
			outChannelToCon.write ( event )
			preCon[GETCHANNEL] = false
			break
			
		}  // end switch
	  }  // end while
	}  // end run


}
