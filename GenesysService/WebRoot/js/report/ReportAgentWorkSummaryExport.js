var Exports = Object();
Exports.Name = function(){return "坐席工作汇总";}
Exports.Header = function(){return [
["nYear","时间"],                              
["agent","工号"],
["nWorkLength","实际在线时长"],
["nReadyCount","就绪次数"],
["nReadyLength","就绪时长"],
["nNotReadyCount10","小休次数"],
["nNotReadyLength10","小休时长"],
["nAfterWorkCount","后处理次数"],
["nAfterWorkLength","后处理时长"],
["nAfterWorkLength","平均后处理时长"],
["nType"],
["nMonth"],
["nDay"],
["nHour"],
["nWeek"],
["sWorkTime"]
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
			lst[i] = "\t"+ standartCalculateLength(records.get(i));
			break;
		case 4:
			lst[i] = "\t"+ standartCalculateLength(records.get(i));
			break;
		case 6:
			lst[i] = "\t"+ standartCalculateLength(records.get(i));
			break;
		case 8:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 9:
			var v1 = records.get(h.indexOf('nAfterWorkCount'));
			var v2 = parseInt(records.get(i));
			if(v1==0){
				lst[i]= "\t" + "00:00:00";
			}else{
				lst[i]= "\t"+ standartCalculateLength(v2/v1);
			}
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