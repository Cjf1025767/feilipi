var Exports = Object();
Exports.Name = function(){return "呼入明细";}
Exports.Header = function(){return [
["begintime","时间"],
["lastnode","分机号码"],
["ani","主叫号码"],
["dnis","被叫号码"],
["type","应答情况"],
["agent","应答工号"],
["enterqueuetime","进入队列时间"],
["exitqueuetime","离开队列时间"],
["exitqueuetime","排队时长"],
["length"],
["cityinfoId"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	var dLess,dTotal;
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			lst[i] = "\t"+records.get(i);
			break;
		case 1:
			lst[i] = parseInt(records.get(i))==0?"":records.get(i);
			break;
		case 4:
			var value = records.get(i);
			if(value == 4){
				lst[i]= "IVR";
        	}else if(value == 12){
        		lst[i]= "排队";
        	}else if(value == 13 || value == 14){
        		lst[i]= "转接";
        	}else{
        		lst[i]= "";
        	}
			break;
		case 6:
			if((records.get(h.indexOf('type')) == 12 && records.get(h.indexOf('agent')) == "") || records.get(h.indexOf('type')) == 13){
        		var Qtime = Date.parse(records.get(h.indexOf('begintime'))),
        		v = new Date(Qtime + records.get(h.indexOf('cityinfoId')) * 1000),
        		nYear = v.getFullYear() + '-',
        		nMonth = (v.getMonth()+1 < 10 ? '0'+(v.getMonth()+1) : v.getMonth()+1) + '-',
        		nDay = v.getDate();
        		
        		lst[i] = nYear + nMonth + nDay + ' ' + v.getHours() + ':' + v.getMinutes() + ':' + v.getSeconds();
    		}else{
    			lst[i] = "";
    		}
			break;
		case 7:
			if((records.get(h.indexOf('type')) == 12 && records.get(h.indexOf('agent')) == "") || records.get(h.indexOf('type')) == 13){
        		var Qtime = Date.parse(records.get(h.indexOf('begintime'))),
        		v = new Date(Qtime + records.get(h.indexOf('length')) * 1000),
        		nYear = v.getFullYear() + '-',
        		nMonth = (v.getMonth()+1 < 10 ? '0'+(v.getMonth()+1) : v.getMonth()+1) + '-',
        		nDay = v.getDate();
        		
        		lst[i] = nYear + nMonth + nDay + ' ' + v.getHours() + ':' + v.getMinutes() + ':' + v.getSeconds();
    		}else{
    			lst[i] = "";
    		}
			break;
		case 8:
			var Q_Len = Math.abs(records.get(i) - records.get(h.indexOf('enterqueuetime')));
			if((records.get(h.indexOf('type')) == 12 && records.get(h.indexOf('agent')) == "") || records.get(h.indexOf('type')) == 13){
				lst[i]= "\t" + standartCalculateLength(Q_Len);
			}else{
				lst[i]= "\t" + "00:00:00";
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
