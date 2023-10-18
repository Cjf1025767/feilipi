var Exports = Object();
Exports.Name = function(){return ""}
Exports.Header = function(){return [
	["time","time"],
	["resource_name","resource_name"],
	["agent_group","agent_group"],
	["user_name","user_name"],
	
	["accepted","accepted"],
	["notaccepted","notaccepted"],
	["offered","offered"],
	["abandoned_invite","abandoned_invite"],
	["rejected","rejected"],
	["invite","invite"],
		["invite_time","invite_time"],
		["engage_time","engage_time"],
		["engage","engage"],
		["hold_time","hold_time"],
		["wrap_time","wrap_time"],
		["wrap","wrap"],
		["consult_received_accepted","consult_received_accepted"],
		["consult_received_engage_time","consult_received_engage_time"],
		["transfer_init_agent","transfer_init_agent"],
		["xfer_received_accepted","xfer_received_accepted"],
		["interaction_type_code","interaction_type_code"],
		["interaction_subtype_code","interaction_subtype_code"]
];}

Exports.Values = function(records,h)
{
	var lst = new Array();
	var lst = new Array();
	for(var i=0;i<records.size();i++){
		lst[i] ="\t"+ records.get(i);
	}
	lst[h.indexOf('invite_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('invite_time')));
	lst[h.indexOf('engage_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('engage_time')));
	lst[h.indexOf('hold_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('hold_time')));
	lst[h.indexOf('wrap_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('wrap_time')));
	lst[h.indexOf('consult_received_engage_time')] = "\t"+ standartCalculateLength(records.get(h.indexOf('consult_received_engage_time')));
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