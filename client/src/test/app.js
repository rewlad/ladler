
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import VDom          from "../main/vdom"
import FeedbackVDomEvent from "../main/vdom-transforms"

const feedback = Feedback()
const componentClasses = {}
const transforms = FeedbackVDomEvent(componentClasses, feedback).transforms
const vdom = VDom(document.body, transforms)
const receivers = [feedback.receivers, vdom.receivers]
SSEConnection("http://localhost:5556/sse", receivers, 5)
