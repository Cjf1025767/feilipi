var Exports = Object();
Exports.Name = function(){return "呼入业务(IVR)";}
Exports.Header = function(){return [
["begintime","时间"],
["username","业务类型"],
["ani","来电号码"],
["length","时长"],
["vdn"]
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
			lst[i] = (records.get(i) == null || records.get(i) == "")? records.get(h.indexOf('vdn')):records.get(i);
			break;
		default:
			lst[i] = records.get(i);
			break;
		}
	}
	return lst;
}
