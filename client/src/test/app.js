
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"
import Update        from "../main/vdom-update"

SSEConnection("http://localhost:5556/sse",[
    Feedback.receivers,
    Update(VDom.render(document.body))
],5)
