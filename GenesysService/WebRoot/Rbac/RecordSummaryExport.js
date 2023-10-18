var Exports = Object();
Exports.Name = function(){return "录音汇总";}
Exports.Header = function(){return [
["host", "主机"],
["rolename", "部门"],
["nYear", "时间"],
["nTotalCount", "录音总数"],
["nInboundCount", "呼入总数"],
["nOutboundCount", "呼出总数"],
["nBackupCount", "备份数"],
["nSeconds","总时长(秒)"],
["nMonth", ""],
["nDay", ""],
["nHour", ""],
["nWeek", ""]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		default:
			lst[i] = records.get(i);
			break;
		case 2:
			var nHour = records.get(h.indexOf('nHour'));
			var nWeek = records.get(h.indexOf('nWeek'));
			var nDay = records.get(h.indexOf('nDay'));
			var nMonth = records.get(h.indexOf('nMonth'));
			if(nHour!=null){
				lst[i] = records.get(i) + '年' + nMonth + '月' + nDay + '日 ' + nHour + '时';
			}else if(nDay!=null){
				lst[i] = records.get(i) + '年' + nMonth + '月' + nDay + '日';
			}else if(nWeek!=null){
				lst[i] = records.get(i) + '年' + nWeek + '周';
			}else{
				lst[i] = records.get(i) + '年' + nMonth + '月';
			}
		}
	}
	return lst;
}
