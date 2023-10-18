var Exports = Object();
Exports.Name = function(){return ""}
Exports.Header = function(){return [
	["start_ts_time","start time"],
	["end_ts_time","end time"],
	["resource_name","resource_name"],
	["employee_id","employee_id"],
	["media_name","media_name"],
	["tenant_name","tenant_name"],
	["total_duration","total_duration"]
];}
Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		lst[i] ="\t"+ records.get(i);
	}
	lst[h.indexOf('total_duration')] = "\t"+ standartCalculateLength(records.get(h.indexOf('total_duration')));
	return lst;
}
function standartCalculateLength(d){
	var hour = Math.floor(d/(3600));//小时
	var h = d%(3600);
	var minute=Math.floor(h/(60)) ;//分钟
	var m = h%(60); 
	var second=Math.floor(m) ;
	var hh = (hour < 10 ? "0" + hour : hour)
	var mm = (minute < 10 ? "0" + minute : minute)
	var ss = (second < 10 ? "0" + second : second)
	return hh+":"+mm+":"+ss;
}

