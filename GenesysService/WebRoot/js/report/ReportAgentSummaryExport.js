var Exports = Object();
Exports.Name = function(){return "工作汇总";}
Exports.Header = function(){return [
["nYear", "时间"],
["username", "姓名"],
["agent", "工号"],
["nOutboundLength","总通话时长"],
["nOutboundCount","总通话次数"],
["nOutboundCount","总平均通话时长"],
["nInboundCount","呼入接通数"],
["nNoAnswerCount","呼入未应答数"],
["nInboundLength","呼入通话时长"],
["nOutboundCount","呼出接通数"],
["nNoConnectCount","呼出未应答数"],
["nOutboundLength","呼出通话时长"],
["nNoAnswerWait","振铃时长(呼入未应答)"],
["nNoConnectWait","振铃时长(呼出未应答)"],
["nType"],
["nMonth"],
["nDay"],
["nHour"],
["nWeek"]
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
		case 2:
			lst[i] = "\t" + records.get(i);
			break;
		case 3:
			var total = records.get(i) == null? 0:records.get(i);
			var total2 = records.get(h.indexOf('nInboundLength')) == null? 0:records.get(h.indexOf('nInboundLength'));
			if(total>0 || total2 >0){
				lst[i] = "\t" + standartCalculateLength(total + total2);
			}else{
				lst[i] = "\t" + "00:00:00";
			}
			break;
		case 4:
			lst[i] = parseInt(records.get(i) + records.get(h.indexOf('nInboundCount')));
			break;
		case 5:
			var inC = records.get(i) + records.get(h.indexOf('nInboundCount'));
			var inL = records.get(h.indexOf('nInboundLength')) == null ? 0:records.get(h.indexOf('nInboundLength')),
				outL = records.get(h.indexOf('nOutboundLength')) == null ? 0:records.get(h.indexOf('nOutboundLength'));
			var len = inL + outL;
			if(len>0 && inC>0){
				lst[i]= "\t"+ standartCalculateLength(len/inC);
			}else{
				lst[i] ="\t" + "00:00:00";
			}
			break;
		case 8:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 11:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 12:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 13:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
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
