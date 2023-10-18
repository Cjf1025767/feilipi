package main;

import java.util.Calendar;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Calloutrecord;
import hbm.model.Callouttask;
import hbm.model.CallouttaskEx;

public class TaskCreator extends TimerTask {
	public static Log log = LogFactory.getLog(TaskCreator.class);
	public static class CalloutrecordExtends extends Calloutrecord{
		public CalloutrecordExtends(Calloutrecord record,CallouttaskEx taskex) {
			this.setId(record.getId());
			this.setTaskid(record.getTaskid());
			this.setPhone(record.getPhone());
			this.setStatus(record.getStatus());
			this.setCalluuid(record.getCalluuid());
			this.setRetrycount(record.getRetrycount());
			this.setCompletestatus(record.getCompletestatus());
			this.setCompletedate(record.getCompletedate());
			this.setResults(record.getResults());
			this.setTaskorder(record.getTaskorder());
			this.setExecutions(record.getExecutions());
			this.setTalklength(record.getTalklength());
			this.taskex = taskex;
		}
		private static final long serialVersionUID = 1L;
		public CallouttaskEx taskex;
	}
	public static BlockingQueue<CalloutrecordExtends> calloutQueue = new LinkedBlockingQueue<CalloutrecordExtends>();
	private java.util.Map<String,CallTask> calltasks = new java.util.HashMap<>();
	@Override
	public void run() {
		//定时扫描数据库，实现启动和停止外呼
		Calendar current = Calendar.getInstance();
		try{
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					//激活的外呼任务,  status第一位等于1表示激活status第四位等于1表示外呼任务已经加载并激活
					String sQuery = calltasks.keySet().size()>0 ? " from CallouttaskEx where bitand(status,1)=1 and startdate<=:now and expiredate>:now and id not in(:ids)" 
							: " from CallouttaskEx where bitand(status,1)=1 and startdate<=:now and expiredate>:now";
					Query enableQuery = dbsession.createQuery(sQuery).setTimestamp("now", current.getTime());
					@SuppressWarnings("unchecked")//为加入队列的激活任务
					java.util.List<CallouttaskEx> enableTasks = calltasks.keySet().size()>0 ? enableQuery.setParameterList("ids", calltasks.keySet()).list() : enableQuery.list();
					for(CallouttaskEx task: enableTasks) {
						if(!calltasks.containsKey(task.getId())) {
							CallTask calltask = new CallTask();
							task.setStatus(task.getStatus()|0b1000);
							dbsession.update(task);
							calltask.setTask(task);
							calltasks.put(task.getId(),calltask);
						}
					}
					//关闭禁用任务，关闭过期任务
					sQuery = calltasks.keySet().size()>0 ? " from Callouttask where bitand(status,1)!=1 or expiredate<=:now and id in(:ids)"
							: " from Callouttask where bitand(status,1)!=1 or expiredate<=:now";
					Query disableQuery = dbsession.createQuery(sQuery).setTimestamp("now", current.getTime());
					@SuppressWarnings("unchecked")
					java.util.List<Callouttask> disableTasks = calltasks.keySet().size()>0 ? disableQuery.setParameterList("ids", calltasks.keySet()).list() : disableQuery.list();
					for(Callouttask task: disableTasks) {
						if(calltasks.containsKey(task.getId())){
							CallTask calltask = calltasks.get(task.getId());
							if(calltask!=null) {
								calltask.cancel();
								task.setStatus(task.getStatus()&0b1111111111110111);//第四位标记为未加载任务
								dbsession.update(task);
								calltasks.remove(task.getId());
							}
						}
					}
				}catch(org.hibernate.HibernateException e){
					log.error("ERROR:",e);
				}catch(Throwable e){
					log.error("ERROR:",e);
				}
				ts.commit();
			}catch(org.hibernate.HibernateException e){
				log.error("ERROR:",e);
			}catch(Throwable e){
				log.error("ERROR:",e);
			}
			dbsession.close();
		}catch(org.hibernate.HibernateException e){
			log.error("ERROR:",e);
		}catch(Throwable e){
			log.error("ERROR:",e);
		}
		for(CallTask item :calltasks.values()) {
			item.startTask();
		}
	}
}
