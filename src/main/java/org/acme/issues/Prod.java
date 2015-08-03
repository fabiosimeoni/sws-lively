package org.acme.issues;

import static org.fao.sws.lively.core.Database.Source.*;
import static org.fao.sws.lively.modules.Common.*;
import static org.fao.sws.lively.modules.Users.*;

import org.fao.sws.lively.SwsTest;
import org.fao.sws.lively.core.Database;
import org.junit.Test;

/**
 * These tests run against the data in production with local configuration.
 *
 */
@Database(PROD)
public class Prod extends SwsTest {

	@Test
	public void smoketest() {
		
		show (users(), u-> u.getUsername());
		
	}

}
