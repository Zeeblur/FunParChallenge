package c25

import java.io.Serializable;
import org.jcsp.groovy.JCSPCopy



class EnrolEvent implements Serializable, JCSPCopy  
{
	def name = ""
	def toPlayerChannelLocation = null

	
	def copy() {
		def e = new EnrolEvent(name:this.name, 
			toPlayerChannelLocation:this.toPlayerChannelLocation)
		return e
	}
}
