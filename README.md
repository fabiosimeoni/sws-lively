# sws-ooc-test
Out-of-Container IntegrationTests for the SWS.

#Howto

- configure `src/main/resources/connection.properties` to match your local PG installation (note: user must be full admin).
- link your "domain1"'s `config` to `src/main/resource/config` and tests will hook to it. it's git-ignored. 
- make sure `sws-domain`, `sws-model`,  `sws-web` and [`sws-playground`](https://github.com/fabiosimeoni/sws-playground) are in the workspace (we have no mvn repo to host them).
- derive suites from `LiveTest` and inject to your heart's content.

