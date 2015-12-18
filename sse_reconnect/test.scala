
/*
sudo iptables -L
sudo iptables -I INPUT -j DROP
sudo iptables -D INPUT 1
*/

import java.net.ServerSocket

object Test extends App {
    val srvr = new ServerSocket(5555)
    val skt = srvr.accept()
    val out = skt.getOutputStream
    //shutdownInput
    println("Server has connected")
    for(i <- 0 to 99){
        val str = s"[$i]\n"
        val b = str.getBytes("UTF-8")
        out.write(b) 
        out.flush()
        println(str)
        Thread.sleep(1000)
    }
    
}

// in case of net problems write can: just work as ok, throw and m. b. block;
/*
client gets static doc with EventSource-eval;
client connects;
server accepts, do not read;
server generates connection_id and register socket/desktop/last_time under it;
server sends reset
server sends connection_id
server sends sub-ajax which will use connection_id in url
server sends run-bind_sess_desk
client gets desktop_id from window or sets equal to connection_id
client gets sess_tickets from sessionStorage (not id sec) 
client sends bookmark, sess_tickets or desktop_id
server binds existing on makes new desktop

connection thread waits for ping or invalidate, execs view;

?scheduler -> wake OR just wait(timeout)
?db-action -> trottle -> wake

scheduled clean-up looks at last_time, closes sockets, removes keys
*/

/*
iptables -I INPUT 1 -j DROP
vika-HP-EliteBook-8440p jstest # iptables -D INPUT 1
*/

/*
events may be lost, so tree breaks and desktop-tree == connection;
but desktop-models (filters) are more persistent (window.)


sse in chrome has limited number? in any browser sum with long-poll?
does ws affected?
separate domain for the long-polling connection

ff does not reconnect sse

postMessage needs ref to win;
use local storage to communicate:
on write value "storage" event is fired on every tab (except the current one).

or we'll need dynamic domain per source restarter

*/

/*
k-v DB, sorted, with tran-s, small mem;
segments with log/history data by access/sharing group
segments with main data by private/public access 
segments with (req-k,resp-v,dep-req-k-s) by vcprop-ver


indexes / watchers / mat-view
id-integrity
life


kg.k=v
main data:
objid.propid:=val
index:
propid-val.objid:=
dep: if left was changed then recheck right:
objid.propid=>propid-val.objid:=
dep: if left to be changed then reset all right:
propid-val.objid<=#:=objid.propid

deps(objid.propid)
searcher will have dep(range("propid-val.","propid-val;"));

dual del/add k=v with vk= ; or multi o=abc ao= bo= ; indexable is flag of val

fact.dep(n)=reason


couch: emit(k,v,docid)

MLDB:
ACID Transactions
Sorted Duplicate Keys
Multi-thread/Multi-process concurrency
Atomic hot backup
no index

LevelDB:
WriteBatch; no tran?
uniq keys
asc iter; nodesc?
sync/async
1proc/many threads
snapshots
index?

RocksDB upgrades LevelDB for big dbs/ssd

ForestDB
open a separate handle on a ForestDB file for each thread
doc db
sync/async commit
iterate by seq-num 
snapshot by seq-num 
rollback to seq-num 
*/


import java.net.{Socket,InetSocketAddress}
import com.sun.net.httpserver.{HttpServer,HttpHandler,HttpExchange}

class MyHandler extends HttpHandler {
    def handle(t: HttpExchange) = {
        //t.getResponseHeaders.add("Access-Control-Allow-Origin","http://localhost:5556")
        val bytes = response.getBytes("UTF-8")
        t.sendResponseHeaders(200, bytes.length);
        val os = t.getResponseBody
        os.write(bytes)
        os.close()
    }
    def response = """<!DOCTYPE html>
<head><meta charset="utf-8"><title>...</title>
<script>"use strict";
(function(){

var eventSource = new EventSource("http://localhost:5556/sse");
eventSource.onmessage = function (event) {
    event.data.split('\n').forEach(function(str,j){
        var el = JSON.parse(str);
        console.log(el);
    })
};

})()
</script>
</head>"""
}

class MyConnection(skt: Socket) extends Runnable {
    def run() = {
        val out = skt.getOutputStream
        println("connected")
        def write(str: String) = { out.write(str.getBytes("UTF-8")); out.flush() }
        
        write("HTTP/1.1 200 OK\nContent-Type: text/event-stream\nAccess-Control-Allow-Origin: *\n\n")
        for(i <- 0 to 99){
            write(s"data:$i\n\n")
            Thread.sleep(1000)
        }
    }
}

object Test0 extends App {
    val server = HttpServer.create(new InetSocketAddress(5557),0);
    server.createContext("/", new MyHandler())
    server.start()
    
    val srvr = new ServerSocket(5556)
    println("ready")
    while(true){
        val skt = srvr.accept()
        new Thread(new MyConnection(skt)).start()
        
    }
}