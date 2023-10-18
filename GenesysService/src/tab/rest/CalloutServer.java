package tab.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import hbm.factory.HibernateSessionFactory;
import hbm.model.Callholidays;
import hbm.model.Calloutbatch;
import hbm.model.Calloutphone;
import hbm.model.CalloutphoneId;
import hbm.model.Calloutrecord;
import hbm.model.CalloutrecordEx;
import hbm.model.Callouttask;
import hbm.model.CallouttaskEx;
import hbm.model.Callouttrunk;
import main.CallTask;
import main.GenesysSSGService;
import main.Runner;
import tab.EXTJSSortParam;
import tab.RESTDateParam;
import tab.RESTMapParam;
import tab.rbac.RbacClient;
import tab.util.Util;

//对应
//server.addRestConfig(new tab.RestResourceConfig(),"/tab");
@Path("/call")
public class CalloutServer {
	public static Log log = LogFactory.getLog(CalloutServer.class);
	//API文档: (value = "权限系统回调接口", notes = "该函数接收权限系统传入的redirect_code(验证码),state(用户自定义数据),uid(用户ID),oid(用户所属roleguid), 注意使用验证码向权限服务器确认调用真实性")
	@GET
	@Path("/Login")
	public Response Login(@Context HttpServletRequest request, @QueryParam("redirect_code") String sCode,
			@QueryParam("state") String sState, @QueryParam("uid") String sUId, @QueryParam("oid") String sOId,
			@QueryParam("version") String sRedirectVersion) throws IOException {
		// 获取调用页面的sURL根路径, 如果路径和浏览器内地址不同，需注意反向代理服务器的设置，例如：nginx proxy_set_header Host
		// $http_host;
		log.info("ContextPath:" + request.getContextPath());
		log.info("PathInfo:" + request.getPathInfo());
		log.info("RequestURI:" + request.getRequestURI());
		log.info("RequestURL:" + request.getRequestURL());
		String sAccessPath = request.getHeader("Referer");
		if (sAccessPath == null || sAccessPath.length() == 0) {
			return Response.status(500).entity("Request processing failed; missing header 'Referer'!").build();
		}
		log.info("Referer:" + sAccessPath);
		log.info("Login: redirect_code=" + sCode + ", state=" + sState + ", uid=" + sUId + ", oid=" + sOId);
		String sOriginURL = "";
		String sVersion = "";
		Matcher matcher = Pattern.compile(
				"^((http://)|(https://))(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,5})*/[a-zA-Z0-9\\&%_\\./-~-]*?")
				.matcher(sAccessPath);
		if (matcher.find()) {
			sOriginURL = matcher.group();
			if (sOriginURL.length() > 0 && sOriginURL.substring(sOriginURL.length() - 1).equals("/")) {
				sOriginURL = sOriginURL.substring(0, sOriginURL.length() - 1);
			}
			sAccessPath = sAccessPath.substring(sOriginURL.length());
			matcher = Pattern.compile("^/(\\d+)(/.*)").matcher(sAccessPath);
			if (matcher.find() && matcher.groupCount() == 2) {
				sVersion = matcher.group(1);
			}
		} else {
			return Response.status(500).entity("Request processing failed; invalid URL: " + sAccessPath).build();
		}
		if (sRedirectVersion != null) {
			if (!sVersion.equals(sRedirectVersion)) {
				log.info("sendRedirect: " + sVersion + " -> " + sRedirectVersion);
			}
			sVersion = sRedirectVersion;
		}
		if (sVersion.length() > 0) {
			sAccessPath = "/" + sVersion;
		} else {
			sAccessPath = "";
		}
		String sBackOriginURL = sOriginURL + sAccessPath;
		if (sUId == null || sUId.length() == 0 || sCode == null) {
			// 不正确的回调，返回到根路径
			log.info("sendRedirect:" + sBackOriginURL);
			return Response.seeOther(URI.create(sBackOriginURL)).build();
		}
		// 收到回调请求后，需要验证该回调请求, 是否对接的权限系统发送来的
		sUId = Util.uncompressUUID(sUId);
		sOId = Util.uncompressUUID(sOId);
		HttpSession httpsession = request.getSession();
		if (RbacClient.checkAccessCode(sCode).length() == 0) {
			log.info("sendRedirect:" + sBackOriginURL);
			return Response.seeOther(URI.create(sBackOriginURL)).build();
		}
		httpsession.setAttribute("uid", sUId);
		httpsession.setAttribute("oid", sOId);
		if (sState != null && sState.length() > 0) {
			// 成功则使用用户自定义数据, 跳转到指定页面
			sState = Util.ObjectToString(URLDecoder.decode(sState, "UTF-8"));
			if (sState.charAt(0) == '/') {
				sOriginURL += sState;
			} else {
				sOriginURL += sAccessPath + "/" + sState;
			}
		} else {
			// 验证成功无跳转页面, 返回到根目录
			sOriginURL += "/";
		}
		log.info("sendRedirect:" + sOriginURL);
		return Response.seeOther(URI.create(sOriginURL)).build();
	}

	//API文档: (value = "删除外呼网关", notes = "")
	@POST
	@Path("/RemoveTrunk")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response RemoveTrunk(@Context HttpServletRequest R,
			@FormParam("id") String sId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Transaction ts = dbsession.beginTransaction();
			try {
				Callouttrunk trunk = (Callouttrunk) dbsession.createCriteria(Callouttrunk.class)
						.add(Restrictions.eq("id", sId)).uniqueResult();
					if (trunk != null) {
						dbsession.delete(trunk);			
						result.put("success", true);
					} else {
						result.put("msg", "无此记录");
					}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			ts.commit();
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "更新或添加外呼网关", notes = "")
	@POST
	@Path("/AddOrUpdateTrunk")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddOrUpdateTrunk(@Context HttpServletRequest R,
			@FormParam("id") String sId,
			@FormParam("activate") Boolean bActivate,
			@FormParam("intercurrent") Integer nIntercurrent,
			@FormParam("maxretrycount") Integer nMaxRetryCount,
			@FormParam("delay") Integer nDelay,
			@FormParam("name") String sName,
			@FormParam("timetolive") Integer sTimetolive,
			@FormParam("trunk") String sTrunk) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Transaction ts = dbsession.beginTransaction();
			Callouttrunk trunk = (sId != null)
					? (Callouttrunk) dbsession.createCriteria(Callouttrunk.class).add(Restrictions.eq("id", sId))
							.uniqueResult()
					: (new Callouttrunk());
			if (trunk != null) {
				if (sId == null) {
					trunk.setId(UUID.randomUUID().toString().toUpperCase());
					if (sTrunk != null)
						trunk.setTrunk(sTrunk);
					else
						trunk.setTrunk("中继网关");
					if (sName != null)
						trunk.setName(sName);
					else
						trunk.setName("名称");
					if (nDelay != null)
						trunk.setDelay(nDelay);
					else
						trunk.setDelay(0);
					if (nIntercurrent != null)
						trunk.setIntercurrent(nIntercurrent);
					else
						trunk.setIntercurrent(1);
					if (trunk.getIntercurrent() <= 0)
						trunk.setIntercurrent(1);
					if (nMaxRetryCount != null)
						trunk.setMaxretrycount(nMaxRetryCount);
					else
						trunk.setMaxretrycount(0);
					if (bActivate != null)
						trunk.setStatus(bActivate ? trunk.getStatus() | 1 : trunk.getStatus() & 0xFFFE);
					else
						trunk.setStatus(0);
					if (sTimetolive != null)
						trunk.setTimetolive(sTimetolive);
					else trunk.setTimetolive(60);
					trunk.setCreatedate(Calendar.getInstance().getTime());
					trunk.setUpdatedate(trunk.getCreatedate());
				} else {
					trunk.setUpdatedate(Calendar.getInstance().getTime());
				}
				if (sTrunk != null)
					trunk.setTrunk(sTrunk);
				if (sName != null)
					trunk.setName(sName);
				if (nDelay != null)
					trunk.setDelay(nDelay);
				if (nIntercurrent != null)
					trunk.setIntercurrent(nIntercurrent);
				if (nMaxRetryCount != null)
					trunk.setMaxretrycount(nMaxRetryCount);
				if (sTimetolive != null)
					trunk.setTimetolive(sTimetolive);
				
				if (bActivate != null)
					trunk.setStatus(bActivate ? trunk.getStatus() | 1 : trunk.getStatus() & 0xFFFE);
				dbsession.saveOrUpdate(trunk);
				ts.commit();
				result.put("item", trunk);
				result.put("success", true);
			} else {
				result.put("msg", "无此记录");
			}
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示外呼网关列表", notes = "支持分页和排序")
	@POST
	@Path("/ListTrunk")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListTrunk(@Context HttpServletRequest R,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			int nTotalCount = Util
					.ObjectToNumber(dbsession.createQuery("select count(*) from Callouttrunk").uniqueResult(), 0);
			if (nTotalCount > 0) {
				Criteria criteria = dbsession.createCriteria(Callouttrunk.class).setFirstResult(pageStart)
						.setMaxResults(pageLimit);
				if (Sort != null) {
					if (Sort.getsDirection().equalsIgnoreCase("asc")) {
						criteria.addOrder(Order.asc(Sort.getsProperty()));
					} else {
						criteria.addOrder(Order.desc(Sort.getsProperty()));
					}
				}
				result.put("items", criteria.list());
			} else {
				result.put("items", null);
			}
			result.put("total", nTotalCount);
			result.put("success", true);
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "删除外拨批次", notes = "")
	@POST
	@Path("/RemoveBatch")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response RemoveBatch(@Context HttpServletRequest R,
			@FormParam("id") String sId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Transaction ts = dbsession.beginTransaction();
			Calloutbatch batch = (Calloutbatch) dbsession.createCriteria(Calloutbatch.class)
					.add(Restrictions.eq("id", sId)).uniqueResult();
			if (batch != null) {
				dbsession.delete(batch);
				dbsession.createQuery("delete Calloutphone where id.batchid=:batchid")
						.setString("batchid", batch.getId()).executeUpdate();
				ts.commit();
				result.put("success", true);
			} else {
				result.put("msg", "无此记录");
			}
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}
	
	@SuppressWarnings("deprecation")
	//API文档: (value = "更新或添加外拨批次", notes = "")
	@POST
	@Path("/AddOrUpdateBatch")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddOrUpdateBatch(@Context HttpServletRequest R,
			@FormParam("id") String sId,
			@FormParam("activate") Boolean bActivate,
			@FormParam("name") String sName,
			@FormParam("description") String sDesc,
			@FormParam("fid") String sFId,
			@FormParam("row") Integer nRow,
			@FormParam("col") Integer nCol) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		
		try {
			Transaction ts = dbsession.beginTransaction();
			try {
				Calloutbatch batch = (sId != null)
						? (Calloutbatch) dbsession.createCriteria(Calloutbatch.class).add(Restrictions.eq("id", sId))
								.uniqueResult()
						: (new Calloutbatch());
				if (batch != null) {
					if (sId == null) {
						batch.setId(UUID.randomUUID().toString().toUpperCase());
						sId = batch.getId();
						if (sDesc != null)
							batch.setDescription(sDesc);
						else
							batch.setDescription("描述");
						if (sName != null)
							batch.setName(sName);
						else
							batch.setName("批次名称");
						if (bActivate != null)
							batch.setStatus(bActivate ? batch.getStatus() | 1 : batch.getStatus() & 0xFFFE);
						else
							batch.setStatus(0);
						batch.setCreatedate(Calendar.getInstance().getTime());
						batch.setUpdatedate(batch.getCreatedate());
					} else {
						batch.setUpdatedate(Calendar.getInstance().getTime());
					}
					if (sDesc != null)
						batch.setDescription(sDesc);
					if (sName != null)
						batch.setName(sName);
					if (bActivate != null)
						batch.setStatus(bActivate ? batch.getStatus() | 1 : batch.getStatus() & 0xFFFE);//第一位暂停0或启动任务1
					dbsession.saveOrUpdate(batch);
					if (sFId != null) {
						Integer taskOrder = Util.ObjectToNumber(
								dbsession.createQuery("select count(*) from Calloutphone where id.batchid=:batch")
										.setString("batch", sId).uniqueResult(),
								1) + 1;
						String sTmpFile = System.getProperty("org.sqlite.tmpdir") + File.separator + sFId;
						// 导入电话号码
						XSSFWorkbook workbook = null;
						workbook = new XSSFWorkbook(new FileInputStream(sTmpFile));
						for (int numSheets = 0; numSheets < workbook.getNumberOfSheets()
								&& numSheets < 1; numSheets++) {
							if (null != workbook.getSheetAt(numSheets)) {
								XSSFSheet aSheet = (XSSFSheet) workbook.getSheetAt(numSheets);
								for (int rowNumOfSheet = nRow; rowNumOfSheet <= aSheet
										.getLastRowNum(); rowNumOfSheet++) {
									XSSFRow aRow = aSheet.getRow(rowNumOfSheet);
									if (null != aRow) {
										if (null != aRow.getCell(nCol) && aRow.getCell(nCol).toString().length() > 0) {
											XSSFCell cell = aRow.getCell(nCol);
											cell.setCellType(HSSFCell.CELL_TYPE_STRING);
											Calloutphone eRow = new Calloutphone();
											eRow.setTaskorder(taskOrder);
											CalloutphoneId eRowId = new CalloutphoneId();
											eRowId.setBatchid(sId);
											eRowId.setPhone(cell.getStringCellValue());
											eRow.setId(eRowId);
											try {
												dbsession.save(eRow);
												taskOrder++;
											} catch (org.hibernate.NonUniqueObjectException err) {
												log.warn("Double Phone: " + eRowId.getPhone());
											}
										}
									}
								}
							}
						}
						workbook.close();
					}
					ts.commit();
					result.put("item", batch);
					result.put("success", true);
				} else {
					result.put("msg", "无此记录");
				}
			} catch (org.hibernate.HibernateException e) {
				if (ts.getStatus() == TransactionStatus.ACTIVE || ts.getStatus() == TransactionStatus.MARKED_ROLLBACK) {
					ts.rollback();
				} else
					ts.commit();
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
		} catch (IOException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示外拨批次列表", notes = "支持分页和排序")
	@POST
	@Path("/ListBatch")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListBatch(@Context HttpServletRequest R,
			@FormParam("start") @DefaultValue("0") Integer pageStart,
			@FormParam("limit") @DefaultValue("100") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(httpsession.getAttribute("uid")==null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			int nTotalCount = Util
					.ObjectToNumber(dbsession.createQuery("select count(*) from Calloutbatch").uniqueResult(), 0);
			if (nTotalCount > 0) {
				Criteria criteria = dbsession.createCriteria(Calloutbatch.class).setFirstResult(pageStart)
						.setMaxResults(pageLimit);
				if (Sort != null) {
					if (Sort.getsDirection().equalsIgnoreCase("asc")) {
						criteria.addOrder(Order.asc(Sort.getsProperty()));
					} else {
						criteria.addOrder(Order.desc(Sort.getsProperty()));
					}
				}
				result.put("items", criteria.list());
			} else {
				result.put("items", null);
			}
			result.put("total", nTotalCount);
			result.put("success", true);
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public class ExcelRow {
		public String A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X, Y, Z;
	}
	
	@POST
	@Path("/UploadWavFile")
	@Consumes("multipart/form-data")
	@Produces("application/json" + ";charset=utf-8")
	public Response UploadWavFile(@Context HttpServletRequest R, FormDataMultiPart formParams) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sTmpFile = null, sFId = null;
		java.util.Map<String, java.util.List<FormDataBodyPart>> parts = formParams.getFields();
		for (java.util.Map.Entry<String, java.util.List<FormDataBodyPart>> entrys : parts.entrySet()) {
			if (entrys.getKey().equalsIgnoreCase("fid")) {
				for (FormDataBodyPart part : entrys.getValue()) {
					log.error("fid: "+part.getValue());
					sFId = part.getValue();
				}
			}
		}
		if(sFId==null || sFId.length()==0) {
			return Response.status(200).entity(result).build();
		}
		for (java.util.Map.Entry<String, java.util.List<FormDataBodyPart>> entrys : parts.entrySet()) {
			if (entrys.getKey().equalsIgnoreCase("filename")) {
				for (FormDataBodyPart part : entrys.getValue()) {
					sTmpFile = System.getProperty("org.sqlite.tmpdir") + File.separator;
					FormDataContentDisposition file = part.getFormDataContentDisposition();
					int pos = file.getFileName().lastIndexOf(".");
					if (pos > 0) {
						sFId += file.getFileName().substring(pos);
					}
					InputStream fileInputStream = part.getValueAs(InputStream.class);
					try {
						sTmpFile += sFId;
						FileUtils.copyInputStreamToFile(fileInputStream, new java.io.File(sTmpFile));
					} catch (IOException e) {
						log.error("ERROR:", e);
						result.put("msg", e.toString());
						return Response.status(200).entity(result).build();
					}
				}
			}
		}
		result.put("fid", sFId);
		result.put("success", true);
		return Response.status(200).entity(result).build();
	}

	@SuppressWarnings("deprecation")
	//API文档: (value = "预览Excel文档", notes = "返回预览数据和预览文件id")
	@POST
	@Path("/PreviewBatch")
	@Consumes("multipart/form-data")
	@Produces("application/json" + ";charset=utf-8")
	public Response PreviewBatch(@Context HttpServletRequest R, FormDataMultiPart formParams) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		String sTmpFile = null, sFId = null;
		java.util.Map<String, java.util.List<FormDataBodyPart>> parts = formParams.getFields();
		for (java.util.Map.Entry<String, java.util.List<FormDataBodyPart>> entrys : parts.entrySet()) {
			if (entrys.getKey().equalsIgnoreCase("filename")) {
				for (FormDataBodyPart part : entrys.getValue()) {
					sFId = UUID.randomUUID().toString();
					sTmpFile = System.getProperty("org.sqlite.tmpdir") + File.separator;
					FormDataContentDisposition file = part.getFormDataContentDisposition();
					int pos = file.getFileName().lastIndexOf(".");
					if (pos > 0) {
						sFId += file.getFileName().substring(pos);
					}
					InputStream fileInputStream = part.getValueAs(InputStream.class);
					try {
						sTmpFile += sFId;
						FileUtils.copyInputStreamToFile(fileInputStream, new java.io.File(sTmpFile));
					} catch (IOException e) {
						log.error("ERROR:", e);
						result.put("msg", e.toString());
						return Response.status(200).entity(result).build();
					}
				}
			}
		}
		try {
			if (sTmpFile != null) {
				java.util.List<ExcelRow> eRows = new java.util.ArrayList<>();
				XSSFWorkbook workbook = null;
				workbook = new XSSFWorkbook(new FileInputStream(sTmpFile));
				for (int numSheets = 0; numSheets < workbook.getNumberOfSheets() && numSheets < 1; numSheets++) {
					if (null != workbook.getSheetAt(numSheets)) {
						XSSFSheet aSheet = (XSSFSheet) workbook.getSheetAt(numSheets);
						// 只取第一个sheet的前10行
						for (int rowNumOfSheet = 0; rowNumOfSheet <= aSheet.getLastRowNum()
								&& rowNumOfSheet < 10; rowNumOfSheet++) {
							XSSFRow aRow = aSheet.getRow(rowNumOfSheet);
							if (null != aRow) {
								ExcelRow eRow = new ExcelRow();
								for (int cellNumOfRow = 0; cellNumOfRow < aRow.getLastCellNum()
										&& cellNumOfRow < 26; cellNumOfRow++) {
									if (null != aRow.getCell(cellNumOfRow)
											&& aRow.getCell(cellNumOfRow).toString().length() > 0) {
										XSSFCell cell = aRow.getCell(cellNumOfRow);
										cell.setCellType(HSSFCell.CELL_TYPE_STRING);
										switch (cellNumOfRow + 65) {
										case 'A':
											eRow.A = cell.getStringCellValue();
											break;
										case 'B':
											eRow.B = cell.getStringCellValue();
											break;
										case 'C':
											eRow.C = cell.getStringCellValue();
											break;
										case 'D':
											eRow.D = cell.getStringCellValue();
											break;
										case 'E':
											eRow.E = cell.getStringCellValue();
											break;
										case 'F':
											eRow.F = cell.getStringCellValue();
											break;
										case 'G':
											eRow.G = cell.getStringCellValue();
											break;
										case 'H':
											eRow.H = cell.getStringCellValue();
											break;
										case 'I':
											eRow.I = cell.getStringCellValue();
											break;
										case 'J':
											eRow.J = cell.getStringCellValue();
											break;
										case 'K':
											eRow.K = cell.getStringCellValue();
											break;
										case 'L':
											eRow.L = cell.getStringCellValue();
											break;
										case 'M':
											eRow.M = cell.getStringCellValue();
											break;
										case 'N':
											eRow.N = cell.getStringCellValue();
											break;
										case 'O':
											eRow.O = cell.getStringCellValue();
											break;
										case 'P':
											eRow.P = cell.getStringCellValue();
											break;
										case 'Q':
											eRow.Q = cell.getStringCellValue();
											break;
										case 'R':
											eRow.R = cell.getStringCellValue();
											break;
										case 'S':
											eRow.S = cell.getStringCellValue();
											break;
										case 'T':
											eRow.T = cell.getStringCellValue();
											break;
										case 'U':
											eRow.U = cell.getStringCellValue();
											break;
										case 'V':
											eRow.V = cell.getStringCellValue();
											break;
										case 'W':
											eRow.W = cell.getStringCellValue();
											break;
										case 'X':
											eRow.X = cell.getStringCellValue();
											break;
										case 'Y':
											eRow.Y = cell.getStringCellValue();
											break;
										case 'Z':
											eRow.Z = cell.getStringCellValue();
											break;
										}
									}
								}
								eRows.add(eRow);
							}
						}
					}
				}
				workbook.close();
				result.put("fid", sFId);
				result.put("data", eRows);
				result.put("success", true);
			}
		} catch (IOException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示外拨批次电话号码列表", notes = "支持分页和排序")
	@POST
	@Path("/ListBatchPhone")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListBatchPhone(@Context HttpServletRequest R,
			@FormParam("id") String sId,
			@FormParam("phone") String sPhone,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			String sQuery = "select count(*) from Calloutphone where id.batchid=:batchid";
			if (sPhone != null && sPhone.length() > 0) {
				sQuery += " and id.phone like :phone";
			}
			Query query = dbsession.createQuery(sQuery).setString("batchid", sId);
			if (sPhone != null && sPhone.length() > 0) {
				query.setString("phone", "%" + sPhone + "%");
			}
			int nTotalCount = Util.ObjectToNumber(query.uniqueResult(), 0);
			if (nTotalCount > 0) {
				Criteria criteria = dbsession.createCriteria(Calloutphone.class).setFirstResult(pageStart)
						.setMaxResults(pageLimit);
				if (sPhone != null && sPhone.length() > 0) {
					criteria.add(Restrictions.like("id.phone", sPhone, MatchMode.ANYWHERE));
				}
				criteria.add(Restrictions.eq("id.batchid", sId));
				if (Sort != null) {
					if (Sort.getsDirection().equalsIgnoreCase("asc")) {
						criteria.addOrder(Order.asc(Sort.getsProperty()));
					} else {
						criteria.addOrder(Order.desc(Sort.getsProperty()));
					}
				}
				result.put("items", criteria.list());
			} else {
				result.put("items", null);
			}
			result.put("total", nTotalCount);
			result.put("success", true);
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "删除外拨任务", notes = "")
	@POST
	@Path("/RemoveTask")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response RemoveTask(@Context HttpServletRequest R,
			@FormParam("id") String sId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Transaction ts = dbsession.beginTransaction();
			Callouttask task = (Callouttask) dbsession.createCriteria(Callouttask.class).add(Restrictions.eq("id", sId))
					.uniqueResult();
			if (task != null) {
				dbsession.delete(task);
				ts.commit();
				result.put("success", true);
			} else {
				result.put("msg", "无此记录");
			}
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "更新或添加外拨任务", notes = "")
	@POST
	@Path("/AddOrUpdateTask")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddOrUpdateTask(@Context HttpServletRequest R,
			@FormParam("id") String sId,
			@FormParam("agentratio") Double dAgentratio,
			@FormParam("activate") Boolean bActivate,
			@FormParam("startdate") RESTDateParam startdate,
			@FormParam("expiredate") RESTDateParam expiredate,
			@FormParam("period") Integer nPeriod,
			@FormParam("name") String sName,
			@FormParam("batchid") String sBatchId,
			@FormParam("trunkid") String sTrunkId,
			@FormParam("workdeptid") String sWorkdeptId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			Transaction ts = dbsession.beginTransaction();
			CallouttaskEx task = (sId != null)
					? (CallouttaskEx) dbsession.createCriteria(CallouttaskEx.class).add(Restrictions.eq("id", sId))
							.uniqueResult()
					: (new CallouttaskEx());
			if (task != null) {
				if (sId == null) {
					task.setId(UUID.randomUUID().toString().toUpperCase());
					if (sTrunkId != null) {
						task.setTrunkid(sTrunkId);
						task.trunk = (Callouttrunk) dbsession.createQuery(" from Callouttrunk where id=:id")
								.setString("id", task.getTrunkid()).uniqueResult();
					} else {
						task.trunk = (Callouttrunk) dbsession.createQuery(" from Callouttrunk where bitand(status,1)=1")
								.setFirstResult(0).setMaxResults(1).uniqueResult();
						task.setTrunkid(task.trunk != null ? task.trunk.getId() : "");
					}
					if (sWorkdeptId != null) {
						task.setWorkdeptid(sWorkdeptId);
					}else {
						task.setWorkdeptid(StringUtils.EMPTY);
					}
					if (sBatchId != null) {
						task.setBatchid(sBatchId);
						task.batch = (Calloutbatch) dbsession.createQuery(" from Calloutbatch where id=:id")
								.setString("id", task.getBatchid()).uniqueResult();
					} else {
						task.batch = (Calloutbatch) dbsession.createQuery(" from Calloutbatch order by updatedate desc")
								.setFirstResult(0).setMaxResults(1).uniqueResult();
						task.setBatchid(task.batch != null ? task.batch.getId() : "");

					}
					if (expiredate != null)
						task.setExpiredate(expiredate.getDate());
					else {
						task.setExpiredate(Calendar.getInstance().getTime());
					}
					if (startdate != null)
						task.setStartdate(startdate.getDate());
					else {
						task.setStartdate(Calendar.getInstance().getTime());
					}
					if (nPeriod != null)
						task.setPeriod(nPeriod);
					else
						task.setPeriod(0);
					if (dAgentratio != null)
						task.setAgentratio(dAgentratio);
					else
						task.setAgentratio(0);
					if (sName != null)
						task.setName(sName);
					else
						task.setName("新外呼任务");
					if (bActivate != null)
						task.setStatus(bActivate ? task.getStatus() | 1 : task.getStatus() & 0b1111111111111110);//第一位暂停0或启动任务1
					else
						task.setStatus(0);//第一位暂停0或启动任务1
					task.setExecutions(1);
					task.setFinishes(0);
					task.setNextdate(new java.util.Date(0));
				}
				if (sTrunkId != null) {
					task.setTrunkid(sTrunkId);
					task.trunk = (Callouttrunk) dbsession.createQuery(" from Callouttrunk where id=:id")
							.setString("id", task.getTrunkid()).uniqueResult();
				}
				if (sWorkdeptId != null) {
					task.setWorkdeptid(sWorkdeptId);
				}
				if (sBatchId != null) {
					task.setBatchid(sBatchId);
					task.batch = (Calloutbatch) dbsession.createQuery(" from Calloutbatch where id=:id")
							.setString("id", task.getBatchid()).uniqueResult();
				}
				if (sName != null)
					task.setName(sName);
				if (expiredate != null)
					task.setExpiredate(expiredate.getDate());
				if (startdate != null)
					task.setStartdate(startdate.getDate());
				if (nPeriod != null)
					task.setPeriod(nPeriod);
				if (dAgentratio != null)
					task.setAgentratio(dAgentratio);
				if (bActivate != null)
					task.setStatus(bActivate ? task.getStatus() | 1 : task.getStatus() & 0b1111111111111110);//第一位暂停0或启动任务1
				dbsession.saveOrUpdate(task);
				ts.commit();
				if((task.getStatus()&0b1) == 0b1) {
					log.info("task: "+task.getName()+", "+task.getId()+task.getId()+", status active.");
				}
				result.put("item", task);
				result.put("success", true);
			} else {
				result.put("msg", "无此记录");
			}
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示外拨任务列表", notes = "支持分页和排序")
	@POST
	@Path("/ListTask")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListTask(@Context HttpServletRequest R,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		Session dbsession = HibernateSessionFactory.getThreadSession();
		try {
			int nTotalCount = Util
					.ObjectToNumber(dbsession.createQuery("select count(*) from Callouttask").uniqueResult(), 0);
			if (nTotalCount > 0) {
				Criteria criteria = dbsession.createCriteria(CallouttaskEx.class).setFirstResult(pageStart)
						.setMaxResults(pageLimit);
				if (Sort != null) {
					if (Sort.getsDirection().equalsIgnoreCase("asc")) {
						criteria.addOrder(Order.asc(Sort.getsProperty()));
					} else {
						criteria.addOrder(Order.desc(Sort.getsProperty()));
					}
				}
				result.put("items", criteria.list());
			} else {
				result.put("items", null);
			}
			result.put("total", nTotalCount);
			result.put("success", true);
		} catch (org.hibernate.HibernateException e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		dbsession.close();
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示外拨明细", notes = "支持分页和排序")
	@POST
	@Path("/ListRecord")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListRecord(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam startDate,
			@FormParam("endtime") RESTDateParam endDate,
			@FormParam("caller") String caller,
			@FormParam("task") String task,
			@FormParam("minlength") Integer minLength,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if(startDate==null || endDate==null) {
			return Response.status(200).entity(result).build();
		}
		/* Genesys外呼表
		 if(nType!=null && nType==1) {
			//在线
			Session odbsession = OHibernateSessionFactory.getThreadSession();
			try {
				java.util.List<Calloutrecord> Calloutrecords = new java.util.ArrayList<Calloutrecord>();
				String sSqlStr = "select (case when DIAL_SCHED_TIME is null then CALL_TIME else DIAL_SCHED_TIME end) as completedate,PHONE as phone,RECORD_STATUS as completestatus,"
						+ "CALL_RESULT as result,ATTEMPT as retrycount, USERDATA01 as task from JFL_JRSYB";
				sSqlStr += " where 1=1";
				if (caller != null && caller.length() > 0) {
					sSqlStr += " and PHONE like :phone";
				}
				if (task != null && task.length() > 0) {
					sSqlStr += " and USERDATA01 like :task";
				}
				SQLQuery query = odbsession.createSQLQuery(sSqlStr);
				if (caller != null && caller.length() > 0) {
					query.setParameter("phone","%"+caller+"%");
				}
				if (task != null && task.length() > 0) {
					query.setParameter("USERDATA01","%"+task+"%");
				}
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> jrsyb_results = query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP).list();
				for(java.util.Map<String, Object> oresult : jrsyb_results) {
					Calloutrecord record = new Calloutrecord();
					int status = tab.util.Util.ObjectToNumber(oresult.get("completestatus"), 4);
					if(status==3) {
						record.setCompletestatus(1);
					}else if(status>3) {
						record.setCompletestatus(-1);
					}else {
						record.setCompletestatus(0);
					}
					record.setId(tab.util.Util.ObjectToString(oresult.get("id")));
					record.setPhone(tab.util.Util.ObjectToString(oresult.get("phone")));
					record.setCompletedate(new java.util.Date(tab.util.Util.ObjectToNumber(oresult.get("completedate"),0L)*1000));
					record.setTalklength(0);
					record.setResults(tab.util.Util.ObjectToString(oresult.get("task")));//临时传递任务名, 作为IVR的操作码
					record.setRetrycount(tab.util.Util.ObjectToNumber(oresult.get("retrycount"),1));
					record.setStatus(1);
					record.setTaskid(tab.util.Util.ObjectToString(oresult.get("result")));
					record.setTaskorder(0);
					record.setExecutions(0);
					Calloutrecords.add(record);
				}
				
				sSqlStr = "select (case when DIAL_SCHED_TIME is null then CALL_TIME else DIAL_SCHED_TIME end) as completedate,PHONE as phone,RECORD_STATUS as completestatus,"
						+ "CALL_RESULT as result,ATTEMPT as retrycount, USERDATA01 as task from JFL_CRZX";
				sSqlStr += " where 1=1";
				if (caller != null && caller.length() > 0) {
					sSqlStr += " and PHONE like :phone";
				}
				if (task != null && task.length() > 0) {
					sSqlStr += " and USERDATA01 like :task";
				}
				query = odbsession.createSQLQuery(sSqlStr);
				if (caller != null && caller.length() > 0) {
					query.setParameter("phone","%"+caller+"%");
				}
				if (task != null && task.length() > 0) {
					query.setParameter("USERDATA01","%"+task+"%");
				}
				@SuppressWarnings("unchecked")
				java.util.List<java.util.Map<String, Object>> crzx_results = query.setResultTransformer(OffersetTransformers.ALIAS_TO_ENTITY_MAP).list();
				for(java.util.Map<String, Object> oresult : crzx_results) {
					Calloutrecord record = new Calloutrecord();
					int status = tab.util.Util.ObjectToNumber(oresult.get("completestatus"), 4);
					if(status==3) {
						record.setCompletestatus(1);
					}else if(status>3) {
						record.setCompletestatus(-1);
					}else {
						record.setCompletestatus(0);
					}
					record.setId(tab.util.Util.ObjectToString(oresult.get("id")));
					record.setPhone(tab.util.Util.ObjectToString(oresult.get("phone")));
					record.setCompletedate(new java.util.Date(tab.util.Util.ObjectToNumber(oresult.get("completedate"),0L)*1000));
					record.setTalklength(0);
					record.setResults(tab.util.Util.ObjectToString(oresult.get("task")));//临时传递任务名, 作为IVR的操作码
					record.setRetrycount(tab.util.Util.ObjectToNumber(oresult.get("retrycount"),1));
					record.setStatus(1);
					record.setTaskid(tab.util.Util.ObjectToString(oresult.get("result")));
					record.setTaskorder(0);
					record.setExecutions(0);
					Calloutrecords.add(record);
				}
				Calloutrecords.sort(new java.util.Comparator<Calloutrecord>() {
					@Override
					public int compare(Calloutrecord o1, Calloutrecord o2) {
						return (int)(o2.getCompletedate().getTime() - o1.getCompletedate().getTime());
					}
				});
				result.put("items", Calloutrecords);
				result.put("total", Calloutrecords.size());
				result.put("success", true);
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			odbsession.close();
			return Response.status(200).entity(result).build();
		}
		*/
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				String sQuery = "select count(*) from Calloutrecord where completedate>=:start and completedate<=:end";
				if (caller != null && caller.length() > 0) {
					sQuery += " and phone like :phone";
				}
				if (minLength != null && minLength>0){
					if(minLength==1) {
						sQuery += " and taskid='15'";
					}else if(minLength==2) {
						sQuery += " and taskid='19'";
					}
				}
				if (task != null && task.length() > 0) {
					sQuery += " and results like :task";
				}
				Query query = dbsession.createQuery(sQuery).setTimestamp("start", startDate.getStartDate())
						.setTimestamp("end", endDate.getEndDate());
				if (caller != null && caller.length() > 0) {
					query.setString("phone", "%" + caller + "%");
				}
				//if (minLength != null && minLength > 0) {
				//	query.setInteger("talklength", minLength);
				//}
				if (task != null && task.length() > 0) {
					query.setString("task", "%" + task + "%");
				}
				int nTotalCount = Util.ObjectToNumber(query.uniqueResult(), 0);
				if (nTotalCount > 0) {
					Criteria criteria = dbsession.createCriteria(CalloutrecordEx.class);
					if (sScript == null || sScript.length() < 0) {
						criteria.setFirstResult(pageStart).setMaxResults(pageLimit);
					}
					criteria.add(Restrictions.between("completedate", startDate.getStartDate(), endDate.getEndDate()));
					if (caller != null && caller.length() > 0)
						criteria.add(Restrictions.like("phone", "%"+caller+"%"));
					if (minLength != null && minLength>0){
						if(minLength==1) {
							criteria.add(Restrictions.eq("taskid", "15"));
						}else if(minLength==2) {
							criteria.add(Restrictions.eq("taskid", "19"));
						}
					}
					if (task != null && task.length() > 0) {
						criteria.add(Restrictions.like("results", "%"+task+"%"));
					}
					if (Sort != null) {
						if (Sort.getsDirection().equalsIgnoreCase("asc")) {
							criteria.addOrder(Order.asc(Sort.getsProperty()));
						} else {
							criteria.addOrder(Order.desc(Sort.getsProperty()));
						}
					}
					if (sScript == null || sScript.length() == 0) {
						result.put("items", criteria.list());
						result.put("total", nTotalCount);
						result.put("success", true);
					}else {
						tab.MyExportExcelFile export = null;
						export = new tab.MyExportExcelFile();
						if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
								+ "WebRoot" + sScript)) {
							dbsession.close();
							return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
						}
						@SuppressWarnings("unchecked")
						java.util.List<Calloutrecord> CallRecordList = criteria.list();
						for (int i = 0; i < CallRecordList.size(); i++) {
							Calloutrecord call = CallRecordList.get(i);
							if (sScript == null || sScript.length() == 0) {
							} else {
								export.CommitRow(call);
							}
						}
						String sFileName = export.GetFile();
						if (sFileName != null && sFileName.length() > 0) {
							java.io.File f = new java.io.File(sFileName);
							int pos = sFileName.lastIndexOf(".");
							dbsession.close();
							return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
									.header("Content-Disposition",
											"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
													+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
															.format(Calendar.getInstance().getTime())
															.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
													+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
									.build();
						}
					}
				} else {
					if (sScript == null || sScript.length() == 0) {
						result.put("items", null);
						result.put("total", 0);
						result.put("success", true);
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}

	//租赁
	@POST
	@Path("/ListSummaryRecord")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListSummaryRecord(@Context HttpServletRequest R,
			@FormParam("starttime") RESTDateParam startDate,
			@FormParam("endtime") RESTDateParam endDate,
			@FormParam("type") Integer type,
			@FormParam("task") String task,
			@FormParam("minlength") Integer minLength,
			@FormParam("sort") EXTJSSortParam Sort,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				String sQuery = " from Calloutrecord where completedate between :start and :end";
	
				if (minLength != null && minLength>0){
					if(minLength==1) {
						sQuery += " and taskid='15'";
					}else if(minLength==2) {
						sQuery += " and taskid='19'";
					}
				}
				if (task != null && task.length() > 0) {
					sQuery += " and results like :name";
				}
				String sDateColumn = " taskid as taskid,year(completedate) as nYear";
				if (type == 0) {// hour
					sQuery += " group by taskid,year(completedate), month(completedate),day(completedate),hour(completedate)";
					sDateColumn = " taskid as taskid,year(completedate) as nYear, month(completedate) as nMonth,day(completedate) as nDay,hour(completedate) as nHour";
				} else if (type == 1) {// day
					sQuery += " group by taskid,year(completedate), month(completedate),day(completedate)";
					sDateColumn = " taskid as taskid,year(completedate) as nYear, month(completedate) as nMonth,day(completedate) as nDay";
				} else if (type == 3) {// month
					sQuery += " group by taskid,year(completedate), month(completedate)";
					sDateColumn = " taskid as taskid, year(completedate) as nYear, month(completedate) as nMonth";
				}
	
				if (Sort != null) {
					sQuery += String.format(" order by %s %s", Sort.getsProperty(), Sort.getsDirection());
				}
				sQuery = String.format("select new Map(" + type + " as type, taskid as taskid,sum(case when completestatus>0 then 1 else 0 end) as complete,"
						+ " sum(case when completestatus<0 then 1 else 0 end) as failed, sum(case when completestatus=0 then 1 else 0 end) as processing,"
						+ " sum(case when status=21 or status=33 or status=26 then 1 else 0 end) as talklength, sum(retrycount) as retrycount, %s)", sDateColumn) + sQuery;
				log.info(sQuery);
				org.hibernate.Query query = dbsession.createQuery(sQuery).setTimestamp("start", startDate.getStartDate())
						.setTimestamp("end", endDate.getEndDate());
	
				if (task != null && task.length() > 0) {
					query.setString("name", "%" + task + "%");
				}
				if (sScript == null || sScript.length() == 0) {
					result.put("items", query.list());
					result.put("success", true);
				}else {
					tab.MyExportExcelFile export = null;
					export = new tab.MyExportExcelFile();
					if (!export.Init(System.getProperty("tab.path") + System.getProperty("file.separator")
							+ "WebRoot" + sScript)) {
						dbsession.close();
						return Response.status(404).entity("FILE NOT FOUND:" + sScript).type("text/plain").build();
					}
					@SuppressWarnings("unchecked")
					java.util.List<java.util.Map<String, Object>> CallRecordList = query.list();
					for (int i = 0; i < CallRecordList.size(); i++) {
						java.util.Map<String, Object> call = CallRecordList.get(i);
						if (sScript == null || sScript.length() == 0) {
						} else {
							export.CommitRow(call);
						}
					}
					String sFileName = export.GetFile();
					if (sFileName != null && sFileName.length() > 0) {
						java.io.File f = new java.io.File(sFileName);
						int pos = sFileName.lastIndexOf(".");
						dbsession.close();
						return Response.ok(f).header(HttpHeaders.CONTENT_TYPE, "application/csv;charset=UTF-8")
								.header("Content-Disposition",
										"attachment;filename=\"" + URLEncoder.encode(export.GetFileName(), "utf-8")
												+ (new SimpleDateFormat("(yyyy-MM-dd HH:mm:ss)"))
														.format(Calendar.getInstance().getTime())
														.replaceAll("\\*|/|\\?|:|>|<|\\\\|\"|\\|", "")
												+ ((pos > 0) ? sFileName.substring(pos) : "") + "\"")
								.build();
					}
				}
			} catch (org.hibernate.HibernateException e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			} catch (Throwable e) {
				log.warn("ERROR:", e);
				result.put("msg", e.toString());
			}
			dbsession.close();
		} catch (Throwable e) {
			log.warn("ERROR:", e);
			result.put("msg", e.toString());
		}
		if (sScript == null || sScript.length() == 0) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(404).entity("no datas").type("text/plain").build();
	}
	@POST
	@Path("/ImportCalloutRecord")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ImportCalloutRecord(@Context HttpServletRequest R,@FormParam("batchName") String taskid,@FormParam("phoneNumber") String phoneNumber) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	//API文档: (value = "首页面板查询任务数", notes = "仅统计任务最后执行任务Executions里的电话数据")
	@POST
	@Path("/DashboardTaskInfo")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DashboardTaskInfo(@Context HttpServletRequest R,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		if (pageStart == null || pageLimit == null) {
			pageStart = 0;
			pageLimit = 1;
		}
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "删除坐席分组", notes = "")
	@POST
	@Path("/RemoveAgentQueue")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response RemoveAgentQueue(@Context HttpServletRequest R,
			@FormParam("id") Integer nId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "更新或添加坐席分组", notes = "")
	@POST
	@Path("/AddOrUpdateAgentQueue")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddOrUpdateAgentQueue(@Context HttpServletRequest R,
			@FormParam("id") Integer nId,
			@FormParam("field") String sField,
			@FormParam("value") String sValue,
			@FormParam("values") List<String> Values,
			@FormParam("role") String sRoleId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示队列列表", notes = "")
	@POST
	@Path("/ListQueues")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListQueues(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示分组列表", notes = "")
	@POST
	@Path("/ListRoles")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListRoles(@Context HttpServletRequest R) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示坐席分组列表", notes = "支持分页和排序")
	@POST
	@Path("/ListAgentQueue")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListAgentQueue(@Context HttpServletRequest R,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示坐席信息", notes = "")
	@POST
	@Path("/GetAgentInfo")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public String GetAgentInfo(@Context HttpServletRequest R) {
		return "";
	}

	//API文档: (value = "删除短信模板", notes = "")
	@POST
	@Path("/RemoveSmsTemplate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response RemoveSmsTemplate(@Context HttpServletRequest R,
			@FormParam("templatecode") String sTemplateCode,
			@FormParam("sync") Boolean bSync) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "更新或添加短信模板", notes = "")
	@POST
	@Path("/AddOrUpdateSmsTemplate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddOrUpdateSmsTemplate(@Context HttpServletRequest R,
			@FormParam("templatecode") String sTemplateCode,
			@FormParam("name") String sName,
			@FormParam("type") String sType,
			@FormParam("remark") String sRemark,
			@FormParam("value") String sValue) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示短信模板列表", notes = "支持分页和排序")
	@POST
	@Path("/ListSmsTemplate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListSmsTemplate(@Context HttpServletRequest R,
			@FormParam("sync") Boolean bSync,
			@FormParam("approval") Boolean bApproval,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示短信模板列表", notes = "支持分页和排序")
	@POST
	@Path("/GetSmsTemplate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetSmsTemplate(@Context HttpServletRequest R,
			@FormParam("templatecode") String sTemplateCode) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示短信模板列表", notes = "支持分页和排序")
	@POST
	@Path("/PreviewSmsTemplate")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response PreviewSmsTemplate(@Context HttpServletRequest R,
			@FormParam("templatecode") String sTemplateCode) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "删除短信签名", notes = "")
	@POST
	@Path("/RemoveSmsSign")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response RemoveSmsSign(@Context HttpServletRequest R,
			@FormParam("signname") String sSmsSign,
			@FormParam("sync") Boolean bSync) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "更新或添加短信签名", notes = "")
	@POST
	@Path("/AddOrUpdateSmsSign")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response AddOrUpdateSmsSign(@Context HttpServletRequest R,
			@FormParam("signname") String sSmsSignName) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "显示短信签名列表", notes = "支持分页和排序")
	@POST
	@Path("/ListSmsSign")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListSmsSign(@Context HttpServletRequest R,
			@FormParam("sync") Boolean bSync,
			@FormParam("approval") Boolean bApproval,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "发送短信", notes = "")
	@POST
	@Path("/SendSms")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response SendSms(@Context HttpServletRequest R,
			@FormParam("mobile") String sMobile,
			@FormParam("templatecode") String sTemplateCode,
			@FormParam("signname") String sSignName,
			@FormParam("agent") String sAgent,
			@FormParam("values") String sValues) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	//API文档: (value = "查询短信记录", notes = "支持分页和排序")
	@POST
	@Path("/ReportSms")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportSms(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("agent") String sAgent,
			@FormParam("mobile") String sMobile, @FormParam("groupguid") String sGroupguid,
			@FormParam("script") String sScript,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		if (pageStart == null)
			pageStart = 0;
		if (pageLimit == null)
			pageLimit = 1000;
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/ReportSmsSummary")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ReportSmsSummary(@Context HttpServletRequest R, @FormParam("starttime") RESTDateParam dtStarttime,
			@FormParam("endtime") RESTDateParam dtEndtime, @FormParam("type") Integer nType,
			@FormParam("script") String sScript) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}

	@POST
	@Path("/Dial")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response Dial(@Context HttpServletRequest R, @DefaultValue("") @QueryParam("context") String sContext,@QueryParam("crmkey") String sCrmKey,
			@QueryParam("appid") String sAppId, @QueryParam("secret") String sSecret, @DefaultValue("") @QueryParam("from") String sAgent,@QueryParam("exten") String sExten,
			@QueryParam("to") String sNumber) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/DialEx")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response DialEx(@Context HttpServletRequest R, @DefaultValue("") @QueryParam("context") String sContext,@QueryParam("crmkey") String sCrmKey,
			@QueryParam("appid") String sAppId, @QueryParam("secret") String sSecret, @DefaultValue("") @QueryParam("from") String sAgent,@DefaultValue("") @QueryParam("exten") String sExten,
			@QueryParam("to") String sNumber,@DefaultValue("") @QueryParam("customerId") String sCustomerId,@DefaultValue("") @QueryParam("customerid") String sCustomerid,@DefaultValue("") @QueryParam("agent") String sUsername) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/Logout")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response Logout(@Context HttpServletRequest R, @QueryParam("crmkey") String sCrmKey,
			@QueryParam("appid") String sAppId, @QueryParam("secret") String sSecret, @DefaultValue("") @QueryParam("from") String sAgent,@QueryParam("exten") String sExten) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	
	//API文档: (value="重置密码", notes = "重置当前选择坐席用户的密码")
	@POST
    @Path("/UIResetPassword")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UIResetPassword(@Context HttpServletRequest R,
    		@FormParam("id") Integer nId)
    {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
    }
	@POST
    @Path("/UpdateFreeswitchFifo")
	@Consumes("application/x-www-form-urlencoded")
    @Produces("application/json" + ";charset=utf-8")
    public Response UpdateFreeswitchFifo(@Context HttpServletRequest R,@DefaultValue("") @FormParam("Agent")String sAgent,
    		@DefaultValue("") @FormParam("Extension")String sExtension,@DefaultValue("") @FormParam("Groups")String sGroups,
    		@FormParam("Ready")Boolean bReady,@FormParam("Registed")Boolean bRegisted)
    {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
    }

	public static boolean getCurrentWeekHolidayPhone(Session dbsession, String sDept,LocalDateTime dtNow, int nTime, tab.configServer.ValueString phone) {
		int nWeek = dtNow.getDayOfWeek().getValue();//sunday=7
		Callholidays holiday = null;
		String sSqlStr = " from Callholidays where id=:week";
		if(sDept.length()>0) {
			sSqlStr += " and dept=:dept";
			holiday = (Callholidays)dbsession.createQuery(sSqlStr).setInteger("week", nWeek).setString("dept", sDept).setFirstResult(0).setMaxResults(1).uniqueResult();
		}else {
			sSqlStr += " and (dept is null or dept='')";
			holiday = (Callholidays)dbsession.createQuery(sSqlStr).setInteger("week", nWeek).setFirstResult(0).setMaxResults(1).uniqueResult();
		}
		if(holiday==null) {
			//没记录，默认上班
			phone.value = StringUtils.EMPTY;
			return true;
		}
		phone.value = Util.ObjectToString(holiday.getPhone());
		LocalDateTime starttime = holiday.getStarttime()==null ? dtNow : holiday.getStarttime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime endtime = holiday.getEndtime()==null ? dtNow.plusSeconds(1) : holiday.getEndtime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
		boolean bActivate = false;
		if(nTime>=starttime.getSecond()+starttime.getMinute()*100+starttime.getHour()*10000 && nTime<endtime.getSecond()+endtime.getMinute()*100+endtime.getHour()*10000) {
			if(holiday.getActivate()!=0) {
				//有上班时间段
				bActivate = true;
			}
		}else {
			if(holiday.getActivate()==0) {
				//不在下班范围内的则为上班时间段
				bActivate = true;
			}
		}
		//再查找是否有分段时间
		sSqlStr = " from Callholidays where id=:week and (second(starttime)+minute(starttime)*100+hour(starttime)*10000)<=:time and (second(endtime)+minute(endtime)*100+hour(endtime)*10000)>:time";//id为星期加10
		if(sDept.length()>0) {
			sSqlStr += " and dept=:dept";
			holiday = (Callholidays)dbsession.createQuery(sSqlStr).setInteger("week", nWeek+10).setString("dept", sDept).setInteger("time", nTime).setFirstResult(0).setMaxResults(1).uniqueResult();
		}else {
			sSqlStr += " and (dept is null or dept='')";
			holiday = (Callholidays)dbsession.createQuery(sSqlStr).setInteger("week", nWeek+10).setInteger("time", nTime).setFirstResult(0).setMaxResults(1).uniqueResult();
		}
		if(holiday==null) {
			//没分段记录, 返回上下班状态和值班电话
			return bActivate;
		}
		//找到分段记录, 如果分段记录里是下班是不反回值班电话的。
		//如果需要返回值班电话，则分段里仅设置上班时间，配合上层的值班电话（每周）里设置上班时间，实现多段时间上班即可
		if(bActivate || holiday.getActivate()==0) {
			phone.value = StringUtils.EMPTY;
			return false;
		}
		return true;
	}

	@GET
	@Path("/CheckHolidays")
	@Produces("application/json" +";charset=utf-8")
	public Response CheckHolidays(@Context HttpServletRequest R,@QueryParam("dept") @DefaultValue("") String sDept,
			@QueryParam("time") @DefaultValue("") RESTDateParam time) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", true);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			CheckHolidays(dbsession , sDept, (time!=null?time.getDate():null), result);
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		return Response.status(200).entity(result).build();
	}
	
	public static boolean CheckHolidays(Session dbsession ,String sDept,java.util.Date time, java.util.Map<String, Object> result) {
		tab.configServer.ValueString phone = new tab.configServer.ValueString(StringUtils.EMPTY);
			try {
				LocalDateTime dtNow = time!=null ? time.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : LocalDateTime.now();
				//LocalDateTime当前时间转Date，线程安全
				//java.util.Date now = Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());
				//Date转LocalDateTime
				//LocalDateTime localDateTime = (new java.util.Date()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				//指定时间
				//dtNow = LocalDateTime.of(2020, 7,27,6,30,0);
				//加减时间
				//dtNow = dtNow.plusHours(3);
				result.put("now", dtNow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
				
				int nTime = dtNow.getSecond() + dtNow.getMinute()*100 + dtNow.getHour()*10000;
				int nDay = dtNow.getDayOfMonth()+ dtNow.getMonthValue()*100 + dtNow.getYear()*10000;
				//判断部门是否使用默认号码
				if(tab.util.Util.ObjectToNumber(dbsession.createQuery("select count(*) from Callholidays where dept=:dept").setString("dept", sDept).uniqueResult(),0)==0) {
					sDept = StringUtils.EMPTY;
				}
				//节假日判断，如果starttime is null or endtime is null 则表示全天工作
				String sSqlStr = " from Callholidays where id>=100 and ( "
						+ "(DAY(startdate)+MONTH(startdate)*100+YEAR(startdate)*10000)=:nday"
						+ " and (starttime is null or endtime is null or ((second(starttime)+minute(starttime)*100+hour(starttime)*10000)<=:time and (second(endtime)+minute(endtime)*100+hour(endtime)*10000)>:time)) )";
				if(sDept.length()>0) {
					sSqlStr += " and dept=:dept";
				}else {
					sSqlStr += " and (dept is null or dept='')";
				}
				sSqlStr += " order by activate";
				Callholidays holiday = null;
				if(sDept.length()>0) {
					holiday = (Callholidays)dbsession.createQuery(sSqlStr).setInteger("nday", nDay).setString("dept", sDept)
						.setInteger("time", nTime).setFirstResult(0).setMaxResults(1).uniqueResult();
				}else {
					holiday = (Callholidays)dbsession.createQuery(sSqlStr).setInteger("nday", nDay)
							.setInteger("time", nTime).setFirstResult(0).setMaxResults(1).uniqueResult();
				}
				if(holiday!=null) {
					//找到节假日设置记录
					result.put("phone", Util.ObjectToString(holiday.getPhone()));
					if(holiday.getStarttime()!=null && holiday.getEndtime()!=null) {
						//设置了该节假日时间范围
						if(holiday.getActivate()==0) {
							result.put("success", false);
							return false;
						}
						//上班
						result.put("success", true);
						return true;
					}
					//没设置时间范围
					if(holiday.getActivate()==0) {
						//休假无值班电话, 则查找当前星期的值班电话
						result.put("success", false);
						return false;
					}
					//没设置时间范围，并且设置为上班，则查询当前星期的上下班和值班设置
					if(getCurrentWeekHolidayPhone(dbsession,sDept,dtNow,nTime,phone)) {
						if(StringUtils.isBlank(holiday.getPhone())){
							result.put("phone", phone.value);
						}
						result.put("success", true);
						return true;
					}
					if(StringUtils.isBlank(holiday.getPhone())){
						result.put("phone", phone.value);
					}
					result.put("success", false);
					return false;
				}
				//没有特定节假日设置，则查询当前星期的设置
				if(getCurrentWeekHolidayPhone(dbsession,sDept,dtNow,nTime,phone)) {
					result.put("phone", phone.value);
					result.put("success", true);
					return true;
				}
				result.put("phone", phone.value);
				result.put("success", false);
				return false;
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			return tab.util.Util.ObjectToBoolean(result.get("success"));
	}
	
	@SuppressWarnings("unchecked")
	@POST
	@Path("/ListHolidaysWeek")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" +";charset=utf-8")
	public Response ListHolidaysWeek(@Context HttpServletRequest R,@FormParam("dept") @DefaultValue("") String sDept) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		java.util.Map<String, String> weeks = new java.util.HashMap<String, String>();
		java.util.List<Callholidays> phones = new java.util.ArrayList<Callholidays>();
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				java.util.List<Callholidays> holidays = null;
				if(sDept.length()>0) {
					holidays = dbsession.createQuery(" from Callholidays where id in(1,2,3,4,5,6,7) and dept=:dept order by id asc").setString("dept",sDept).list();
				}else {
					holidays = dbsession.createQuery(" from Callholidays where id in(1,2,3,4,5,6,7) and (dept is null or dept='') order by id asc").list();
				}
				for(Callholidays holiday: holidays) {
					if(holiday.getActivate()!=0) {
						weeks.put("week"+holiday.getId(), "on");
					}
					phones.add(holiday); 
				}
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
			result.put("success", true);
			result.put("weeks", weeks);
			result.put("phones", phones);
			result.put("total", phones.size());
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		return Response.status(200).entity(result).build();
	}
	
	@SuppressWarnings("unchecked")
	@POST
	@Path("/UpdateHolidaysWeek")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" +";charset=utf-8")
	public Response UpdateHolidaysWeek(@Context HttpServletRequest R,
			@FormParam("weeks") RESTMapParam weeks,@FormParam("dept") @DefaultValue("") String sDept) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		java.util.Map<String, String> weekValues = (java.util.Map<String, String>)weeks.getData(); 
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					java.util.Date dtNow = Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());
					java.util.List<Callholidays> holidays = null;
					if(sDept.length()>0) {
						holidays = dbsession.createQuery(" from Callholidays where id in(1,2,3,4,5,6,7) and dept=:dept order by id").setString("dept",sDept).list();
					}else {
						holidays = dbsession.createQuery(" from Callholidays where id in(1,2,3,4,5,6,7) and (dept is null or dept='') order by id").list();
					}
					boolean bChange = false;
					if(holidays.size()!=7) {
						java.util.List<Integer> tmp = Arrays.asList(1,2,3,4,5,6,7);
						for(int i=0;i<holidays.size();i++) {
							tmp.remove(holidays.get(i).getId());
						}
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						for(Integer j:tmp) {
							Callholidays holiday = new Callholidays(UUID.randomUUID().toString().toUpperCase(),j,"星期",j>5?0:1,
									Date.from(LocalDateTime.now().plusDays(1).atZone(java.time.ZoneId.systemDefault()).toInstant()),
									sdf.parse("2020-01-01 09:00:00"),sdf.parse("2020-01-01 18:00:00"),
									dtNow,dtNow,sDept==null?StringUtils.EMPTY:sDept,StringUtils.EMPTY);
							dbsession.save(holiday);
							bChange = true;
						}
					}
					if(bChange) {
						ts.commit();
						ts = dbsession.beginTransaction();
						if(sDept.length()>0) {
							holidays = dbsession.createQuery(" from Callholidays where id in(1,2,3,4,5,6,7) and dept=:dept order by id").setString("dept",sDept).list();
						}else {
							holidays = dbsession.createQuery(" from Callholidays where id in(1,2,3,4,5,6,7) and (dept is null or dept='') order by id").list();
						}
					}
					bChange = false;
					for(Callholidays holiday: holidays) {
						if(weekValues.containsKey("week"+holiday.getId())) {
							if(holiday.getActivate()!=1) {
								holiday.setActivate(1);
								holiday.setUpdatedate(dtNow);
								dbsession.update(holiday);
								bChange = true;
							}
						}else {
							if(holiday.getActivate()!=0) {
								holiday.setActivate(0);
								holiday.setUpdatedate(dtNow);
								dbsession.update(holiday);
								bChange = true;
							}
						}
					}
					if(bChange) {
						ts.commit();
					}else {
						ts.rollback();
					}
					result.put("success", true);
				} catch (org.hibernate.HibernateException e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);
					ts.rollback();
				} catch(Throwable e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);	
					ts.rollback();
				}
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/ListHolidays")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" +";charset=utf-8")
	public Response ListHolidays(@Context HttpServletRequest R,
			@DefaultValue("false") @FormParam("onlytime") Boolean bOnlyTime,
			@FormParam("week") Integer nId ,
			@FormParam("dept") @DefaultValue("") String sDept,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				int nTotalCount = 0;
				if(bOnlyTime) {
					if(sDept.length()>0) {
						nTotalCount = Util.ObjectToNumber(dbsession.createQuery("select count(*)  from Callholidays where id=:id and dept=:dept").setString("dept", sDept).setInteger("id", nId+10).uniqueResult(), 0);
					}else {
						nTotalCount = Util.ObjectToNumber(dbsession.createQuery("select count(*)  from Callholidays where id=:id and (dept is null or dept='')").setInteger("id", nId+10).uniqueResult(), 0);
					}
				}else {
					nTotalCount = Util.ObjectToNumber(dbsession.createQuery("select count(*)  from Callholidays where id>=100").uniqueResult(), 0);
				}
				if (nTotalCount > 0) {
					Criteria criteria = dbsession.createCriteria(Callholidays.class)
							.setFirstResult(pageStart)
							.setMaxResults(pageLimit);
					if(bOnlyTime) {
						if(sDept.length()>0) {
							criteria.add(Restrictions.and(Restrictions.eq("dept", sDept),Restrictions.eq("id", nId+10)));
						}else {
							criteria.add(Restrictions.and(Restrictions.or(Restrictions.isNull("dept"),Restrictions.eq("dept","")),Restrictions.eq("id", nId+10)));
						}
						criteria.addOrder(Order.asc("id"));
					}else {
						criteria.add(Restrictions.ge("id", 100));
						if (Sort != null) {
							if (Sort.getsDirection().equalsIgnoreCase("asc")) {
								criteria.addOrder(Order.asc(Sort.getsProperty()));
							} else {
								criteria.addOrder(Order.desc(Sort.getsProperty()));
							}
						}
					}
					result.put("items", criteria.list());
				} else {
					result.put("items", null);						
				}
				result.put("total", nTotalCount);
				result.put("success", true);
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		return Response.status(200).entity(result).build();
	}
	
	@POST
	@Path("/AddOrUpdateHolidays")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" +";charset=utf-8")
	public Response AddOrUpdateHolidays(@Context HttpServletRequest R,
			@DefaultValue("false") @FormParam("onlytime")/*仅配置默认时间*/ Boolean bOnlyTime,
			@DefaultValue("false") @FormParam("onlyactivate") /*仅配置激活状态*/Boolean bOnlyActivate,
			@FormParam("week") Integer nId,
			@FormParam("dept") String sDept,
			@FormParam("phone") String sPhone,
			@FormParam("callholidayid") @DefaultValue("")  String sCallholidayId,
			@FormParam("name") String sName,
			@FormParam("startdate") RESTDateParam startdate,
			@FormParam("starttime") RESTDateParam starttime,
			@FormParam("endtime") RESTDateParam endtime,
			@FormParam("activate") /*是否工作日*/Boolean bActivate
			) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					java.util.Date dtNow = Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant());
					if(sCallholidayId.length()>0) {
						Callholidays holiday = (Callholidays) dbsession.createCriteria(Callholidays.class).add(Restrictions.eq("callholidayid", sCallholidayId)).uniqueResult();
						if(holiday!=null) {
							if(bActivate!=null)holiday.setActivate(bActivate?1:0);
							if(!bOnlyActivate) {
								if(sName!=null)holiday.setName(sName);
								if(startdate!=null)holiday.setStartdate(startdate.getDate());
								if(starttime!=null) {
									holiday.setStarttime(starttime.getDate());
								}
								else {
									holiday.setStarttime(null);
								}
								if(endtime!=null) {
									holiday.setEndtime(endtime.getDate());
								}else {
									holiday.setEndtime(null);
								}
								if(sDept!=null) {
									holiday.setDept(sDept);
								}
								if(sPhone!=null) {
									holiday.setPhone(sPhone);
								}
							}
							holiday.setUpdatedate(dtNow);
							dbsession.update(holiday);
							ts.commit();
							result.put("item", holiday);
							result.put("success", true);
						}
					}else if(bOnlyTime){
						Callholidays holiday = new Callholidays(UUID.randomUUID().toString().toUpperCase(),nId+10,"NewTime",0,
								Date.from(LocalDateTime.now().plusDays(1).atZone(java.time.ZoneId.systemDefault()).toInstant()),
								dtNow,dtNow,sDept==null?StringUtils.EMPTY:sDept,sPhone==null?StringUtils.EMPTY:sPhone);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						holiday.setStarttime(sdf.parse("2020-01-01 12:00:00"));
						holiday.setEndtime(sdf.parse("2020-01-01 13:00:00"));
						dbsession.save(holiday);
						result.put("item", holiday);
						ts.commit();
						result.put("success", true);
					}else {
						Callholidays holiday = new Callholidays(UUID.randomUUID().toString().toUpperCase(),100,"NewDate",1,
								Date.from(LocalDateTime.now().plusDays(1).atZone(java.time.ZoneId.systemDefault()).toInstant()),
								dtNow,dtNow,sDept==null?StringUtils.EMPTY:sDept,sPhone==null?StringUtils.EMPTY:sPhone);
						holiday.setStarttime(null);
						holiday.setEndtime(null);
						dbsession.save(holiday);
						result.put("item", holiday);
						ts.commit();
						result.put("success", true);
					}
				} catch (org.hibernate.HibernateException e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);
					ts.rollback();
				} catch(Throwable e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);	
					ts.rollback();
				}
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		if((boolean) result.get("success")) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(500).entity(result).build();
	}
	
	@POST
	@Path("/RemoveHolidays")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" +";charset=utf-8")
	public Response RemoveHolidays(@Context HttpServletRequest R,
			@FormParam("callholidayid") @DefaultValue("") String sHolidayId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					if(sHolidayId.length()>0) {
						Callholidays holiday = (Callholidays) dbsession.createCriteria(Callholidays.class).add(Restrictions.eq("callholidayid", sHolidayId)).uniqueResult();
						if(holiday!=null) {
							dbsession.delete(holiday);
							ts.commit();
							result.put("success", true);
						}
					}
				} catch (org.hibernate.HibernateException e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);
					ts.rollback();
				} catch(Throwable e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);	
					ts.rollback();
				}
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		if((boolean) result.get("success")) {
			return Response.status(200).entity(result).build();
		}
		return Response.status(500).entity(result).build();
	}

	@POST
	@Path("/UploadBusinessType")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public static boolean UploadBusinessType(@Context HttpServletRequest R,
			@FormParam("id") String sId,
			@FormParam("activate") Boolean bActivate,
			@FormParam("name") String sName,
			@FormParam("description") String sDesc,
			@FormParam("fid") String sFId,
			@FormParam("row") Integer nRow,
			@FormParam("col") Integer nCol,
			@DefaultValue("0")@FormParam("customerType")Integer customerType) {
		switch (customerType) {
		case 1:
			break;
		default:
			break;
		}
		return false;
	}
	
	@POST
	@Path("/ListCustomerPhone")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response ListCustomerPhone(@Context HttpServletRequest R,
			@FormParam("phone") String sPhone,
			@FormParam("start") Integer pageStart,
			@FormParam("limit") Integer pageLimit,
			@FormParam("sort") EXTJSSortParam Sort) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		return Response.status(200).entity(result).build();
	}
	@POST
	@Path("/CancelResult")//取消外呼请求
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response getResult(@Context HttpServletRequest R,
			@FormParam("RequestId") String RequestId) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		String SSGUrl=Runner.SSGUrl.length()>0?Runner.SSGUrl:StringUtils.EMPTY;
		HttpPost httpPost = new HttpPost(SSGUrl);
		 String sJsonRpc = String.format("<SSGRequest xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n" + 
		 		"<CancelRequest RequestID=\"%s\"/>\r\n" + 
		 		"</SSGRequest >\r\n" + 
		 		"",RequestId);
		 CloseableHttpClient client = tab.util.Util.getIdleHttpClient(SSGUrl.indexOf("https://")==0?true:false);
			if(client==null) {
				throw new NotAuthorizedException("Invalid CloseableHttpClient");
			}
			try {
				StringEntity sEntity = new StringEntity(sJsonRpc, org.apache.http.Consts.UTF_8);
		        httpPost.setEntity(sEntity);
		        httpPost.setHeader("Content-Type","application/xml");
				HttpResponse res = client.execute(httpPost);
				HttpEntity entity = res.getEntity();
				
				try {
					int nStatusCode = res.getStatusLine().getStatusCode();
					if(nStatusCode<300) {
						log.info( "nStatusCode"+nStatusCode+"==sJsonRpc"+sJsonRpc);
						String sSessionInfo = EntityUtils.toString(entity, org.apache.http.Consts.UTF_8);
						log.info("entitymessage:"+sSessionInfo);
						 try {
							 Document doc = DocumentHelper.parseText(sSessionInfo);
							 Element rootsEle = doc.getRootElement();
							 Element roots=rootsEle.element("ResponseElement");
							 if(roots!=null) {
								 System.out.print(roots.getName());
								 if(roots.attributeValue("ResponseType").equals("SUCCESS")){
									 log.info("ssg外呼请求成功"+sSessionInfo);
									 String rerequestid=roots.attributeValue("RequestID");
									 result.put("success", true);
									 result.put("requestid", rerequestid);
								 }else if(roots.attributeValue("ResponseType").equals("FAILURE")) {
									 String rerequestid=roots.attributeValue("RequestID");
									 String reseasoncode=roots.attributeValue("ReasonCode");
									 String Reason=roots.attributeValue("Reason");
									 result.put("requestid", rerequestid);
									 result.put("reseasoncode", reseasoncode); 
									 result.put("Reason", Reason);
									 log.info("ssg外呼请求失败"+sSessionInfo);
								 }
							 }else {
								log.info("请求失败"+sSessionInfo);
								result.put("message", sSessionInfo);
							 }
						      
						} catch (DocumentException e) {
							// TODO Auto-generated catch block
							log.error("ERROR:",e);
							e.printStackTrace();
						}
					}else {
						result.put("message", "Invalid StatusCode"+nStatusCode);
						log.error("Invalid StatusCode(" + nStatusCode + ") " + "sJsonRpc"+sJsonRpc);
						throw new NotAuthorizedException("Invalid StatusCode");
					}
				} catch (ParseException e) {
					log.error("ERROR:",e);
					throw new NotAuthorizedException("Invalid ParseException");
				} 
				catch (IOException e) {
					log.error("ERROR:",e);
					throw new NotAuthorizedException("Invalid IOException");
				}
			
			   }catch(IOException e){
					log.error("ERROR:",e);
	    			throw new NotAuthorizedException("Invalid IOException");
				
			   }
		
		
	if((boolean) result.get("success")) {
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					Calloutrecord record = (Calloutrecord) dbsession.createCriteria(Calloutrecord.class).add(Restrictions.eq("calluuid", RequestId)).uniqueResult();
					if(record!=null) {
						record.setResults("外呼请求已取消");;//已取消
					}
				} catch (org.hibernate.HibernateException e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);
					ts.rollback();
				} catch(Throwable e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);	
					ts.rollback();
				}
				ts.commit();
			} catch (org.hibernate.HibernateException e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			} catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
	}
		return Response.status(200).entity(result).build();
	}
	
	@GET//外呼结果通知接口
	@Path("/GetNotifition")
	@Consumes("application/x-www-form-urlencoded")
	@Produces("application/json" + ";charset=utf-8")
	public Response GetNotifition(@Context HttpServletRequest R,
			@QueryParam("Token") String Token,
			@QueryParam("RequestID") String RequestID,
			@QueryParam("TenantName") String TenantName,
			@QueryParam("IVRProfileName") String IVRProfileName,
			@QueryParam("Telnum") String Telnum,
			@QueryParam("AttemptsMade") Integer AttemptsMade,
			@QueryParam("MaxAttempts") Integer MaxAttempts,
			@QueryParam("TimeToLive") String TimeToLive,
			@QueryParam("TTLRemaining") String TTLRemaining,
			@QueryParam("CallUUID") String CallUUID,
			@QueryParam("Result") String Result,
			@QueryParam("Status") String Status) {
		java.util.Map<String, Object> result = new java.util.HashMap<String, Object>();
		result.put("success", false);
		HttpSession httpsession = R.getSession();
		if (httpsession == null) {
			log.error("ERROR: getSession() is null");
			return Response.status(200).entity(result).build();
		}
		try {
			Session dbsession = HibernateSessionFactory.getThreadSession();
			try {
				Transaction ts = dbsession.beginTransaction();
				try {
					if(Token!=null&&Token.length()>0) {
						CalloutrecordEx recordex = (CalloutrecordEx) dbsession.createCriteria(CalloutrecordEx.class).add(Restrictions.eq("id", Token)).uniqueResult();
						if(recordex!=null) {
							recordex.setRetrycount(AttemptsMade);//尝试次数
							recordex.setResults(Status);//呼叫状态
							LocalDateTime dtNow = LocalDateTime.now();
							 java.util.Date now = Date.from(dtNow.atZone(java.time.ZoneId.systemDefault()).toInstant());
							if(Result.equals("SUCCESS")) {
								recordex.setCompletedate(now);
								recordex.setCompletestatus(CallTask.STATUS_COMPLETE);//最终成功呼出
								log.info("GetNotifition:result=SUCCESS,desc="+result);
							}else {
								recordex.setCompletedate(now);
								recordex.setCompletestatus(CallTask.STATUS_FAILED);//最终失败呼出
								log.info("GetNotifition:result=FAILUREdesc="+result);
							}
							CompletableFuture<String> outbound = GenesysSSGService.outboundQueue.get(Token);
							if (outbound != null) {
								outbound.complete(Result);
							}
						}
						dbsession.save(recordex);
					}
				} catch (org.hibernate.HibernateException e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);
					ts.rollback();
				} catch(Throwable e) {
					result.put("msg", e.toString());
					log.error("ERROR:", e);	
					ts.rollback();
				}
				ts.commit();
			}catch (Throwable e) {
				result.put("msg", e.toString());
				log.error("ERROR:", e);
			}
			dbsession.close();
		} catch (org.hibernate.HibernateException e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		} catch(Throwable e) {
			result.put("msg", e.toString());
			log.error("ERROR:",e);
		}
		
		return Response.status(200).entity(result).build();
	}
	
}
