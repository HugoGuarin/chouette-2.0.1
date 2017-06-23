package fr.certu.chouette.struts.interceptor;


import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.springframework.context.ApplicationContext;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

import fr.certu.chouette.struts.ActionLock;
import fr.certu.chouette.struts.SingletonManager;

@SuppressWarnings("serial")
public class SingleActionInterceptor extends AbstractInterceptor 
{

	
	@Override
	public String intercept(ActionInvocation invocation) throws Exception
	{
		ApplicationContext applicationContext = SingletonManager.getApplicationContext(); 
		ActionLock actionLock = (ActionLock) applicationContext.getBean("actionLock");
		
		HttpSession session = ServletActionContext.getRequest().getSession();
		String sessionId = session.getId();
		String lockSessionId = actionLock.getSessionId();
		Object action = invocation.getAction();
		
		// retrieve current requested action
		String actionName = action.getClass().getSimpleName();
		
		// if request action is deconnection. If so and if the requester is
		// the current owner of lock, the lock is released.
		//TODO DeconnexionAction Name should be retrieve via (an) action map 
		if (actionName.equals("DeconnexionAction"))
		{			
			// Release token if deconnexion request comes from current lock owner
			if (lockSessionId != null && lockSessionId.equals(sessionId))
			{
				actionLock.releaseLock();
			}
		}
		// if lock is free take session and set http session timeout
		else if (lockSessionId == null || actionLock.isTimeoutExpired())
		{
			actionLock.reserveLock(sessionId);
			session.setMaxInactiveInterval(actionLock.getTimeout());
		}
		// if lock is owned by current user update lock taken time
		else if (sessionId.equals(lockSessionId))
		{
			actionLock.initTakenAt();
		}
		// app session is busy, invalidate http session and 
		// return for user to be redirected towards login screen  
		else
		{
			session.invalidate();			
			return "busy-session";
		}
		
		// action invocation
		return invocation.invoke();
	}
}
