# ladler
Scala server-centric web application framework with:
- React component tree sync from server to client with SSE
- material design
- event-sourcing-like approach
- multiple versions of reality (long transactions)
- integration with embedded key-value storage


To build client:
```
cd client
npm install
#debug, production, watch:
./node_modules/.bin/webpack -dpw
```

To build and run server:
```
cd server
sbt 'test:runMain ee.cone.base.test_sse.TestApp'
```
There can be in place of test_sse: test_react_db, test_layout, etc.

For demo app with LMDB persistence:

```
sbt runL
```

PATH may be fixed in case jdk or sbt are in some custom places:
```
PATH=/web/_data/jdk/bin:$PATH
```
