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
			break;
		case 7:
			var len = records.get(i);
			var hour = Math.floor(len / (3600)); //小时
            var h = len % (3600);
            var minute = Math.floor(len / (60)); //分钟
            var m = len % (60);
            var second = Math.floor(m);
            var hh = (hour < 10 ? "0" + hour : hour)
            var mm = (minute < 10 ? "0" + minute : minute)
            var ss = (second < 10 ? "0" + second : second)
            lst[i] = hh + ":" + mm + ":" + ss;
			break;
		}
	}
	return lst;
}
