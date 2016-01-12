
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
function connect(data) {
    console.log("conn: "+data)
    connectionKeyState = data
    sessionKey(function(){ sessionStorage.setItem("sessionKey", getConnectionKey(never)) })
    getLoadKey(function(){ loadKeyState = getConnectionKey(never) })
    localStorage.setItem(loadKeyForSession(), getLoadKey(never))
    pong()
}
function ping(data) {
    console.log("ping: "+data)
    if(localStorage.getItem(loadKeyForSession()) !== getLoadKey(never)) { // tab was refreshed/duplicated
        sessionStorage.clear()
        location.reload()
    } else if(getConnectionKey(never) === data) pong() // was not reconnected
}

export default function handlers() { return ({connect,ping}) }
