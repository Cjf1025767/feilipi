var Exports = Object();
Exports.Name = function(){return "通话明细";}
Exports.Header = function(){return [
["ringtime", "时间"],
["username", "姓名"],
["agent", "座席工号"],
["channel", "分机号码"],
["split", "是否Skill呼叫"],
["ani", "内线外线"],
["ani", "主叫"],
["dnis", "被叫"],
["begintime", "接听时间"],
["endtime", "结束时间"],
["length", "通话时长"],
["wait", "振铃时长"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			lst[i] = ("\t"+ records.get(i)).substring(0,20);
			break;
		case 1:
			lst[i] = (records.get(i) == null || records.get(i) == "")? records.get(h.indexOf('channel')):records.get(i);
			break;
		case 4:
			if(records.get(i) != 0){
				lst[i] = "Skill呼入";
			}else{
				lst[i] = "非Skill呼入";
			}
			break;
		case 5:
			if (records.get(i).length < 5 && records.get(h.indexOf("dnis")).length < 5)
            {
				lst[i] = "内线";
			}else{
            	lst[i] = "外线";
            }
			break;
		case 7:
			lst[i] = "\t" + records.get(i);
			break;
		case 8:
			lst[i] = ("\t"+ records.get(i)).substring(11,20);
			break;
		case 9:
			lst[i] = ("\t"+ records.get(i)).substring(11,20);
			break;
		case 10:
			lst[i] = "\t"+ standartCalculateLength(records.get(i));
			break;
		case 11:
			lst[i] = "\t"+ standartCalculateLength(records.get(i));
			break;
		case 12:
			lst[i] = "\t" + records.get(i);
			break;
		default:
			lst[i] = records.get(i);
			break;
		}
	}
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
