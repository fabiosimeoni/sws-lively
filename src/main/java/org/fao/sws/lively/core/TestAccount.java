package org.fao.sws.lively.core;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import org.fao.sws.ejb.GroupService;
import org.fao.sws.ejb.security.BatchAccount;
import org.fao.sws.model.security.SWSAccount;


@RequiredArgsConstructor
public class TestAccount implements SWSAccount {

	@Delegate(excludes=Excludes.class)
	final BatchAccount account;
	
	final GroupService groups;
	
	@Override
	public boolean isAdministrator() {
		
		return groups.getGroupsByUserId(account.getUserId())
					 .stream()
					 .filter(g->g.isAdministrator())
					 .findAny()
					 .isPresent();
					 
	}
	
	static interface Excludes {
		
		boolean isAdministrator();
		
	}
}
