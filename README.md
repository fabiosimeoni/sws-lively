# sws-ooc-test
Out-of-Container IntegrationTests for the SWS.

#Howto

- match LocalSource#source() to match your local PG installation, user must be full admin.
- link your "domain1"'s `config` to `src/main/resource/config` and tests will hook to it. it's git-ignored. 
- you need `sws-domain`, `sws-model`, and [`sws-plaground`](https://github.com/fabiosimeoni/sws-playground) in the workspace (we have no mvn repo to host them).

