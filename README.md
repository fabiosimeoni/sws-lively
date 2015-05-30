# sws-ooc-test
Out-of-Container IntegrationTests for the SWS.

#Howto

- make sure you have a local PG installation with `sws_data` db accessible to `sws` user with `sws` pwd. 
- place your "domain1" config under `src/main/resource/config` and tests will hook to it. it's git-ignored. 
- `config` can be a symbolic link to local GF's installation.

