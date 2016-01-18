
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDomMix       from "../main/vdom-mix"



const feedback = Feedback()
const componentClasses = {}
const vdom = VDomMix(feedback, componentClasses)
const receivers = [feedback.receivers, vdom.receivers]
SSEConnection("http://localhost:5556/sse", receivers, 5)
