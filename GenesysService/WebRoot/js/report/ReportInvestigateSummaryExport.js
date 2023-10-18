var Exports = Object();
Exports.Name = function(){return "满意度汇总";}
Exports.Header = function(){return [
["nYear","时间"],
["agentNo","工号"],
["n1Count","非常满意"],
["n2Count","满意"],
["n3Count","不满意"],
["nTotalCallCount","总评分"],
["nTotalCallCount","有效评分"],
["n5Count"],
["n0Count"],
["nType"],
["nMonth"],
["nDay"],
["nDayOfWeek"],
["nHour"],
["nWeek"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	var dLess,dTotal;
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			switch(records.get(h.indexOf('nType'))){
				case 0:
				lst[i] = records.get(i)+"年"+records.get(h.indexOf('nMonth'))+"月"+records.get(h.indexOf('nDay'))+"日 " + records.get(h.indexOf('nHour'))+"时";
				break;
				case 1:
				lst[i] = records.get(i)+"年"+records.get(h.indexOf('nMonth'))+"月"+records.get(h.indexOf('nDay'))+"日";
				break;
				case 2:
				lst[i] = records.get(i)+"年"+records.get(h.indexOf('nWeek')) + "周";
				break;
				case 3:
				lst[i] = records.get(i)+"年"+records.get(h.indexOf('nMonth'))+"月";
				break;
			}
			break;
		case 6:
			lst[i] = parseInt(records.get(h.indexOf('nTotalCallCount')))-parseInt(records.get(h.indexOf('n0Count')));
			break;
		default:
			lst[i] = records.get(i);
			break;
		}
	}
	return lst;
}
