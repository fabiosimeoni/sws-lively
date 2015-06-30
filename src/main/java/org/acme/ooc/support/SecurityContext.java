package org.acme.ooc.support;

import lombok.RequiredArgsConstructor;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.fao.sws.domain.plain.operational.User;
import org.fao.sws.ejb.GroupService;
import org.fao.sws.ejb.security.BatchAccount;
import org.fao.sws.ejb.security.SwsBatchRealm;
import org.fao.sws.model.security.SWSAccount;

@RequiredArgsConstructor
public class SecurityContext {

	private final User user;
	private final GroupService service;
	
	private ThreadState threadState; 
	
	private Subject createSecurityContext(){
		
		Realm realm = new SwsBatchRealm();
		DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);
		SecurityUtils.setSecurityManager(securityManager);
		
		SimpleSession session = new SimpleSession();
		session.setExpired(false);
		session.setAttribute(SWSAccount.SESSION_KEY, new TestAccount(new BatchAccount(user,false),service));;
		 
		return new Subject.Builder()
			.authenticated(true)
			.sessionCreationEnabled(false)
			.session(session)
			.principals(new SimplePrincipalCollection(user,realm.getName()))
			.buildSubject();
	}
	
	
	public void bind(){
		if(threadState != null){
			remove();
		}
		threadState = new SubjectThreadState(createSecurityContext());
		threadState.bind();
	}
	
	public void remove(){
		if(threadState != null){
			threadState.restore();
		}
	}
}
