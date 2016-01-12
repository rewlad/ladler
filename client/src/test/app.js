
"use strict";

import SSEConnection from "../main/sse-connection"
import KeepAlive     from "../main/keep-alive"
import VDom          from "../main/vdom"

SSEConnection("http://localhost:5556/sse",[KeepAlive(),VDom(document.body)],5)
