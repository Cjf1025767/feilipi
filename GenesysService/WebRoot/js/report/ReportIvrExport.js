var Exports = Object();
Exports.Name = function(){return "呼入队列明细";}
Exports.Header = function(){return [
["vdn","来电目的"],
["begintime","时间"],
["channel","分机号"],
["ani","主叫号码"],
["dnis","被叫号码"],
["type","应答情况"],
["ringtime","进入队列时间"],
["endtime","离开队列时间"],
["wait","等待时长"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			switch(records.get(i)){
			case "consulting":
			  lst[i] = "业务咨询(厂)";
			  break;
			case "change":
			  lst[i] = "变更申请(厂)";
			  break;
			case "complaints":
			  lst[i] = "投诉及建议(厂)";
			  break;
			case "consulting_jrsyb":
			  lst[i] = "业务咨询(金)";
			  break;
			case "change_jrsyb":
			  lst[i] = "变更申请(金)";
			  break;
			case "complaints_jrsyb":
			  lst[i] = "投诉及建议(金)";
			  break;
			case "other_jrsyb":
			  lst[i] = "其他(金)";
			  break;
			default:
			  lst[i] = "其他(厂)";
			  break;
			}
			break;
		case 5:
			switch(records.get(i)){
			case 'Abandoned':    
			case 'CustomerAbandoned':
			case 'Redirected':
				lst[i] = "排队放弃";
				break;
			case 'IVR':
				lst[i] = "自动语音";
				break;
			case 'Pulled':
			case 'None':
			case 'Incomplete':
			case 'OutboundStopped':
			case 'Transferred':
			case 'Diverted':
			case 'DestinationBusy':
			case 'Conferenced':
			case 'Cleared':
			case 'Routed':
			case 'AbnormalStop':
			case 'Deferred':
				lst[i] = value;
				break;
			case 'Completed':
				lst[i] = "转接坐席";
				break;
			}
			break;
		case 8:
			lst[i] = standartCalculateLength(records.get(i))
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