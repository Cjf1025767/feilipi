package tab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.api.scripting.ScriptUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @导出报表
 */
public class MyExportExcelFile {
	private static Log logger = LogFactory.getLog(MyExportExcelFile.class);
	private ThreadCommitCsv _ThreadCommitCsv = new ThreadCommitCsv();
	private boolean bFormatCsv = true;
	protected ScriptEngine _JsEngine = null;
	protected Object _JsExportObject = null;
	protected java.util.List<String> _FiledNames = new java.util.ArrayList<String>();
	protected java.util.List<Object> _FiledValues = new java.util.ArrayList<Object>();
	protected java.util.HashMap<String, Boolean> _FiledUsings = new java.util.HashMap<String, Boolean>();
	protected java.util.List<String> _FooterNames = new java.util.ArrayList<String>();
	protected java.util.List<String> _FooterValues = new java.util.ArrayList<String>();
	boolean _bFoooterComplete = false;
	protected String _sReportName = new String("ReportName");
	protected String _sHeader = new String();
	protected java.io.PrintWriter _CsvPrintWriter = null;
	protected String _sTempFilePathName = null;
	protected boolean _bNoParseValuesMethod = false;
	public static String MyWebRoot = "/WebRoot/";
	
	public MyExportExcelFile() {

	}

	class ThreadCommitCsv implements Runnable {

		private boolean _bSuccess = true;
		private boolean _bCommiting = true;
		private Thread _hThread = null;
		LinkedBlockingQueue<java.util.List<Object>> _Queue = new LinkedBlockingQueue<java.util.List<Object>>();

		public ThreadCommitCsv() {
		}

		protected void finalize() {
			try {
				_bCommiting = false;
				super.finalize();
			} catch (Throwable e) {
			}
		}

		void Start() {
			if (_hThread == null) {
				_hThread = new Thread(this);
				_hThread.setPriority(Thread.MAX_PRIORITY);
				_hThread.start();
			}
		}

		boolean Commit(java.util.List<Object> objs) {
			_Queue.offer(new java.util.ArrayList<Object>(objs));
			return _bSuccess;
		}

		@Override
		public void run() {
			while (_bSuccess && (_bCommiting || _Queue.size() > 0)) {
				if (_Queue.size() == 0) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				java.util.List<Object> fvs = _Queue.poll();
				if (fvs == null) {
					_bSuccess = false;
					return;
				}

				ScriptObjectMirror Values = null;

				Invocable inv = (Invocable) _JsEngine;
				if (!_bNoParseValuesMethod) {
					try {
						Values = (ScriptObjectMirror) inv.invokeMethod(_JsExportObject, "Values", fvs, _FiledNames);
					} catch (NoSuchMethodException e) {
						_bNoParseValuesMethod = true;
					} catch (ScriptException e) {
						logger.warn("ERROR", e);
						_CsvPrintWriter.close();
						_bSuccess = false;
						return;
					}
				}
				String sValues = new String();
				try {
					if (_bNoParseValuesMethod) {
						for (int vi = 0; vi < fvs.size(); vi++) {
							if (_FiledUsings.get(_FiledNames.get(vi))) {
								if (sValues.length() > 0)sValues += ",";
								sValues += ConvertCsvCell(fvs.get(vi).toString());
							}
						}
					} else {
						for (int vi = 0; vi < Values.size(); vi++) {
							String sName = _FiledNames.get(vi);
							if (_FiledUsings.get(sName)) {
								if (sValues.length() > 0)sValues += ",";
								sValues += ConvertCsvCell(Values.getSlot(vi) != null ? Values.getSlot(vi).toString() : "");
							}
						}
					}
				} catch (NullPointerException e) {
					logger.warn("ERROR", e);
					_bSuccess = false;
					return;
				} catch (java.lang.Exception e) {
					logger.warn("ERROR", e);
					_bSuccess = false;
					return;
				}
				_CsvPrintWriter.println(sValues);
			}
			if (_sHeader.length() > 0) {
				_CsvPrintWriter.println(_sHeader);
				_sHeader = "";
			}
		}

		public void CommitOver() {
			if(_bCommiting){
				_bCommiting = false;
				try {
					_hThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public boolean Init(String sScript) 
	{
		bFormatCsv = true;
		_JsEngine = new ScriptEngineManager().getEngineByName("JavaScript");
		try {
			logger.debug(sScript);
			FileInputStream file=new FileInputStream(sScript);
			logger.warn( sScript);
			InputStreamReader filereader=new InputStreamReader(file,"UTF-8");
			_JsEngine.eval(filereader);
		} catch (UnsupportedEncodingException e) {
			logger.warn("ERROR", e);
			return false;
		} catch (FileNotFoundException e) {
			logger.warn("ERROR", e);
			return false;
		} catch (ScriptException e) {
			logger.warn("ERROR", e);
			return false;
		}

		try {
			_JsExportObject = _JsEngine.get("Exports");
		} catch (NullPointerException e) {
			logger.warn("ERROR", e);
			return false;
		} catch (IllegalArgumentException e) {
			logger.warn("ERROR", e);
			return false;
		}
		if (_JsExportObject == null) {
			logger.warn("null = engine.get(\"Exports\"), " + MyWebRoot + sScript);
			return false;
		}
		Invocable inv = (Invocable) _JsEngine;

		try {
			if ("ReportName".equals(_sReportName)) {
				_sReportName = (String) inv.invokeMethod(_JsExportObject,"Name");
			}

		} catch (NoSuchMethodException e) {
		} catch (ScriptException e) {
		}
		
		ScriptObjectMirror Columns = null;
		try {
			Object os = inv.invokeMethod(_JsExportObject, "Header");
			Columns = (ScriptObjectMirror) ScriptUtils.convert(os,ScriptObjectMirror.class);
		} catch (NoSuchMethodException e) {
			logger.warn("ERROR", e);
			return false;
		} catch (ScriptException e) {
			logger.warn("ERROR", e);
			return false;
		}
		String sHeader = new String();
		for (int i = 0; Columns!=null && i < Columns.size(); i++) {
			ScriptObjectMirror Item = null;
			try{
				Item = (ScriptObjectMirror) ScriptUtils.convert(Columns.getSlot(i),ScriptObjectMirror.class);
			} catch (Throwable e) {
				logger.warn("ERROR", e);
				return false;
			}
			if(Item==null){
				logger.warn("ERROR: Header["+i+"] is null");
				return false;
			}
			if(Item.size() > 0 && Item.getSlot(0)!=null && Item.getSlot(0).toString().length()>0){
				String sKey = Item.getSlot(0).toString();
				_FiledNames.add(sKey);
				_FiledValues.add(null);
				if(Item.size() > 1 && Item.getSlot(1)!=null && Item.getSlot(1).toString().length()>0){
					_FiledUsings.put(sKey, true);
					if (sHeader.length() > 0)sHeader += ",";
					sHeader += ConvertCsvCell(Item.getSlot(1).toString());
				}else{
					if (_FiledUsings.containsKey(sKey) && _FiledUsings.get(sKey)) {
						//该列已经需要显示,则始终true
					} else {
						_FiledUsings.put(sKey, false);
					}
				}
			}
		}
		Columns = null;//清空Header
		try {
			Object os = inv.invokeMethod(_JsExportObject, "Footer");
			Columns = (ScriptObjectMirror) ScriptUtils.convert(os,ScriptObjectMirror.class);
		} catch (NoSuchMethodException e) {
			logger.warn("No Exports.Footer");
		} catch (ScriptException e) {
			logger.warn("No Exports.Footer");
		}
		for (int i = 0; Columns!=null && i < Columns.size(); i++) {
			ScriptObjectMirror Item = (ScriptObjectMirror) ScriptUtils.convert(Columns.getSlot(i),ScriptObjectMirror.class);
			if(Item!=null && Item.size()>1 ){
				_FooterNames.add(Item.getSlot(0).toString());
				if (Item.size() > 1) {
					_FooterValues.add(Item.getSlot(1).toString());
				}else{
					_FooterValues.add(null);
				}
			}
		}
		String sPath = System.getProperty("tab.logpath") + "DownloadFiles";
		java.io.File Path = new java.io.File(sPath);
		if(!Path.exists()){
			if(!Path.mkdirs()){
				return false;
			}
		}
		sPath += File.separator;
		if(bFormatCsv){
			_sTempFilePathName = sPath + UUID.randomUUID().toString() + ".csv";
			java.io.FileOutputStream fos = null;
			try {
				fos = new java.io.FileOutputStream(_sTempFilePathName);
				final byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
				fos.write(bom);
				_CsvPrintWriter = new java.io.PrintWriter(new java.io.OutputStreamWriter(fos, "UTF-8"), true);
			} catch (IOException e) {
				logger.warn("ERROR", e);
				return false;
			}
			if (sHeader.length() > 0) {
				_sHeader = sHeader;
			}
			_ThreadCommitCsv.Start();
			return true;
		}
		return false;
	}
	protected void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
		}
		destroy();
	}
	public void destroy() {
		_CsvPrintWriter.close();
		_JsEngine = null;
	}

	private static String ConvertCsvCell(String Value) {
		if(Value.length()==0)Value = "\t";
		if (Value.indexOf('"') >= 0) {
			Value = Value.replaceAll("\"", "\"\"");
		}
		if (Value.indexOf(',') >= 0) {
			return "\"" + Value + "\"";
		}
		if (Value.indexOf('\n') >= 0) {
			return "\"" + Value + "\"";
		}
		return Value;
	}
	
	public boolean CommitRow(java.util.HashMap<String, Object> map) {
		if (_sHeader.length() > 0) {
			_CsvPrintWriter.println(_sHeader);
			_sHeader = "";
		}
		for(java.util.Map.Entry<String, Object> entry : map.entrySet()){
			for (int i = 0; i < _FiledNames.size(); i++) {
				if (_FiledNames.get(i).equals(entry.getKey())) {
					_FiledValues.set(i, entry.getValue());
				}
			}
		}
		if(bFormatCsv){
			return _ThreadCommitCsv.Commit(_FiledValues);
		}
		return false;
	}
	public boolean CommitRow(Object obj) {
		if (_sHeader.length() > 0) {
			_CsvPrintWriter.println(_sHeader);
			_sHeader = "";
		}
		Class<?> clazz = obj.getClass();
		if(HashMap.class.equals(clazz)) {
			HashMap<?, ?> map = (HashMap<?, ?>) obj;
			for(Object fieldName: map.keySet()) {
				Object value = map.get(fieldName);
				for (int i = 0; i < _FiledNames.size(); i++) {
					String sFieldName = _FiledNames.get(i);
					if(sFieldName.equals(fieldName)) {
						_FiledValues.set(i, value);
					}
				}
			}
		}else {
			for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				String fieldName = field.getName();
				Object value = null;
				try {
					value = field.get(obj);
				} catch (IllegalArgumentException e) {
					logger.error("ERROR:",e);
				} catch (IllegalAccessException e) {
					logger.error("ERROR:",e);
				}
				for (int i = 0; i < _FiledNames.size(); i++) {
					String sFieldName = _FiledNames.get(i);
					int nPos = sFieldName.indexOf(".");
					if (nPos<0 && sFieldName.equals(fieldName)) {
						_FiledValues.set(i, value);
					}else if(nPos>0 && sFieldName.length()>nPos+1 && sFieldName.substring(0,nPos).equals(fieldName)){
						_FiledValues.set(i,GetFiledValue(sFieldName.substring(nPos+1),value));
					}
				}
			}
		}
		if(bFormatCsv){
			return _ThreadCommitCsv.Commit(_FiledValues);
		}
		return false;
	}
	private Object GetFiledValue(String sFieldName,Object obj){
		if(obj==null)return null;
		Class<?> clazz = obj.getClass();
		try {
			for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				String fieldName = field.getName();
				if(fieldName.equals(sFieldName)){
					return field.get(obj);
				}
			}
		} catch(SecurityException e) {
			logger.error("ERROR:",e);
		} catch (IllegalArgumentException e) {
			logger.error("ERROR:",e);
		} catch (IllegalAccessException e) {
			logger.error("ERROR:",e);
		}
		return null;
	}
	public void CommitFooter(java.util.HashMap<String, Object> mFooter)
	{
		if(bFormatCsv){
			_ThreadCommitCsv.CommitOver();
			for (int i = 0; i < _FooterNames.size(); i++) {
				if(_FooterValues.get(i)!=null){
					if(mFooter.containsKey(_FooterNames.get(i).toString())){
						_CsvPrintWriter.println(_FooterValues.get(i) + "," 
								+ ConvertCsvCell(mFooter.get(_FooterNames.get(i)).toString()));		
					}else{
						if(_FooterValues.get(i)!=null)_CsvPrintWriter.println(_FooterValues.get(i));
					}
				}
			}
		}
		_bFoooterComplete = true;
	}
	public void CommitFooter(Object obj)
	{
		if(bFormatCsv){
			_ThreadCommitCsv.CommitOver();
			for (int i = 0; i < _FooterNames.size(); i++) {
				if(_FooterValues.get(i)!=null){
					boolean bFound = false;
					String sValue = "";
					Class<?> clazz = obj.getClass();
					for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
						field.setAccessible(true);
						if(field.getName().equals(_FooterNames.get(i).toString())){
							Object value = null;
							try {
								value = field.get(obj);
							} catch (IllegalArgumentException e) {
								logger.error("ERROR:",e);
							} catch (IllegalAccessException e) {
								logger.error("ERROR:",e);
							}
							sValue = value!=null?String.valueOf(value):"";						
							bFound = true;
							break;
						}
					}
					if(bFound){
						_CsvPrintWriter.println(_FooterValues.get(i) + "," + ConvertCsvCell(sValue));
					}else{
						_CsvPrintWriter.println(_FooterValues.get(i));
					}
				}
			}
		}
		_bFoooterComplete = true;
	}
	public String GetFileName(){
		return _sReportName;
	}
	public String GetFile() 
	{
		if(!bFormatCsv)return null;
		_ThreadCommitCsv.CommitOver();
		if(!_bFoooterComplete){
			for (int i = 0; i < _FooterNames.size(); i++) {
				if(_FooterValues.get(i)!=null){
					_CsvPrintWriter.println(_FooterValues.get(i));
				}
			}
		}
		_CsvPrintWriter.close();
		try {
			java.io.File fcsv = new java.io.File(_sTempFilePathName);
			if (!(fcsv.isFile() && fcsv.exists())) {
				return null;
			}
			if(fcsv.length()<1024*1024*2){//5MB
				return _sTempFilePathName;
			}
			String sZipFilePathName = _sTempFilePathName;
			int pos = _sTempFilePathName.lastIndexOf(".csv");
			if(pos>0){
				sZipFilePathName = _sTempFilePathName.substring(0,pos) + ".zip";
			}
			java.io.FileOutputStream fout = new java.io.FileOutputStream(sZipFilePathName);
			java.util.zip.ZipOutputStream zout = 
					new java.util.zip.ZipOutputStream(new java.io.BufferedOutputStream(fout,65535));
			zout.setLevel(9);

			java.util.zip.ZipEntry ze = new java.util.zip.ZipEntry(_sReportName	+ ".csv");
			zout.putNextEntry(ze);

			java.io.FileInputStream fin = new java.io.FileInputStream(_sTempFilePathName);
			java.io.BufferedReader buffered = new java.io.BufferedReader(new java.io.InputStreamReader(fin,"UTF-8"));
			String line = null;
			while ((line = buffered.readLine()) != null) {
				zout.write(line.getBytes("UTF-8"));
				zout.write("\r\n".getBytes("UTF-8"));
			}
			fin.close();
			zout.close();
			if (fcsv.isFile() && fcsv.exists()) {
				fcsv.delete();
			}
			return sZipFilePathName;
		} catch (IOException e) {
			logger.warn("ERROR", e);
			return null;
		}
	}
}
