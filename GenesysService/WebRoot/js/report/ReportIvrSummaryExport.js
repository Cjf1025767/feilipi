var Exports = Object();
Exports.Name = function(){return "呼入队列汇总";}
Exports.Header = function(){return [
["nyear","时间"],
["queue","来电目的"],
["ninboundcount","呼入数"],
["ninboundlength","IVR时长(平均)"],
["ninboundwait","排队时长(平均)"],
["nnoanswercount","无人接听"],
["nnoanswerwait","无人接听排队时长(平均)"],
["ntype"],
["nmonth"],
["nday"],
["nhour"],
["nweek"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		switch(i){
		case 0:
			var tyTime = records.get(h.indexOf('ntype'));
			if(tyTime==0){
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nmonth')) + '月' + records.get(h.indexOf('nday')) + '日 ' + records.get(h.indexOf('nhour'))+'时';
			}else if(tyTime==1){
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nmonth')) + '月' + records.get(h.indexOf('nday'))+ '日';
			}else if(tyTime==2){
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nweek')) + '周';
			}else{
				lst[i] = records.get(i) + '年' + records.get(h.indexOf('nmonth')) + '月';
			}
			break;
		case 1:
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
		case 3:
		case 4:
		case 6:
			lst[i] = standartCalculateLength(records.get(i));
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