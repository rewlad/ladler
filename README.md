# ladler
scala server-centric framework with React integration and material design

```
cd client
npm install
#debug, production, watch:
./node_modules/.bin/webpack -dpw
```

```
cd server
sbt 'test:runMain ee.cone.base.test_sse.TestApp'
```
There can be in place of test_sse: test_react_db, test_layout, etc.

```
sbt runL
```
for demo app with LMDB persistence


PATH may be fixed in case jdk or sbt are in some custom places:
```
PATH=/web/_data/jdk/bin:$PATH
```
