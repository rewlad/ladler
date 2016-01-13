
"use strict";

import SSEConnection from "../main/sse-connection"
import Feedback      from "../main/feedback"

function TestShow(){
    var dataToShow

    function show(event){
        // console.log((new Date).getTime() % 10000,event.data % 100)
        dataToShow = event.data //+ " " + connectionKeyState + " " + sessionKey(function(){})
    }
    function animationFrame(){
        document.body.textContent = dataToShow
        requestAnimationFrame(animationFrame)
    }

    requestAnimationFrame(animationFrame)

    return ({show})
}

const receivers = [Feedback.receivers, TestShow()]
SSEConnection("http://localhost:5556/sse",receivers,5)
