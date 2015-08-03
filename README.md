# sws-lively
Run automated `@Test`s in an embedded EJB container without build, packaging, and button-clicking costs.


#Setup

- configure `~/m2/.settings` with the properties required in `src/main/resources/templates/connection.properties`, e.g.:

>
	...
 	<profiles\>
  		...
		<profile>	
			<id>sws</id>
			<activation><activeByDefault>true</activeByDefault></activation>
			<properties>
				<sws.local.user>...</sws.local.user>
				<sws.local.pwd>...</sws.local.pwd>
				<sws.qa.user>...</sws.qa.user>
				<sws.qa.pwd>...</sws.qa.pwd>
				<sws.prod.user>...</sws.prod.user>
				<sws.prod.pwd>...</sws.prod.pwd>
			</properties>
		</profile>
	</profiles>
	...
	
	(note: user must be full admin).
	
- soft-link your `domain1`'s `config` to `src/main/resource/config` and tests will hook to it (it's `git`-ignored). 
- make sure `sws` project is in the workspace, as it will need to be `mvn`-resolved from there (we have no `mvn` repo to host it).
- adjust the `<sws.version>` you want to work with in `pom.xml`.

#Usage

Derive test suites from `SwsTest`. With each `@Test` you get:

* a container up and running,logging to the IDE's console, and connected to your local database;
* all your `@Inject`s resolved (services, ejbs, daos, configuration, test utils, etc..)
* an administrator already logged in;
* a sandbox that rolls back any changes to the DB at the end of the test.

In each `@Test` you can use:

* a set of thematic _modules_ that you can `star`-import to easily and clearly articulate your tests.
* e.g. `star`-import `Users` to work with users, groups, and permissions.
* e.g. `star`-import `Modules` to work with R-modules. 
* e.g. `star`-import `Sessions` to work with R-modules. 
* e.g. `star`-import `Configuration` to work with datasets, domains, and other config elements. 
* and so on.
 
Enrich existing modules and create new modules as you add more tests. 

In addition: 

* you can annotate test suites with `@Database` to debug issues directly against the data in QA or production. Two suites are ready for this type of 'tests' in `org.acme.issues`. Naming practices here are to use the `Jira` names as test names.
 


