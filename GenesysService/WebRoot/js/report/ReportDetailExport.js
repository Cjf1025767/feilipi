var Exports = Object();
Exports.Name = function(){return "坐席明细";}
Exports.Header = function(){return [
["ringtime","日期"],
["ringtime","来电时间"],
["ani","主叫号码"],
["type","呼叫类型"],
["extension","分机"],
["username","坐席姓名"],
["begintime","开始时间"],
["endtime","结束时间"],
["wait","振铃时长"],
["length","处理时长"],
["vdn"],
["agent"],
["dnis"],
["data"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			lst[i] = ("\t"+ records.get(i)).substr(0,11);
			break;
		case 1:
			lst[i] = ("\t"+ records.get(i)).substring(11,20);
			break;
		case 2:
			var nType = records.get(h.indexOf('type'));
			var sCall = records.get(h.indexOf('dnis'));
			if(nType == 7 || nType == 9 ||nType == 10){
				lst[i] = "\t"+ sCall;
			}else{
				lst[i] = "\t"+ records.get(i);
			} 
			break;
		case 3:
			var calltype = records.get(i);
			if (calltype == 0 || calltype == 1 || calltype == 9 || calltype == 2) {
				lst[i] = "呼出无人应答";
			} else if(calltype == 7 || calltype == 10){
				lst[i] = "呼出接通";
			}else if (calltype == 5 || calltype == 6 || calltype == 11) {
				lst[i] = "呼入接听";
			} else if (calltype == 8 || calltype == 12 || calltype == 14 || calltype == 4) {
				lst[i] = "呼入放弃";
			} else if (calltype == 13 && (records.get(h.indexOf('agent'))) == '') {
				lst[i] = "呼入放弃";
			} else if (calltype == 13) {
				lst[i] = "呼入接听";
			}
			break;
		case 5:
			lst[i] = (records.get(i) == null || records.get(i) == "")? records.get(h.indexOf('extension')):records.get(i);
			break;
		case 6:
			lst[i] = ("\t"+ records.get(i)).substring(11,20);
			break;
		case 7:
			lst[i] = ("\t"+ records.get(i)).substring(11,20);
			break;
		case 8:
			var value =  records.get(i);
			var dResult1 = Math.floor(value / 3600);
			var dHour = ( dResult1 < 10) ? ("0" + dResult1) : dResult1;
			var dResult2 = Math.floor( (value - dHour * 3600) / 60);
			var dMin =  (dResult2 < 10) ? ("0" + dResult2): dResult2;
			var dSec = ((value % 60) < 10) ? ("0" + (value % 60)) : (value % 60);
			lst[i] = dHour + ":" + dMin + ":" + dSec;
			break;
		case 9:
			var value =  records.get(i);
			var dResult1 = Math.floor(value / 3600);
			var dHour = ( dResult1 < 10) ? ("0" + dResult1) : dResult1;
			var dResult2 = Math.floor( (value - dHour * 3600) / 60);
			var dMin =  (dResult2 < 10) ? ("0" + dResult2): dResult2;
			var dSec = ((value % 60) < 10) ? ("0" + (value % 60)) : (value % 60);
			lst[i] = dHour + ":" + dMin + ":" + dSec;
			break;
		default:
			lst[i] = records.get(i);
			break;
		}
	}
	return lst;
}