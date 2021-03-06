/**
 *  Elder Care
 *
 *  Author: SmartThings
 *  Date: 2013-03-06
 *
 *  Stay connected to your loved ones. Get notified if they are not up and moving around 
 *  by a specified time and/or if they have not opened a cabinet or door according to a set schedule. 
 */

preferences {
	section("Who are you checking on?") {
		input "person1", "text", title: "Name?"
	}
	section("If there's no movement (optional, leave blank to not require)...") {
		input "motion1", "capability.motionSensor", title: "Where?", required: false
	}
	section("or a door or cabinet hasn't been opened (optional, leave blank to not require)...") {
		input "contact1", "capability.contactSensor", required: false
	}
	section("between these times...") {
		input "time0", "time", title: "From what time?", required: false
		input "time1", "time", title: "Until what time?"
	}
	section("Text me an alert at (sends push notification if not specified)...") {
		input "phone1", "phone", title: "Phone number?", required: false
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	schedule(time1, "scheduleCheck")
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe() //TODO no longer subscribe like we used to - clean this up after all apps updated
	unschedule()
	schedule(time1, "scheduleCheck")
}

def scheduleCheck()
{
	if(noRecentContact() && noRecentMotion()) {
		def person = person1 ?: "your elder"
		def msg = "Alert! There has been no activity at ${person}'s place ${timePhrase}"
		log.debug msg
		if (phone1) {
			sendSms(phone1, msg)
		}
		else {
			sendPush(msg)
		}
	} else {
		log.debug "There has been activity ${timePhrase}, not sending alert"
	}
}

private noRecentMotion()
{
	if(motion1) {
		def motionEvents = motion1.eventsSince(sinceTime)
		log.trace "Found ${motionEvents?.size() ?: 0} motion events"
		if (motionEvents.find { it.value == "active" }) {
			log.debug "There have been recent 'active' events"
			return false
		} else {
			log.debug "There have not been any recent 'active' events"
			return true
		}
	} else {
		log.debug "Motion sensor not enabled"
		return true
	}
}

private noRecentContact()
{
	if(contact1) {
		def contactEvents = contact1.eventsSince(sinceTime)
		log.trace "Found ${contactEvents?.size() ?: 0} door events"
		if (motionEvents.find { it.value == "open" }) {
			log.debug "There have been recent 'open' events"
			return false
		} else {
			log.debug "There have not been any recent 'open' events"
			return true
		}
	} else {
		log.debug "Contact sensor not enabled"
		return true
	}
}

private getSinceTime() {
	if (time0) {
		return timeToday(time0)
	}
	else {
		return new Date(now() - 21600000)
	}
}

private getTimePhrase() {
	def interval = now() - sinceTime.time
	if (interval < 3600000) {
		return "in the past ${Math.round(interval/60000)} minutes"
	}
	else if (interval < 7200000) {
		return "in the past hour"
	}
	else {
		return "in the past ${Math.round(interval/3600000)} hours"
	}
}