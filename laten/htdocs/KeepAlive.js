"use strict";

function KeepAlive(){
    var loadKeyState, connectionKeyState;

    function never(){ throw new Exception() }
    function pong(){
        fetch("/connection",{method:"post",headers:{
            "X-r-action": "pong",
            "X-r-connection": getConnectionKey(never),
            "X-r-session": sessionKey(never)
        }})
        console.log("pong")
    }
    function sessionKey(orDo){ return sessionStorage.getItem("sessionKey") || orDo() }
    function getLoadKey(orDo){ return loadKeyState || orDo() }
    function loadKeyForSession(){ return "loadKeyForSession-" + sessionKey(never) }
    function getConnectionKey(orDo){ return connectionKeyState || orDo() }
    function connect(event) {
        console.log("conn: "+event.data)
        connectionKeyState = event.data
        sessionKey(()=>sessionStorage.setItem("sessionKey", getConnectionKey(never)))
        getLoadKey(()=>{ loadKeyState = getConnectionKey(never) })
        localStorage.setItem(loadKeyForSession(), getLoadKey(never))
        pong()
    }
    function ping(event) {
        console.log("ping: "+event.data)
        if(localStorage.getItem(loadKeyForSession()) !== getLoadKey(never)) window.close() // tab was refreshed/duplicated
        if(getConnectionKey(never) === event.data) pong() // was not reconnected
    }

    return ({connect,ping})
}
