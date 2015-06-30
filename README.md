# sws-ooc-test
Out-of-Container IntegrationTests for the SWS.

#Howto

- configure `src/main/resources/connection.properties` to match your local PG installation (note: user must be full admin).
- link your "domain1"'s `config` to `src/main/resource/config` and tests will hook to it. it's git-ignored. 
- make sure `sws-domain`, `sws-model`,  `sws-web` and [`sws-playground`](https://github.com/fabiosimeoni/sws-playground) are in the workspace (we have no mvn repo to host them).
- adjust the `<sws.version>` you want to work with in `pom.xml` (e.g. point it to trunk, or a freature branch)
- derive suites from `LiveTest`.

With each `@Test` you get:

* a container up and running,logging to the IDE's console, and connected to your local database;
* all your `@Inject`s resolved (services, ejbs, daos, configuration, test utils, etc..)
* an administrator already logged in;
* a sandbox that rolls back any changes to the DB at the end of the test.

In each `@Test` you can use:

* a set of thematic facilities that you can `star`-import to easily and clearly articulate your tests.
* e.g. `star`-import `Users` to work with users, groups, and permissions.
* e.g. `star`-import `Modules` to work with R-modules. 
 
`
 


