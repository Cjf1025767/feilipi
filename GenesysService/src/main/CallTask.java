package main;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Calloutphone;
import hbm.model.Calloutrecord;
import hbm.model.CallouttaskEx;
import main.TaskCreator.CalloutrecordExtends;
import tab.rest.CalloutServer;
import tab.util.Util;
import tab.util.chenUtil;

public class CallTask{
	//Task status的含义： 第一位激活标志，第二位是否呼叫中，第三位是否呼叫完成
	public static Log log = LogFactory.getLog(CallTask.class);
	private static BlockingQueue<String> waitEvent = new LinkedBlockingQueue<String>();
	private CallouttaskEx task;
	private volatile boolean bActive = false;
	private Timer timer = new Timer();
	public static int inboudClientTimeoutSeconds=60;
	private TimerTask timertask = new TimerTask() {
		private boolean start() {
			if(task==null) {
				log.error("start failed, task is null!");
				return false;
			}
			//判断是否加载电话外呼
			boolean bStart = true;
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try {
					Transaction ts = dbsession.beginTransaction();
					try {
						//获取最新任务状态
						task = (CallouttaskEx)dbsession.createCriteria(CallouttaskEx.class).add(Restrictions.eq("id", task.getId())).uniqueResult();
						//第一位手动激活暂停，第四位外呼任务是否加载
						if(task==null) {
							bStart = false;
							log.info("start failed, task is null, task deleted!");
							timer.cancel();
						}else if(bActive==false) {//第四位外呼任务是否加载
							bStart = false;
						}else if((task.getStatus()&0b1)==0b0) {
							bStart = false;
							log.info("task: "+task.getName()+", "+task.getId()+task.getId()+", status disable.");
						}else{
							//TODO: 
							//根据status确定了上次呼叫完成，重新开始新呼叫，执行次数加1
							//外呼电话记录里的次数也表示该外呼记录属于外呼任务的第几次外呼周期
							if(task.getPeriod()>0 && Calendar.getInstance().getTime().getTime()>task.getNextdate().getTime()) {
								//达到重复条件, 启动电话加载
							}else if(task.getPeriod()>0) {
								log.info("task interval: "+task.getName()+", "+task.getId()+", waiting.");
								bStart = false;
							}else if(task.getPeriod()==0) {
								//执行一次
							}else if(task.getPeriod()<0) {
								//工作日执行，一个工作日没执行完毕，下一个工作时间日继续，直到执行完毕再等待下一次工作日达到才能累加执行次数
								java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
								boolean bWorkDay = CalloutServer.CheckHolidays(dbsession,task.getWorkdeptid()==null?StringUtils.EMPTY:task.getWorkdeptid(), null,result);
								int aa=task.getStatus();
								int bb=task.getStatus()&0b10;
								Calendar current = Calendar.getInstance();
								if( !bWorkDay ) {
									task.setStatus(task.getStatus()&0b1111111111111101);
									log.info("task workdays: "+task.getName()+", "+task.getId()+", reset.");
									bStart = false;
								}else {
									//工作日执行且下次执行日期小于当前时间
//									//当天执行过一次之后将下次执行日期设置到明天
									if(current.getTime().getTime()>task.getNextdate().getTime()) {
										chenUtil util=new chenUtil();
										Date todate=util.dataadd(current);
										log.info("nextdate "+task.getName()+", "+task.getId()+":"+current.getTime());
										task.setNextdate(todate);
									}else{//当天已执行 直接返回false
										bStart = false;
									 }
								}
//								if((task.getStatus()&0b10)==0b10) {
//									if( !bWorkDay ) {
//										task.setStatus(task.getStatus()&0b1111111111111101);
//										log.info("task workdays: "+task.getName()+", "+task.getId()+", reset.");
//									}
//									bStart = false;
//								}else if(bWorkDay){
//									//工作日执行且下次执行日期小于当前时间
//									//当天执行过一次之后将下次执行日期设置到明天
//								
//									if(current.getTime().getTime()>task.getNextdate().getTime()) {
//										chenUtil util=new chenUtil();
//										Date todate=util.dataadd(current);
//										log.info("nextdate "+task.getName()+", "+task.getId()+":"+current.getTime());
//										task.setNextdate(todate);
//									}else{//当天已执行 直接返回false
//										bStart = false;
//									 }
//								}
							}else {
								log.error("task: "+task.getName()+", "+task.getId()+", start failed!");
								bStart = false;
							}
						}
					} catch (org.hibernate.HibernateException e) {
						log.error("ERROR:",e);
					} catch (Throwable e) {
						log.error("ERROR:",e);
					}
					ts.commit();
				} catch (org.hibernate.HibernateException e) {
					log.error("ERROR:",e);
				} catch (Throwable e) {
					log.error("ERROR:",e);
				}
				System.out.print(Calendar.getInstance().getTime()+"close");
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.error("ERROR:",e);
			} catch (Throwable e) {
				log.error("ERROR:",e);
			}
			return bStart;
		}
		private void complete() {
			try {
				Session dbsession = HibernateSessionFactory.getThreadSession();
				try {
					Transaction ts = dbsession.beginTransaction();
					try {
						task = (CallouttaskEx)dbsession.createCriteria(CallouttaskEx.class).add(Restrictions.eq("id", task.getId())).uniqueResult();
						if(task!=null){
							Calendar startdate = Calendar.getInstance();
							startdate.setTime(task.getStartdate());
							//设定到开始时间匹配的时分秒
							Calendar current = Calendar.getInstance();
						
							if(task.getPeriod()>0) {
								if(Calendar.getInstance().getTime().getTime()<task.getExpiredate().getTime()) {
									task.setStatus(task.getStatus()&0b1111111111111110);//进入暂停状态, 直到卸载
								}else {
									do {
										current.add(Calendar.MILLISECOND, task.getPeriod());
									}while(Calendar.getInstance().getTime().getTime()>current.getTime().getTime());
									//设置下次执行开始的时间，等待下次执行开始
									task.setNextdate(current.getTime());
								}
							}else if(task.getPeriod()==0) {
								task.setStatus(task.getStatus()&0b1111111111111110);//进入暂停状态，直到卸载
							}else{
								//完成所有电话的外呼，但是还在工作时间，则第二位表示下个工作日再启动
								task.setStatus(task.getStatus()|0b10);
							}
							
							task.setExecutions(task.getExecutions()+1);
							task.setFinishes(task.getFinishes()+1);
							dbsession.update(task);
						}
					} catch (org.hibernate.HibernateException e) {
						log.error("ERROR:",e);
					} catch (Throwable e) {
						log.error("ERROR:",e);
					}
					ts.commit();
				} catch (org.hibernate.HibernateException e) {
					log.error("ERROR:",e);
				} catch (Throwable e) {
					log.error("ERROR:",e);
				}
				dbsession.close();
			} catch (org.hibernate.HibernateException e) {
				log.error("ERROR:",e);
			} catch (Throwable e) {
				log.error("ERROR:",e);
			}
		}
		public void run() {
			if(!start()) {
				return;
			}
			//多次执行，或一次执行，或工作日执行，加载新的电话号码外呼
			if(task.getPeriod()>0) {
				log.info("task start interval:"+task.getName()+", id="+task.getId());
			}else if(task.getPeriod()==0){
				log.info("task start once:"+task.getName()+", id="+task.getId());
			}else if(task.getPeriod()<0){
				log.info("task start workdays:"+task.getName()+", id="+task.getId());
			}
			boolean bMoreRecord = false;
			do{
				try {
					Session dbsession = HibernateSessionFactory.getThreadSession();
					try {
						Transaction ts = dbsession.beginTransaction();
						try {
							//统计正在外呼的记录(status=1 and completestatus=0),实现并发控制
							//执行次数不同的外呼记录, 是过去执行周期里的外呼记录, 因此不影响当前呼叫任务
							Object[] objs = (Object[]) dbsession.createQuery(
									"select sum(case when status=1 and completestatus=0 then 1 else 0 end) as totalCount, max(taskorder) as taskOrder from Calloutrecord where taskid=:taskid and executions=:executions")
									.setString("taskid", task.getId()).setInteger("executions", task.getExecutions()).uniqueResult();
							Integer callCount = Util.ObjectToNumber(objs[0], 0);//获取当前呼叫数量
							Integer taskOrder = Util.ObjectToNumber(objs[1], 0);//获取当前呼叫的最大电话号码的编号，方便排序执行
							//控制并发数量
							int nCurrentNumber = 0;
							if(task.getAgentratio()>0) {
								nCurrentNumber = tab.util.Util.ObjectToNumber(dbsession.createQuery("select count(*) from Callextensioninfo where ntalkstatus=0 and bready>0 and bworktimecomplete=0").setFirstResult(0).setMaxResults(1).uniqueResult(),0); 
								log.info("task: "+task.getName()+", "+task.getId()+", current call:" + callCount + ", current ready agent:" + nCurrentNumber + ", executions:" + task.getExecutions() );
							}else {
								if(task.trunk!=null) {
									nCurrentNumber = task.trunk.getIntercurrent();
								}else {
									nCurrentNumber=Runner.nCurrentNumber;
								}
								log.info("task: "+task.getName()+", "+task.getId()+", current call:" + callCount + ", allow max calling:" + nCurrentNumber + ", executions:" + task.getExecutions() );
							}
							if (nCurrentNumber - callCount > 0) {
								//根据并发限制，获取一定数量新的外呼电话号码，并保持到外呼表Calloutrecord，状态为
								//每导入一个电话号码,taskorder加1,所以每个批次的电话号码的taskorder都是越晚导入的值越大
								@SuppressWarnings("unchecked")
								java.util.List<Calloutphone> phones = dbsession.createQuery(
										" from Calloutphone where id.batchid=:batch and taskorder>:taskorder order by taskorder asc")
										.setString("batch", task.getBatchid()).setInteger("taskorder", taskOrder)
										.setFirstResult(0).setMaxResults(nCurrentNumber - callCount).list();
								for (Calloutphone phone : phones) {
									Calloutrecord record = new Calloutrecord();
									//设计时缺少传递参数字段，所以临时使用这些字段传值
									//record.setCalluuid(task.trunk.getTrunk());//uuid传递trunk
									record.setCalluuid("");//uuid传递trunk
									record.setTalklength(task.trunk.getDelay());//talklength传递延迟
									//record.setResults(task.trunk.getName());//临时传递任务名, 作为IVR的操作码
									record.setResults("队列中");
									record.setCompletedate(Calendar.getInstance().getTime());
									record.setId(UUID.randomUUID().toString().toUpperCase());
									record.setPhone(phone.getId().getPhone());
									record.setRetrycount(0);
									record.setCompletestatus(STATUS_PROCESSING);
									record.setStatus(1);
									record.setTaskid(task.getId());
									record.setTaskorder(phone.getTaskorder());
									record.setExecutions(task.getExecutions());
									dbsession.save(record);
									bMoreRecord = true;
									waitEvent.clear();//初始化等待外呼完成事件
									CalloutrecordExtends recordex = new CalloutrecordExtends(record,task);
									TaskCreator.calloutQueue.put(recordex);//插入外呼队列
									
								}
								if(phones.size()==0)bMoreRecord = false;
							}else {
								//没有空闲并发数，等待有外呼记录完成
								waitEvent.clear();
							}
						} catch (org.hibernate.HibernateException e) {
							log.error("ERROR:",e);
						} catch (Throwable e) {
							log.error("ERROR:",e);
						}
						ts.commit();
					} catch (org.hibernate.HibernateException e) {
						log.error("ERROR:",e);
					} catch (Throwable e) {
						log.error("ERROR:",e);
					}
					dbsession.close();
				} catch (org.hibernate.HibernateException e) {
					log.error("ERROR:",e);
				} catch (Throwable e) {
					log.error("ERROR:",e);
				}
				if(bMoreRecord) {
					try {
						//等待完成的外呼，然后继续获取新的电话号码插入队列
						//判断队列是否为空,如果为空以下代码就循环等待队列中waitEvent.put--当阻塞队列空是poll执行三秒 阻塞队列会超时中断演示
						while(waitEvent.poll(3, TimeUnit.SECONDS)==null) {
							if(bActive==false)break;//如果外呼完成没有调用waitEvent.put，可通过执行暂停重启继续外呼
							if(task.getAgentratio()>0)break;//是转座席外呼超时立即更新空闲座席数，确定是否发起新外呼, 不是转座席，需要等待有外呼记录完成
						}
					} catch (InterruptedException e) {
						log.error("ERROR:",e);
						if(task.getPeriod()>0) {
							log.info("task error interval:"+task.getName()+", id="+task.getId());
						}else if(task.getPeriod()==0){
							log.info("task error once:"+task.getName()+", id="+task.getId());
						}else if(task.getPeriod()<0){
							log.info("task error workdays:"+task.getName()+", id="+task.getId());
						}
						return;
					}
				}
			}while(bMoreRecord && bActive);
			if(bMoreRecord==false) {
				//批次里的电话全部呼出完毕,任务则停止
				complete();
				if(task.getPeriod()>0) {
					log.info("task complete interval:"+task.getName()+", id="+task.getId());
				}else if(task.getPeriod()==0){
					log.info("task complete once:"+task.getName()+", id="+task.getId());
				}else if(task.getPeriod()<0){
					log.info("task complete workdays:"+task.getName()+", id="+task.getId());
				}
			}else if(bActive==false) {
				if(task.getPeriod()>0) {
					log.info("task paused interval:"+task.getName()+", id="+task.getId()+", and timer cancel.");
				}else if(task.getPeriod()==0){
					log.info("task paused once:"+task.getName()+", id="+task.getId()+", and timer cancel.");
				}else if(task.getPeriod()<0){
					log.info("task paused workdays:"+task.getName()+", id="+task.getId()+", and timer cancel.");
				}
			}else{
				if(task.getPeriod()>0) {
					log.info("task continue interval:"+task.getName()+", id="+task.getId());
				}else if(task.getPeriod()==0){
					log.info("task continue once:"+task.getName()+", id="+task.getId());
				}else if(task.getPeriod()<0){
					log.info("task continue workdays:"+task.getName()+", id="+task.getId());
				}
			}
		}
	};//End TimerTask
	public void startTask() {
		if(bActive || task==null)return;
		Calendar startdate = Calendar.getInstance();
		startdate.setTime(task.getStartdate());
		//设定到开始时间匹配的时分秒
		Calendar current = Calendar.getInstance();
//		current.set(Calendar.HOUR_OF_DAY, startdate.get(Calendar.HOUR_OF_DAY));
//		current.set(Calendar.MINUTE, startdate.get(Calendar.MINUTE));
//		current.set(Calendar.SECOND, startdate.get(Calendar.SECOND));
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		//开始任务的设定
		if(task.getPeriod()>0) {
			//定时执行
			while(Calendar.getInstance().getTime().getTime()>current.getTime().getTime()) {
				current.add(Calendar.SECOND, task.getPeriod()/1000);
			}
			log.info("task interval:"+task.getName()+":"+task.getId() + ", status="+task.getStatus() + ", time="+sdf.format(current.getTime()) );
			bActive = true;
			timer.scheduleAtFixedRate(timertask, current.getTime(), task.getPeriod()); 
		}else if(task.getPeriod()==0){
			//仅执行一次
			log.info("task once:"+task.getName()+":"+task.getId() + ", status="+task.getStatus() + ", time="+sdf.format(current.getTime()) );
			bActive = true;
			timer.schedule(timertask, startdate.getTime()); 
		}else if(task.getPeriod()<0){
			//工作日执行，15秒检查一次
			while(Calendar.getInstance().getTime().getTime()>current.getTime().getTime()) {
				current.add(Calendar.SECOND, 15);
			}
			log.info("task workdays:"+task.getName()+":"+task.getId() + ", status="+task.getStatus() + ", time="+sdf.format(current.getTime()) );
			bActive = true;
			timer.scheduleAtFixedRate(timertask, current.getTime(), 15000);
		}
	}
	public void cancel() {
		//task.setStatus(task.getStatus()&0b1111111111110111);//第四位标记为未加载任务
		bActive = false;
		timer.cancel();
		if(task.getPeriod()>0) {
			log.info("task cancel interval:"+task.getName()+", id="+task.getId());
		}else if(task.getPeriod()==0){
			log.info("task cancel once:"+task.getName()+", id="+task.getId());
		}else if(task.getPeriod()<0){
			log.info("task cancel workdays:"+task.getName()+", id="+task.getId());
		}
	}
	public CallouttaskEx getTask() {
		return task;
	}
	public void setTask(CallouttaskEx task) {
		this.task = task;
	}
	public static final int STATUS_PROCESSING = 0;
	public static final int STATUS_FAILED = -1;
	public static final int STATUS_COMPLETE = 1;
	
	public static void CallStat(String sId, String requestId) {
		int status=requestId==""?CallTask.STATUS_FAILED:CallTask.STATUS_COMPLETE;
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					Calloutrecord record = (Calloutrecord)dbsession.createCriteria(Calloutrecord.class).add(Restrictions.eq("id", sId)).uniqueResult();
					if(record!=null){
						//record.setCalluuid(sUuid);
						//record.setStatus(requestId);
						if(requestId=="") {
							 record.setResults(GenesysSSGService.requestFailMessage);
							 record.setCompletestatus(status);
							 LocalDateTime dtNow = LocalDateTime.now();
							 java.util.Date now = Date.from(dtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
							 record.setCompletedate(now);
							 dbsession.update(record);
							 log.info("Call Complete: "+record.getPhone());
						}else {
							final CompletableFuture<String> outbound = new CompletableFuture<String>();
							GenesysSSGService.outboundQueue.put(requestId, outbound);
							String result="";
							if (outbound != null) {
								try {
									 result=outbound.get(inboudClientTimeoutSeconds, TimeUnit.SECONDS);
									//当get获取不到消息的时候就会阻塞一直到获取消息或者设置的时间到
									 log.info(requestId+"的外呼结果为:"+result);
								} catch (Throwable e) {
									log.debug(e.toString());
								} finally {
									if(result=="") {//说明三分钟之内都没有调用GetNotifition接口  则当做ssg服务端挂了
										 record.setResults(GenesysSSGService.blockMessage);
										 status=CallTask.STATUS_FAILED;
										 record.setCompletestatus(status);
										 LocalDateTime dtNow = LocalDateTime.now();
										 java.util.Date now = Date.from(dtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
										 record.setCompletedate(now);
										 dbsession.update(record);
										 log.info("Call Complete: "+record.getPhone());
									}
									GenesysSSGService.outboundQueue.remove(requestId);
								}
							}
						}
					   
					}
				} catch (org.hibernate.HibernateException e) {
					log.error("ERROR:",e);
				} catch (Throwable e) {
					log.error("ERROR:",e);
				}
				ts.commit();
			} catch (org.hibernate.HibernateException e) {
				log.error("ERROR:",e);
			} catch (Throwable e) {
				log.error("ERROR:",e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			log.error("ERROR:",e);
		} catch (Throwable e) {
			log.error("ERROR:",e);
		}
		//每个外呼必须执行提交事件，否则导致不再加载新的外呼记录
		try {
			waitEvent.put(sId);
		} catch (InterruptedException e) {
			log.error("ERROR:",e);
		}
	}
}
