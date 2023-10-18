var Exports = Object();
Exports.Name = function(){return "坐席通话明细";}
Exports.Header = function(){return [
["ringtime","来电时间"],
["ani","主叫号码"],
["type","呼叫类型"],
["channel","分机"],
["username","姓名"],
["begintime","开始时间"],
["endtime","结束时间"],
["wait","振铃时长"],
["length","处理时长"],
["agent"],
["dnis"],
["data"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		default:
			lst[i] = '\t' + records.get(i);
			break;
		}
	}
	return lst;
}