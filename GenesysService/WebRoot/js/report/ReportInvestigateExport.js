var Exports = Object();
Exports.Name = function(){return "满意度明细";}
Exports.Header = function(){return [
["investigateDate","时间"],
["username","姓名"],
["agentNo","工号"],
["ani","主叫"],
["result","满意度"],
["businessType"],
["sCallId"]
];}

Exports.Values = function(records)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
		case 2:
		case 3:
		case 5:
			lst[i] = "\t"+records.get(i);
			break;
		case 4:
			switch(parseInt(records.get(i))){
			default:
				lst[i] = "未评分";
				break;
			case 1:
				lst[i] = "非常满意";
				break;
			case 2:
				lst[i] = "满意";
				break;
			case 3:
				lst[i] = "不满意";
				break;
			}
			break;
		default:
			lst[i] = records.get(i);
			break;
		}
	}
	return lst;
}
