var Exports = Object();
Exports.Name = function(){return "工作明细";}
Exports.Header = function(){return [
["username", "姓名"],
["agent", "工号"],
["currenttime","开始"],
["currenttime","结束"],
["length", "时长"],
["type", "状态"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			lst[i] ="\t" + (records.get(i) == null || records.get(i) == "")? records.get(h.indexOf('agent')):records.get(i);;
			break;
		case 3:
			var currentTime = new Date((new Date(records.get(i))).getTime() +  (records.get(h.indexOf('length'))*1000));
			var hours = currentTime.getHours();
			var minutes = currentTime.getMinutes();
			var seconds = currentTime.getSeconds();
			hours = (hours < 10 ? "0" + hours : hours);
			minutes = (minutes < 10 ? "0" + minutes : minutes);
			seconds = (seconds < 10 ? "0" + seconds : seconds);
			lst[i] = "\t"+ hours+":"+minutes+":"+seconds;
			break;
		case 4:
			lst[i] = "\t" + standartCalculateLength(records.get(i));
			break;
		case 5:
			var value = records.get(i);
			if(value==7){
        		lst[i] = "登出";
            }else if (value == 8){
            	lst[i] = "登入";
            }else if (value == 9){
            	lst[i] = "就绪";
            }else if(value == 10){
            	lst[i] = "未就绪";
			} else if(value == 11){
				lst[i] = "话后处理";
			} else if (value >= 12 && value <=17) {
				lst[i] = "未就绪";
			} else if(value == 18){
				lst[i] = "通话";
			} else if(value == 19){
				lst[i] = "就绪等待";
			} else{
            	lst[i] = "value:" + value;
            }
			break;
		default:
			lst[i] ="\t" + records.get(i);
			break;
		}
	}
	return lst;
}
Exports.Footer = function(){return [
	["Total","Total:"],                                    
	["nLoginTimes","LoginTimes"],
	["nNotreadyTimes","NotreadyTimes"],
	["nReadyTimes","ReadyTimes"],
	["nLogOutTimes","LogOutTimes"],
	["nTotalLoginTime","TotalLoginTime"],
	["nTotalNotreadyTime","TotalNotreadyTime"],
	["nTotalReadyTime","TotalReadyTime"]
	];}
	
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