var Exports = Object();
Exports.Name = function(){return "坐席呼入呼出汇总";}
Exports.Header = function(){return [
["nYear","时间"],
["username","姓名"],                               
["agent","工号"],
["nInboundCount","呼入呼出总数"],
["nInboundCount","呼入数"],
["nInboundCount","呼入接通数"],
["nInboundCount","呼入接通率"],
["nNoAnswerCount","呼入放弃数"],
["nNoAnswerWait","呼入振铃时长"],
["nInboundCount","呼入平均振铃时长"],
["nInboundLength","呼入通话时长"],
["nInboundLength","呼入平均通话时长"],
["nInboundWaitLessCount","呼入接通数(<应答标准)"],
["nMaxAnswerLength","呼入最大通话时长"],
["nOutboundCount","呼出数"],
["nOutboundCount","呼出接通数"],
["nOutboundCount","呼出接通率"],
["nOutboundWaitLessCount","呼出接起(<应答标准)"],
["nNoConnectCount","呼出无人接通数"],
["nNoConnectWait","呼出振铃时长"],
["nOutboundLength","呼出通话时长"],
["nOutboundLength","呼出平均通话时长"],
["nMaxConnectLength","呼出最大通话时长"],
["nInboundWait"],
["nOutboundAbandonWaitLessCount"],
["nOutboundWait"],
["nConnectGreatLength"],
["nType"],
["nMonth"],
["nDay"],
["nHour"],
["nWeek"],
["nInboundAbandonWaitLessCount"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			var tyTime = records.get(h.indexOf('nType'));
			if(tyTime==0){
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nMonth')) + '月' + records.get(h.indexOf('nDay')) + '日 ' + records.get(h.indexOf('nHour'))+'时';
			}else if(tyTime==1){
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nMonth')) + '月' + records.get(h.indexOf('nDay'))+ '日';
			}else if(tyTime==2){
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nWeek')) + '周';
			}else{
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nMonth')) + '月';
			}
			break;
		case 1:
			lst[i] = (records.get(i) == null || records.get(i) == "")? records.get(h.indexOf('agent')):records.get(i);
			break;
		case 3:
			lst[i] = parseInt(records.get(i)) + parseInt(records.get(h.indexOf('nNoAnswerCount'))) + parseInt(records.get(h.indexOf('nOutboundCount'))) + parseInt(records.get(h.indexOf('nNoConnectCount')));
			break;
		case 4:
			lst[i] = parseInt(records.get(i)) + parseInt(records.get(h.indexOf('nNoAnswerCount')));
			break;
		case 6:
			var answer = parseInt(records.get(i));
			var totalAnswer = parseInt(records.get(i)) + parseInt(records.get(h.indexOf('nNoAnswerCount')));
			if(answer==0){
				lst[i] ="0%";
			}else{
				lst[i] = (Math.round(answer/totalAnswer * 10000) / 100 + "%");
			}
			break;
		case 8:
			var inRing = records.get(i) == null? 0:records.get(i),
				inWait = records.get(h.indexOf('nInboundWait')) == null? 0:records.get(h.indexOf('nInboundWait')),
				totalRing = inRing + inWait;
			if(totalRing == 0){
				lst[i]= "\t"+ "00:00:00";
			}else{
				lst[i] = "\t" + standartCalculateLength(totalRing);
			}
			break;
		case 9:
			var v1 = records.get(i) + records.get(h.indexOf('nNoAnswerCount')),
				nNo = records.get(h.indexOf('nNoAnswerWait')) == null? 0:records.get(h.indexOf('nNoAnswerWait')),
				nIn	= records.get(h.indexOf('nInboundWait')) == null? 0:records.get(h.indexOf('nInboundWait'));					
			var v2 = nNo + nIn;
			if(v2 >0){
				lst[i]= "\t" + standartCalculateLength(v2/v1);
			}else{
				lst[i]= "\t" + "00:00:00";
			}
			break;
		case 10:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 11:
			var v1 = parseInt(records.get(h.indexOf('nInboundCount')));
			var v2 = parseInt(records.get(i));
			if(v1==0){
				lst[i]= "\t" + "00:00:00";
			}else{
				lst[i]= "\t" + standartCalculateLength(v2/v1);
			}
			break;
		case 12:
			lst[i] = records.get(i) - records.get(h.indexOf('nInboundAbandonWaitLessCount'));
			break;
		case 13:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 14:
			lst[i] = parseInt(records.get(i)) + parseInt(records.get(h.indexOf('nNoConnectCount')));
			break;
		case 16:
			var answer = parseInt(records.get(i));
			var totalAnswer = parseInt(records.get(i)) + parseInt(records.get(h.indexOf('nNoConnectCount')));
			if(answer==0){
				lst[i] ="0%";
			}else{
				lst[i] = (Math.round(answer/totalAnswer * 10000) / 100 + "%");
			}
			break;
		case 17:
			lst[i] = parseInt(records.get(i)) - parseInt(records.get(h.indexOf('nOutboundAbandonWaitLessCount')));
			break;
		case 19:
			var outL = records.get(i)  == null ? 0:records.get(i),
				outlen = records.get(h.indexOf('nOutboundWait'))== null? 0:records.get(h.indexOf('nOutboundWait')),
				totalLen = outL + outlen;
			if(totalLen == 0){
				lst[i]= "\t"+ "00:00:00";
			}else{
				lst[i] = "\t"+ standartCalculateLength(totalLen);
			}
			break;
		case 20:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 21:
			var v1 = parseInt(records.get(h.indexOf('nOutboundCount')));
			var v2 = parseInt(records.get(i));
			if(v1==0){
				lst[i]= "\t"+ "00:00:00";
			}else{
				lst[i]= "\t" + standartCalculateLength(v2/v1);
			}
			break;
		case 22:
			lst[i] = "\t"+ standartCalculateLength(records.get(i));
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