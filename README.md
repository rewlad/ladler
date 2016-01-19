# ladler
scala server-centric framework with React integration and material design

```
cd client
npm install
#debug, production, watch:
./node_modules/.bin/webpack -dpw


cd server
PATH=/web/_data/jdk/bin:$PATH sbt 'test:runMain io.github.rewlad.ladler.test_sse.TestApp'
PATH=/web/_data/jdk/bin:$PATH sbt 'test:runMain io.github.rewlad.ladler.test_react.TestApp'
```
